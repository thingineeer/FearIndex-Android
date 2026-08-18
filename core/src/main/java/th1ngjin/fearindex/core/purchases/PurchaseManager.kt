package th1ngjin.fearindex.core.purchases

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.crash.CrashReporter
import th1ngjin.fearindex.core.debug.ScreenshotMode
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Play Billing 기반 "평생 광고 제거" one-time(INAPP, non-consumable) 인앱결제 관리 싱글톤.
 *
 * iOS [PurchaseManager.swift](../../../../../../FearIndex-iOS/LocalPackages/Core/Sources/Core/Purchases/PurchaseManager.swift)
 * 의 1:1 포팅. SRP: 광고 제거 entitlement 평가 + 구매/복원만 담당.
 *
 * - 판정 로직(구매 목록 → entitlement, responseCode → 결과 분기)은 [IapEntitlement]/[IapPurchaseOutcome]
 *   순수 객체에 위임하고 이 클래스는 BillingClient 글루만 얇게 유지한다.
 * - [isAdFree] 는 SharedPreferences 캐시로 init 시 동기 복원 → 첫 프레임 광고 깜빡임 방지.
 * - 모든 IAP 에러 로그에는 "Error" 토큰을 포함해 콘솔에서 필터링 가능하게 한다.
 */
@Singleton
class PurchaseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: AnalyticsManager,
    private val crashReporter: CrashReporter,
) {

    // MARK: - Public State

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAdFree = MutableStateFlow(prefs.getBoolean(KEY_AD_FREE_CACHED, false))

    /** 광고 제거 권한. AdBanner/인터스티셜 게이트가 구독. */
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private val _priceText = MutableStateFlow<String?>(null)

    /** 로드된 상품 가격(미로드시 null → UI fallback "US$4.99"). */
    val priceText: StateFlow<String?> = _priceText.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    // MARK: - Private State

    private val didStart = AtomicBoolean(false)

    /** 사용자 구매가 진행 중인지 — 완료/취소/실패 이벤트를 정확히 1회만 내기 위한 게이트. */
    private val purchaseInFlight = AtomicBoolean(false)

    /** 결제 콜백은 Main 스레드에서 오므로 Main.immediate 로 실행 (BillingClient 권장). */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * 결제에 필요한 상품 + 오퍼 토큰 묶음.
     * Play Billing 8+ 는 일회성 상품도 offerToken 이 필수라, 가격 표시와 결제가 어긋나지 않도록 함께 보관한다.
     */
    private data class RemoveAdsOffering(
        val product: ProductDetails,
        val offerToken: String,
    )

    @Volatile
    private var removeAdsOffering: RemoveAdsOffering? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        onPurchasesUpdated(result, purchases)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    // MARK: - Public API

    /**
     * 앱 시작 시 1회 호출 (FearIndexApp.onCreate). 각 단계는 실패해도 앱 시작을 막지 않는다.
     * 스크린샷 모드에서는 서버콜을 스킵한다.
     */
    fun start() {
        if (ScreenshotMode.isEnabled()) return
        if (!didStart.compareAndSet(false, true)) return
        Timber.tag(TAG).i("[IAP] start — cached adFree=%b", _isAdFree.value)
        connectAndRun {
            reevaluateEntitlements()
            loadProductsIfNeeded()
        }
    }

    /**
     * 구매 시작. 결과(완료/취소/실패)는 항상 [purchaseEvents] 로 정확히 1회 전달된다.
     * 이미 진행 중이면 무시한다(중복 탭 방어).
     */
    fun purchaseRemoveAds(activity: Activity) {
        if (!purchaseInFlight.compareAndSet(false, true)) return
        analytics.log(AnalyticsEvent.광고제거구매시작)
        val offering = removeAdsOffering
        if (offering == null) {
            // 상품 미로드 — 연결 후 로드 재시도하고, 로드되면 즉시 구매 시트를 띄운다.
            // 연결/로드 실패 시에도 반드시 실패 이벤트를 내 UI 스피너가 멈추도록 한다.
            scope.launch {
                val loaded = runCatching {
                    if (ensureConnected()) {
                        loadProductsIfNeeded()
                        removeAdsOffering
                    } else {
                        null
                    }
                }.getOrNull()
                if (loaded != null) {
                    launchFlow(activity, loaded)
                } else {
                    reportPurchaseFailure(FAIL_PRODUCT_UNAVAILABLE, "상품 정보를 불러오지 못함")
                }
            }
            return
        }
        launchFlow(activity, offering)
    }

    /**
     * 구매 복원 (entitlement 재평가). 복원 성공 여부 반환.
     * iOS [PurchaseManager.restorePurchases] 대응.
     */
    suspend fun restorePurchases(): Boolean {
        if (!ensureConnected()) {
            Timber.tag(TAG).w("[IAP] 복원 Error(연결 실패)")
            analytics.log(AnalyticsEvent.광고제거복원(성공여부 = _isAdFree.value))
            return _isAdFree.value
        }
        reevaluateEntitlements()
        Timber.tag(TAG).i("[IAP] 복원 완료 — adFree=%b", _isAdFree.value)
        analytics.log(AnalyticsEvent.광고제거복원(성공여부 = _isAdFree.value))
        return _isAdFree.value
    }

    /** onResume 재평가용 (MainActivity.onResume). 조용히 entitlement 만 갱신. */
    fun refreshEntitlements() {
        if (ScreenshotMode.isEnabled()) return
        connectAndRun { reevaluateEntitlements() }
    }

    // MARK: - Billing Flow

    private fun launchFlow(activity: Activity, offering: RemoveAdsOffering) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(offering.product)
                        // Play Billing 8+ 는 일회성 상품도 offerToken 이 필수.
                        .setOfferToken(offering.offerToken)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            reportPurchaseFailure(result.responseCode, result.debugMessage)
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val snapshots = (purchases ?: emptyList()).map { it.toSnapshot() }
        when (val outcome = IapPurchaseOutcome.evaluate(result.responseCode, snapshots, REMOVE_ADS_PRODUCT_ID)) {
            is IapPurchaseOutcome.Outcome.Completed -> {
                outcome.tokensToAcknowledge.forEach { acknowledge(it) }
                grantAdFree(reason = "purchase")
                completePurchase()
            }
            IapPurchaseOutcome.Outcome.Cancelled -> {
                if (!purchaseInFlight.compareAndSet(true, false)) return
                Timber.tag(TAG).i("[IAP] 사용자 구매 취소")
                _purchaseEvents.tryEmit(PurchaseEvent.Cancelled)
            }
            // 이미 소유한 상품 재구매 시도 — 실패가 아니라 entitlement 재평가로 grant 처리 (오류 다이얼로그 방지).
            // connectAndRun 은 연결 실패 시 조용히 스킵해 purchaseInFlight 가 영구 누수(스피너 행,
            // 이후 구매 탭 무시)되므로, 여기서는 실패 경로까지 반드시 터미널 이벤트로 마감한다.
            IapPurchaseOutcome.Outcome.AlreadyOwned -> scope.launch {
                if (runCatching { ensureConnected() }.getOrDefault(false)) {
                    runCatching { reevaluateEntitlements() }
                        .onFailure { Timber.tag(TAG).w(it, "[IAP] 재평가 Error") }
                }
                if (_isAdFree.value) {
                    completePurchase()
                } else {
                    reportPurchaseFailure(result.responseCode, result.debugMessage)
                }
            }
            is IapPurchaseOutcome.Outcome.Failed ->
                reportPurchaseFailure(outcome.code, result.debugMessage)
        }
    }

    /** 구매 완료 처리 — 진행 중인 시도에 대해서만 1회 grant 로그 + Completed 이벤트를 낸다. */
    private fun completePurchase() {
        if (!purchaseInFlight.compareAndSet(true, false)) return
        analytics.log(AnalyticsEvent.광고제거구매완료)
        _purchaseEvents.tryEmit(PurchaseEvent.Completed)
    }

    private fun reportPurchaseFailure(code: Int, message: String) {
        // launchBillingFlow 실패 콜백과 onPurchasesUpdated 콜백이 같은 시도에 이중 발행되는 것을 막는다.
        if (!purchaseInFlight.compareAndSet(true, false)) return
        Timber.tag(TAG).e("[IAP] 구매 실패 Error: %d — %s", code, message)
        crashReporter.recordException(IllegalStateException("[IAP] 구매 실패 Error: $code — $message"))
        analytics.log(AnalyticsEvent.광고제거구매실패(에러메시지 = message))
        _purchaseEvents.tryEmit(PurchaseEvent.Failed(message))
    }

    // MARK: - Entitlements

    /**
     * queryPurchasesAsync(INAPP) 로 광고 제거 보유 여부 재평가.
     * PURCHASED + productId 매칭 → grant, 미acknowledge 면 acknowledge.
     */
    private suspend fun reevaluateEntitlements() {
        val purchases = queryInAppPurchases() ?: return
        val evaluation = IapEntitlement.evaluate(
            purchases.map { it.toSnapshot() },
            REMOVE_ADS_PRODUCT_ID,
        )
        evaluation.tokensToAcknowledge.forEach { acknowledge(it) }
        if (evaluation.isAdFree) grantAdFree(reason = "entitlement")
    }

    private suspend fun queryInAppPurchases(): List<Purchase>? {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        // Play 서비스가 콜백을 영영 안 주면 복원 스피너가 무한 대기하므로 연결과 동일한 timeout.
        return withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { cont ->
                billingClient.queryPurchasesAsync(params) { result, purchases ->
                    if (!cont.isActive) return@queryPurchasesAsync
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(purchases)
                    } else {
                        Timber.tag(TAG).w("[IAP] 구매 조회 Error: %d — %s", result.responseCode, result.debugMessage)
                        cont.resume(null)
                    }
                }
            }
        } ?: run {
            Timber.tag(TAG).w("[IAP] 구매 조회 Error(timeout)")
            null
        }
    }

    private fun acknowledge(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Timber.tag(TAG).w("[IAP] acknowledge Error: %d — %s", result.responseCode, result.debugMessage)
            }
        }
    }

    /** 광고 제거 권한 부여 + 캐시 저장. 이미 부여된 상태면 no-op (중복 디스크 쓰기 방지). */
    private fun grantAdFree(reason: String) {
        if (_isAdFree.value) return
        prefs.edit().putBoolean(KEY_AD_FREE_CACHED, true).apply()
        _isAdFree.value = true
        Timber.tag(TAG).i("[IAP] 광고 제거 활성화 (%s)", reason)
    }

    // MARK: - Products

    /**
     * 상품 로드 (실패해도 앱을 막지 않음 — 구매 시점에 재시도).
     * 빈 응답과 예외를 구분해 로그 — 콘솔에서 "Error" 로 필터 가능.
     */
    private suspend fun loadProductsIfNeeded() {
        if (removeAdsOffering != null) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        // 콜백 미도착 시 구매 스피너가 무한 대기하지 않도록 연결과 동일한 timeout.
        val details = withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { cont ->
                // Play Billing 8+ 콜백은 List<ProductDetails> 가 아니라 QueryProductDetailsResult 를 준다.
                billingClient.queryProductDetailsAsync(params) { result, queryResult ->
                    if (!cont.isActive) return@queryProductDetailsAsync
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(queryResult.productDetailsList.firstOrNull())
                    } else {
                        Timber.tag(TAG).w("[IAP] 상품 로드 Error: %d — %s", result.responseCode, result.debugMessage)
                        cont.resume(null)
                    }
                }
            }
        }
        if (details == null) {
            Timber.tag(TAG).w(
                "[IAP] 상품 로드 Error(빈 응답 — Play Console 미등록/미활성 가능): %s",
                REMOVE_ADS_PRODUCT_ID,
            )
            return
        }
        // v8 부터 일회성 상품도 구매 옵션(오퍼) 목록으로 내려온다. 가격과 결제 토큰을 같은 오퍼에서 취한다.
        val offer = IapOfferSelection.select(
            details.oneTimePurchaseOfferDetailsList.orEmpty().map {
                // 토큰이 비면 select 가 걸러낸다(결제 불가 오퍼).
                IapOfferSelection.Offer(
                    offerToken = it.offerToken.orEmpty(),
                    formattedPrice = it.formattedPrice,
                )
            },
        )
        if (offer == null) {
            Timber.tag(TAG).w(
                "[IAP] 상품 로드 Error(구매 옵션 없음 — Play Console 구매 옵션 미설정 가능): %s",
                REMOVE_ADS_PRODUCT_ID,
            )
            return
        }
        removeAdsOffering = RemoveAdsOffering(product = details, offerToken = offer.offerToken)
        _priceText.value = offer.formattedPrice
    }

    // MARK: - Connection

    /** 연결 보장 후 [block] 실행. 실패 시 조용히 스킵 (앱 흐름 안 막음). */
    private fun connectAndRun(block: suspend () -> Unit) {
        scope.launch {
            try {
                if (ensureConnected()) block()
            } catch (e: Throwable) {
                Timber.tag(TAG).w(e, "[IAP] 작업 Error")
            }
        }
    }

    /**
     * BillingClient 연결 보장 (이미 연결됐으면 즉시 true).
     * Play 서비스가 콜백을 영영 안 주는 경우 UI 스피너가 무한 대기하지 않도록 timeout 을 건다.
     */
    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        // CompletableDeferred.complete 는 멱등이라 setup/disconnected 이중 콜백에도 1회만 반영된다.
        val connected = CompletableDeferred<Boolean>()
        val listener = object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                if (!ok) {
                    Timber.tag(TAG).w("[IAP] 연결 Error: %d — %s", result.responseCode, result.debugMessage)
                }
                connected.complete(ok)
            }

            override fun onBillingServiceDisconnected() {
                Timber.tag(TAG).w("[IAP] 연결 Error(서비스 끊김)")
                connected.complete(false)
            }
        }
        // startConnection 내부의 bindService 바인더 호출이 Play 서비스 지연 시 메인 스레드를 막아
        // ANR 을 냈다(Crashlytics 1.4.1 "thread waiting for a binder transaction" @ensureConnected).
        // 바인딩은 IO 스레드에서 시작하고 결과 콜백만 기다린다(콜백은 라이브러리가 메인으로 전달).
        withContext(Dispatchers.IO) {
            runCatching { billingClient.startConnection(listener) }.onFailure { e ->
                Timber.tag(TAG).w(e, "[IAP] 연결 Error(startConnection 예외)")
                connected.complete(false)
            }
        }
        return withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) { connected.await() } ?: run {
            Timber.tag(TAG).w("[IAP] 연결 Error(timeout)")
            false
        }
    }

    private fun Purchase.toSnapshot() = IapPurchaseSnapshot(
        productIds = products,
        state = purchaseState,
        isAcknowledged = isAcknowledged,
        purchaseToken = purchaseToken,
    )

    companion object {
        /** 평생 광고 제거 one-time(INAPP) 상품 ID (Play Console 등록값). */
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads_lifetime"

        private const val TAG = "IAP"
        private const val PREFS_NAME = "iap_prefs"
        private const val KEY_AD_FREE_CACHED = "iap.adFree.cached"
        private const val FAIL_PRODUCT_UNAVAILABLE = -1
        private const val CONNECT_TIMEOUT_MILLIS = 10_000L
    }
}

/**
 * 구매 흐름 결과 이벤트 — 설정 화면이 구독해 다이얼로그/피드백을 띄운다.
 */
sealed interface PurchaseEvent {
    data object Completed : PurchaseEvent
    data class Failed(val message: String) : PurchaseEvent
    data object Cancelled : PurchaseEvent
}
