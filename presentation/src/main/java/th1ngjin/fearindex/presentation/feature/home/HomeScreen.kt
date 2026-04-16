package th1ngjin.fearindex.presentation.feature.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.util.ShareUrlBuilder
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.domain.entity.SimilarEventsResult
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus as DomainStuckStatus
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.ratingLabel
import th1ngjin.fearindex.presentation.component.AdBanner
import th1ngjin.fearindex.presentation.component.ComparisonCard
import th1ngjin.fearindex.presentation.component.FearGaugeView
import th1ngjin.fearindex.presentation.component.FearIndexSkeletonView
import th1ngjin.fearindex.presentation.component.InsightDetailSheet
import th1ngjin.fearindex.presentation.component.InsightTeaserCard
import th1ngjin.fearindex.presentation.component.SimilarEventsCard
import th1ngjin.fearindex.presentation.component.SegmentedPicker
import th1ngjin.fearindex.presentation.component.StuckCounterCard
import th1ngjin.fearindex.presentation.component.StuckStatus as UiStuckStatus
import th1ngjin.fearindex.presentation.di.AnalyticsEntryPoint
import th1ngjin.fearindex.presentation.feature.insight.InsightViewModel
import th1ngjin.fearindex.presentation.feature.similarevents.SimilarEventsViewModel
import th1ngjin.fearindex.presentation.feature.vote.VoteViewModel
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val insightViewModel: InsightViewModel = hiltViewModel()
    val insightState by insightViewModel.uiState.collectAsState()
    val voteViewModel: VoteViewModel = hiltViewModel()
    val similarEventsViewModel: SimilarEventsViewModel = hiltViewModel()

    // InsightViewModel이 HomeViewModel을 관찰
    LaunchedEffect(Unit) {
        insightViewModel.observeHome(viewModel)
    }

    val selectedType = uiState.selectedType
    val selectedIndex = when (selectedType) {
        FearIndexType.MARKET -> 0
        FearIndexType.CRYPTO -> 1
    }
    val currentState = when (selectedType) {
        FearIndexType.MARKET -> uiState.marketState
        FearIndexType.CRYPTO -> uiState.cryptoState
    }

    // 물림 카운터 상태
    val stuckResult by voteViewModel.resultFor(selectedType).collectAsState()
    val myStuckStatus by voteViewModel.myStatusFor(selectedType).collectAsState()

    // SimilarEvents 실시간 구독
    val similarEventsResult by similarEventsViewModel.resultFor(selectedType).collectAsState()

    val context = LocalContext.current
    val analytics = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java)
            .analyticsManager()
    }

    // 인사이트 상세 BottomSheet
    insightState.selectedInsight?.let { insight ->
        InsightDetailSheet(
            insight = insight,
            onDismiss = insightViewModel::dismissDetail,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Title: "공포 탐욕 지수" (center) + share button (right)
        val loadedScore = (currentState as? FearIndexState.Loaded)?.fearIndex?.roundedScore
        val loadedRating = loadedScore?.let { ratingLabel(it) }
        TitleBar(
            currentScore = loadedScore,
            ratingLabel = loadedRating,
            onShareClicked = {
                if (loadedScore != null) {
                    val typeLabel = if (selectedType == FearIndexType.MARKET) "시장" else "암호화폐"
                    analytics.log(AnalyticsEvent.공유버튼탭(지수타입 = typeLabel, 현재점수 = loadedScore))
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Segmented Picker: 시장 / 암호화폐
        SegmentedPicker(
            items = listOf("시장", "암호화폐"),
            selectedIndex = selectedIndex,
            onItemSelected = { index ->
                val type = if (index == 0) FearIndexType.MARKET else FearIndexType.CRYPTO
                viewModel.selectIndexType(type)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Ticker (auto-rotating market index bar)
        if (uiState.marketIndices.isNotEmpty()) {
            TickerView(indices = uiState.marketIndices)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4+. Content (gauge → comparison → ad → teaser → stuck → timestamp)
        when (currentState) {
            is FearIndexState.Loading -> FearIndexSkeletonView()
            is FearIndexState.Loaded -> LoadedContent(
                fearIndex = currentState.fearIndex,
                indexType = selectedType,
                insights = insightState.insights,
                onInsightClick = insightViewModel::selectInsight,
                similarEventsResult = similarEventsResult,
                stuckResult = stuckResult,
                myStuckStatus = myStuckStatus.toUi(),
                onStuckToggle = { newStatus ->
                    val score = currentState.fearIndex.roundedScore
                    analytics.log(
                        AnalyticsEvent.투표참여(
                            선택 = when (newStatus) {
                                UiStuckStatus.STUCK -> "물렸어요"
                                UiStuckStatus.NOT_STUCK -> "안물렸어요"
                                UiStuckStatus.NO_RESPONSE -> "취소"
                            },
                            지수타입 = if (selectedType == FearIndexType.MARKET) "시장" else "암호화폐",
                            현재점수 = score,
                        ),
                    )
                    voteViewModel.toggleStuckStatus(selectedType, newStatus.toDomain())
                },
            )
            is FearIndexState.Error -> ErrorContent(
                message = currentState.message,
                onRetry = viewModel::refresh,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Title Bar
// ---------------------------------------------------------------------------

@Composable
private fun TitleBar(
    currentScore: Int? = null,
    ratingLabel: String? = null,
    onShareClicked: () -> Unit = {},
) {
    // 공유 메시지의 제목/본문도 다국어. 점수와 등급은 호출부에서 ratingLabel(score)로 미리 주입.
    val shareTitle = stringResource(R.string.home_title) // "공포 탐욕 지수" / "Fear & Greed Index" 등
    val shareTemplate = stringResource(R.string.share_message_template, currentScore ?: 0, ratingLabel ?: "")
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "공포 탐욕 지수",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        val chooserTitle = stringResource(R.string.share_chooser_title)
        IconButton(
            onClick = {
                onShareClicked()
                // Android Intent.ACTION_SEND — 카카오톡/문자/메모 등 공유 가능 대상 전체로 전달.
                // 모든 문자열은 strings.xml(45 locale)에서 가져오므로 하드코딩 금지.
                val shareUrl = ShareUrlBuilder.build(
                    score = currentScore ?: 0,
                    type = "market",
                    rating = ratingLabel ?: "",
                )
                val shareText = "$shareTemplate\n$shareUrl"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                }
                val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "공유",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Ticker View (auto-rotating market index bar)
// ---------------------------------------------------------------------------

@Composable
private fun TickerView(
    indices: List<MarketIndex>,
    modifier: Modifier = Modifier,
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(indices.size) {
        if (indices.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(3_000)
            currentIndex = (currentIndex + 1) % indices.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ticker_rotation",
        ) { index ->
            val item = indices[index]
            val priceFormatted = formatPrice(item.price)
            val arrow = if (item.isPositive) "▲" else "▼"
            val percentFormatted = "$arrow%.2f%%".format(
                kotlin.math.abs(item.changePercent),
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = priceFormatted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.size(6.dp))

                val capsuleColor = if (item.isPositive) {
                    Color(0xFFE53935)
                } else {
                    Color(0xFF1976D2)
                }
                Text(
                    text = percentFormatted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = capsuleColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(capsuleColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private val priceFormatter: java.text.DecimalFormat by lazy {
    java.text.DecimalFormat("#,##0.00")
}

private fun formatPrice(price: Double): String = priceFormatter.format(price)

// ---------------------------------------------------------------------------
// Loading / Error states
// ---------------------------------------------------------------------------

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Spacer(modifier = Modifier.height(80.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onRetry) {
        Text("재시도")
    }
}

// ---------------------------------------------------------------------------
// Loaded Content: Gauge → Comparison → Ad → Teaser → Stuck → Timestamp
// (iOS 홈 화면 순서와 동일)
// ---------------------------------------------------------------------------

@Composable
private fun LoadedContent(
    fearIndex: FearIndex,
    indexType: FearIndexType,
    insights: List<MarketInsight> = emptyList(),
    onInsightClick: (MarketInsight) -> Unit = {},
    similarEventsResult: SimilarEventsResult = SimilarEventsResult.EMPTY,
    stuckResult: StuckCounterResult = StuckCounterResult.EMPTY,
    myStuckStatus: UiStuckStatus = UiStuckStatus.NO_RESPONSE,
    onStuckToggle: (UiStuckStatus) -> Unit = {},
) {
    val score = fearIndex.roundedScore

    // 1. Fear Gauge
    FearGaugeView(score = score)

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Comparison Card
    ComparisonCard(
        currentScore = score,
        previousClose = fearIndex.previousClose,
        previous1Week = fearIndex.previous1Week,
        previous1Month = fearIndex.previous1Month,
        previous1Year = if (indexType == FearIndexType.CRYPTO) null else fearIndex.previous1Year,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 3. AdMob 배너 광고
    AdBanner()

    // 3.5. SimilarEvents 카드 — "지금과 비슷했던 시기" (v1.7.9)
    if (similarEventsResult.matches.isNotEmpty() || similarEventsResult.aggregateStats != null) {
        Spacer(modifier = Modifier.height(16.dp))
        SimilarEventsCard(result = similarEventsResult)
    }

    // 4. 인사이트 티저 카드 — 현재 점수 기준 통계(전 구간). 첫 번째 인사이트 1개.
    val teaserInsight = insights.firstOrNull()
    if (teaserInsight != null) {
        Spacer(modifier = Modifier.height(16.dp))
        InsightTeaserCard(
            insight = teaserInsight,
            onClick = { onInsightClick(teaserInsight) },
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 5. 물림 카운터
    StuckCounterCard(
        stuckPercentage = stuckResult.stuckPercentage.toFloat(),
        myStatus = myStuckStatus,
        onToggle = onStuckToggle,
        onInfoClick = { /* TODO: show info sheet */ },
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 6. Timestamp
    Text(
        text = "Updated: ${timestampFormatter.format(fearIndex.timestamp)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )
}

private val timestampFormatter: DateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
}

// ---------------------------------------------------------------------------
// Domain <-> UI 매핑 (VoteScreen과 동일)
// ---------------------------------------------------------------------------

private fun DomainStuckStatus.toUi(): UiStuckStatus = when (this) {
    DomainStuckStatus.STUCK -> UiStuckStatus.STUCK
    DomainStuckStatus.SAFE -> UiStuckStatus.NOT_STUCK
    DomainStuckStatus.NONE -> UiStuckStatus.NO_RESPONSE
}

private fun UiStuckStatus.toDomain(): DomainStuckStatus = when (this) {
    UiStuckStatus.STUCK -> DomainStuckStatus.STUCK
    UiStuckStatus.NOT_STUCK -> DomainStuckStatus.SAFE
    UiStuckStatus.NO_RESPONSE -> DomainStuckStatus.NONE
}
