package th1ngjin.fearindex.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import th1ngjin.fearindex.data.datasource.BinanceFuturesApi
import th1ngjin.fearindex.data.datasource.CNNFearGreedApi
import th1ngjin.fearindex.data.datasource.CryptoFearIndexApi
import th1ngjin.fearindex.data.datasource.FinraShortVolumeApi
import th1ngjin.fearindex.data.datasource.KospiFearIndexApi
import th1ngjin.fearindex.data.datasource.MarketIndexApi
import th1ngjin.fearindex.data.repository.AssetPriceClosesRepositoryImpl
import th1ngjin.fearindex.data.repository.AssetShortPressureRepositoryImpl
import th1ngjin.fearindex.domain.repository.AssetPriceClosesRepository
import th1ngjin.fearindex.domain.repository.AssetShortPressureRepository
import th1ngjin.fearindex.domain.usecase.GetAssetRSIUseCase
import th1ngjin.fearindex.domain.usecase.GetAssetShortPressureUseCase
import th1ngjin.fearindex.data.repository.CryptoFearIndexRepositoryImpl
import th1ngjin.fearindex.data.repository.FearIndexRepositoryImpl
import th1ngjin.fearindex.data.repository.KospiFearIndexRepositoryImpl
import th1ngjin.fearindex.data.repository.MarketIndexRepositoryImpl
import th1ngjin.fearindex.data.repository.MarketDetailRepositoryImpl
import th1ngjin.fearindex.data.datasource.YahooChartApi
import th1ngjin.fearindex.data.datasource.NaverFinanceApi
import th1ngjin.fearindex.data.datasource.CoinGeckoApi
import th1ngjin.fearindex.data.datasource.ExchangeRateApi
import th1ngjin.fearindex.domain.repository.MarketDetailRepository
import th1ngjin.fearindex.domain.usecase.GetMarketIndicesDetailUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoPricesUseCase
import th1ngjin.fearindex.domain.usecase.GetUsdKrwRateUseCase
import th1ngjin.fearindex.data.repository.ReturnDataRepositoryImpl
import th1ngjin.fearindex.data.repository.SimilarEventsRepositoryImpl
import th1ngjin.fearindex.data.repository.StuckCounterRepositoryImpl
import th1ngjin.fearindex.data.repository.NotificationRepositoryImpl
import th1ngjin.fearindex.data.repository.VoteRepositoryImpl
import th1ngjin.fearindex.data.service.StuckStatusDebouncerImpl
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository
import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.domain.repository.FearIndexRepository
import th1ngjin.fearindex.domain.repository.KospiFearIndexRepository
import th1ngjin.fearindex.domain.repository.MarketIndexRepository
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.repository.ReturnDataRepository
import th1ngjin.fearindex.domain.repository.SimilarEventsRepository
import th1ngjin.fearindex.domain.repository.StuckCounterRepository
import th1ngjin.fearindex.domain.repository.VoteRepository
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import th1ngjin.fearindex.domain.service.StuckStatusDebouncer
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetKospiFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetKospiFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetMarketIndicesUseCase
import th1ngjin.fearindex.domain.usecase.GetVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.ObserveStuckCounterUseCase
import th1ngjin.fearindex.domain.usecase.ObserveVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.SubmitStuckStatusUseCase
import th1ngjin.fearindex.domain.usecase.SubmitVoteUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {
    @Binds
    @Singleton
    abstract fun bindFearIndexRepository(impl: FearIndexRepositoryImpl): FearIndexRepository

    @Binds
    @Singleton
    abstract fun bindCryptoFearIndexRepository(impl: CryptoFearIndexRepositoryImpl): CryptoFearIndexRepository

    @Binds
    @Singleton
    abstract fun bindKospiFearIndexRepository(impl: KospiFearIndexRepositoryImpl): KospiFearIndexRepository

    @Binds
    @Singleton
    abstract fun bindStuckCounterRepository(impl: StuckCounterRepositoryImpl): StuckCounterRepository

    @Binds
    @Singleton
    abstract fun bindMarketIndexRepository(impl: MarketIndexRepositoryImpl): MarketIndexRepository

    @Binds
    @Singleton
    abstract fun bindMarketDetailRepository(impl: MarketDetailRepositoryImpl): MarketDetailRepository

    @Binds
    @Singleton
    abstract fun bindVoteRepository(impl: VoteRepositoryImpl): VoteRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindReturnDataRepository(impl: ReturnDataRepositoryImpl): ReturnDataRepository

    @Binds
    @Singleton
    abstract fun bindSimilarEventsRepository(impl: SimilarEventsRepositoryImpl): SimilarEventsRepository

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: StuckCounterStorage): DeviceIdProvider

    @Binds
    @Singleton
    abstract fun bindStuckStatusDebouncer(impl: StuckStatusDebouncerImpl): StuckStatusDebouncer

    @Binds
    @Singleton
    abstract fun bindAssetPriceClosesRepository(
        impl: AssetPriceClosesRepositoryImpl,
    ): AssetPriceClosesRepository

    @Binds
    @Singleton
    abstract fun bindAssetShortPressureRepository(
        impl: AssetShortPressureRepositoryImpl,
    ): AssetShortPressureRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    @Named("cnn")
    fun provideCNNOkHttpClient(baseClient: OkHttpClient): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val request = original.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36",
                    )
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.cnn.com/markets/fear-and-greed")
                    .header("Origin", "https://www.cnn.com")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideCNNApi(@Named("cnn") client: OkHttpClient): CNNFearGreedApi {
        return Retrofit.Builder()
            .baseUrl("https://production.dataviz.cnn.io/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CNNFearGreedApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMarketIndexApi(client: OkHttpClient): MarketIndexApi {
        return Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MarketIndexApi::class.java)
    }

    // 시장 상세: Yahoo/Naver 는 브라우저 User-Agent 가 필요 (봇 차단 방지, iOS/Watch parity).
    @Provides
    @Singleton
    @Named("market")
    fun provideMarketOkHttpClient(baseClient: OkHttpClient): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideYahooChartApi(@Named("market") client: OkHttpClient): YahooChartApi =
        Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YahooChartApi::class.java)

    @Provides
    @Singleton
    fun provideNaverFinanceApi(@Named("market") client: OkHttpClient): NaverFinanceApi =
        Retrofit.Builder()
            .baseUrl("https://m.stock.naver.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NaverFinanceApi::class.java)

    @Provides
    @Singleton
    fun provideCoinGeckoApi(client: OkHttpClient): CoinGeckoApi =
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CoinGeckoApi::class.java)

    @Provides
    @Singleton
    fun provideExchangeRateApi(client: OkHttpClient): ExchangeRateApi =
        Retrofit.Builder()
            // @Url full-URL 만 쓰므로 baseUrl 은 placeholder.
            .baseUrl("https://latest.currency-api.pages.dev/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ExchangeRateApi::class.java)

    @Provides
    @Singleton
    fun provideGetMarketIndicesDetailUseCase(repository: MarketDetailRepository): GetMarketIndicesDetailUseCase =
        GetMarketIndicesDetailUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCryptoPricesUseCase(repository: MarketDetailRepository): GetCryptoPricesUseCase =
        GetCryptoPricesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetUsdKrwRateUseCase(repository: MarketDetailRepository): GetUsdKrwRateUseCase =
        GetUsdKrwRateUseCase(repository)

    @Provides
    @Singleton
    fun provideCryptoApi(client: OkHttpClient): CryptoFearIndexApi {
        return Retrofit.Builder()
            .baseUrl("https://api.alternative.me/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CryptoFearIndexApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKospiApi(client: OkHttpClient): KospiFearIndexApi {
        return Retrofit.Builder()
            .baseUrl("https://fear-index-a4f4b.web.app/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(KospiFearIndexApi::class.java)
    }

    // ============================================================
    // RSI / 공매도 보조 지표 (iOS AssetPriceClose/AssetShortRatio DataSource 대응)
    // ============================================================

    @Provides
    @Singleton
    fun provideFinraShortVolumeApi(client: OkHttpClient): FinraShortVolumeApi =
        Retrofit.Builder()
            .baseUrl("https://cdn.finra.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FinraShortVolumeApi::class.java)

    @Provides
    @Singleton
    fun provideBinanceFuturesApi(client: OkHttpClient): BinanceFuturesApi =
        Retrofit.Builder()
            .baseUrl("https://fapi.binance.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BinanceFuturesApi::class.java)

    @Provides
    @Singleton
    fun provideGetAssetRSIUseCase(repository: AssetPriceClosesRepository): GetAssetRSIUseCase =
        GetAssetRSIUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAssetShortPressureUseCase(
        repository: AssetShortPressureRepository,
    ): GetAssetShortPressureUseCase =
        GetAssetShortPressureUseCase(repository)

    @Provides
    @Singleton
    fun provideGetFearIndexUseCase(repository: FearIndexRepository): GetFearIndexUseCase =
        GetFearIndexUseCase(repository)

    @Provides
    @Singleton
    fun provideGetFearIndexHistoryUseCase(repository: FearIndexRepository): GetFearIndexHistoryUseCase =
        GetFearIndexHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCryptoFearIndexUseCase(repository: CryptoFearIndexRepository): GetCryptoFearIndexUseCase =
        GetCryptoFearIndexUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCryptoFearIndexHistoryUseCase(repository: CryptoFearIndexRepository): GetCryptoFearIndexHistoryUseCase =
        GetCryptoFearIndexHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideGetKospiFearIndexUseCase(repository: KospiFearIndexRepository): GetKospiFearIndexUseCase =
        GetKospiFearIndexUseCase(repository)

    @Provides
    @Singleton
    fun provideGetKospiFearIndexHistoryUseCase(repository: KospiFearIndexRepository): GetKospiFearIndexHistoryUseCase =
        GetKospiFearIndexHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideGetMarketIndicesUseCase(repository: MarketIndexRepository): GetMarketIndicesUseCase =
        GetMarketIndicesUseCase(repository)

    // ============================================================
    // Firebase (Stuck Counter)
    // ============================================================

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-northeast3")

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideSubmitStuckStatusUseCase(repository: StuckCounterRepository): SubmitStuckStatusUseCase =
        SubmitStuckStatusUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveStuckCounterUseCase(repository: StuckCounterRepository): ObserveStuckCounterUseCase =
        ObserveStuckCounterUseCase(repository)

    // ============================================================
    // Firebase (Vote — Buy/Hold/Sell)
    // ============================================================

    @Provides
    @Singleton
    fun provideSubmitVoteUseCase(repository: VoteRepository): SubmitVoteUseCase =
        SubmitVoteUseCase(repository)

    @Provides
    @Singleton
    fun provideGetVoteResultUseCase(repository: VoteRepository): GetVoteResultUseCase =
        GetVoteResultUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveVoteResultUseCase(repository: VoteRepository): ObserveVoteResultUseCase =
        ObserveVoteResultUseCase(repository)
}
