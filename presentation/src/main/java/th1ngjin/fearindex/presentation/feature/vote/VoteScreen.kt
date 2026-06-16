package th1ngjin.fearindex.presentation.feature.vote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.util.IndexTimestampFormatter
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckStatus as DomainStuckStatus
import th1ngjin.fearindex.presentation.BuildConfig
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.ratingLabel
import th1ngjin.fearindex.presentation.component.SegmentedPicker
import th1ngjin.fearindex.presentation.component.AdBanner
import th1ngjin.fearindex.presentation.component.RepresentativeIndexInfoSheet
import th1ngjin.fearindex.presentation.component.StuckCounterCard
import th1ngjin.fearindex.presentation.component.StuckDetailSheet
import th1ngjin.fearindex.presentation.component.StuckStatus as UiStuckStatus
import th1ngjin.fearindex.presentation.di.AnalyticsEntryPoint
import th1ngjin.fearindex.presentation.feature.home.FearIndexState
import th1ngjin.fearindex.presentation.feature.home.HomeViewModel
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import kotlin.math.roundToInt

// MARK: - Analytics Constants (사용자 UI에 노출되지 않는 Analytics 이벤트 파라미터용 한국어 상수)

private const val ANALYTICS_TYPE_MARKET = "시장"
private const val ANALYTICS_TYPE_KOSPI = "코스피"
private const val ANALYTICS_TYPE_CRYPTO = "암호화폐"
private const val ANALYTICS_SCREEN_VOTE = "투표"
private const val ANALYTICS_STUCK = "물렸어요"
private const val ANALYTICS_NOT_STUCK = "안물렸어요"
private const val ANALYTICS_CANCEL = "취소"

@Composable
fun VoteScreen(
    viewModel: HomeViewModel,
    voteViewModel: VoteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType = uiState.selectedVoteType

    val stuckResult by voteViewModel.resultFor(selectedType).collectAsState()
    val myStuckStatus by voteViewModel.myStatusFor(selectedType).collectAsState()

    var showStuckDetail by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val analytics = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java)
            .analyticsManager()
    }
    LaunchedEffect(selectedType) {
        voteViewModel.ensureStuckCounterStarted(selectedType)
        analytics.log(
            AnalyticsEvent.투표탭진입(
                지수타입 = selectedType.analyticsLabel(),
            ),
        )
    }

    val selectedIndex = when (selectedType) {
        FearIndexType.MARKET -> 0
        FearIndexType.KOSPI -> 1
        FearIndexType.CRYPTO -> 2
    }

    val currentState = when (selectedType) {
        FearIndexType.MARKET -> uiState.marketState
        FearIndexType.KOSPI -> uiState.kospiState
        FearIndexType.CRYPTO -> uiState.cryptoState
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.tab_vote),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SegmentedPicker(
            items = listOf(
                stringResource(R.string.tab_market),
                stringResource(R.string.tab_kospi),
                stringResource(R.string.tab_crypto),
            ),
            selectedIndex = selectedIndex,
            onItemSelected = { index ->
                val newType = when (index) {
                    0 -> FearIndexType.MARKET
                    1 -> FearIndexType.KOSPI
                    else -> FearIndexType.CRYPTO
                }
                if (newType != selectedType) {
                    analytics.log(
                        AnalyticsEvent.투표세그먼트전환(
                            지수타입 = newType.analyticsLabel(),
                            이전타입 = selectedType.analyticsLabel(),
                        ),
                    )
                }
                viewModel.selectVoteIndexType(newType)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentState is FearIndexState.Loaded) {
            CurrentScoreHeader(
                score = currentState.fearIndex.roundedScore,
                previousClose = currentState.fearIndex.previousClose,
                indexType = selectedType,
                timestamp = currentState.fearIndex.timestamp,
                kospiIsFinal = uiState.kospiSnapshot?.isFinal,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stuck Counter Card
        StuckCounterCard(
            stuckPercentage = stuckResult.stuckPercentage.toFloat(),
            myStatus = myStuckStatus.toUi(),
            onToggle = { newStatus ->
                val score = (currentState as? FearIndexState.Loaded)?.fearIndex?.roundedScore ?: 0
                analytics.log(
                    AnalyticsEvent.투표참여(
                        선택 = when (newStatus) {
                            UiStuckStatus.STUCK -> ANALYTICS_STUCK
                            UiStuckStatus.NOT_STUCK -> ANALYTICS_NOT_STUCK
                            UiStuckStatus.NO_RESPONSE -> ANALYTICS_CANCEL
                        },
                        지수타입 = selectedType.analyticsLabel(),
                        현재점수 = score,
                    ),
                )
                voteViewModel.toggleStuckStatus(selectedType, newStatus.toDomain())
            },
            onInfoClick = { showStuckDetail = true },
            totalResponded = stuckResult.totalResponded,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AdMob 배너 광고 (VoteBanner 단위)
        AdBanner(
            adUnitId = BuildConfig.ADMOB_BANNER_VOTE,
            screenName = ANALYTICS_SCREEN_VOTE,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showStuckDetail) {
        StuckDetailSheet(
            result = stuckResult,
            onDismiss = { showStuckDetail = false },
        )
    }
}

@Composable
private fun CurrentScoreHeader(
    score: Int,
    previousClose: Double?,
    indexType: FearIndexType = FearIndexType.MARKET,
    timestamp: java.time.Instant? = null,
    kospiIsFinal: Boolean? = null,
) {
    // 대표 기준 설명 시트 (iOS RepresentativeIndexInfoSheet 대칭)
    var showInfoSheet by remember { mutableStateOf(false) }
    if (showInfoSheet) {
        RepresentativeIndexInfoSheet(onDismiss = { showInfoSheet = false })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.current_index_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // ⓘ 버튼 (iOS representativeIndexInfoButton 대칭)
                IconButton(
                    onClick = { showInfoSheet = true },
                    modifier = Modifier.size(18.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.representative_index_info_accessibility),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = fearScoreColor(score),
                )
                Text(
                    text = ratingLabel(score),
                    style = MaterialTheme.typography.labelMedium,
                    color = fearScoreColor(score),
                )
            }

            if (previousClose != null) {
                val diff = score - previousClose.roundToInt()
                val sign = if (diff >= 0) "+" else ""
                val delta = "$sign${String.format("%.1f", score - previousClose)}"
                Text(
                    text = stringResource(R.string.vs_previous_close_value, delta),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // KOSPI 장 상태줄 (iOS kospiCurrentStatusLine 대칭): KOSPI일 때만.
        if (indexType == FearIndexType.KOSPI && timestamp != null) {
            val statusText = stringResource(
                if (kospiIsFinal == true) R.string.kospi_status_final
                else R.string.kospi_status_intraday,
            )
            val updatedLabel = stringResource(R.string.kospi_current_updated_at)
            val updatedAt = IndexTimestampFormatter.format(timestamp, indexType)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$statusText · $updatedLabel: $updatedAt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

// MARK: - Domain <-> UI 매핑

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

private fun FearIndexType.analyticsLabel(): String = when (this) {
    FearIndexType.MARKET -> ANALYTICS_TYPE_MARKET
    FearIndexType.KOSPI -> ANALYTICS_TYPE_KOSPI
    FearIndexType.CRYPTO -> ANALYTICS_TYPE_CRYPTO
}
