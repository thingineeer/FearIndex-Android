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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckStatus as DomainStuckStatus
import th1ngjin.fearindex.presentation.common.ratingLabel
import th1ngjin.fearindex.presentation.component.SegmentedPicker
import th1ngjin.fearindex.presentation.component.StuckCounterCard
import th1ngjin.fearindex.presentation.component.StuckStatus as UiStuckStatus
import th1ngjin.fearindex.presentation.di.AnalyticsEntryPoint
import th1ngjin.fearindex.presentation.feature.home.FearIndexState
import th1ngjin.fearindex.presentation.feature.home.HomeViewModel
import th1ngjin.fearindex.presentation.theme.fearScoreColor

@Composable
fun VoteScreen(
    viewModel: HomeViewModel,
    voteViewModel: VoteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType = uiState.selectedType

    val stuckResult by voteViewModel.resultFor(selectedType).collectAsState()
    val myStuckStatus by voteViewModel.myStatusFor(selectedType).collectAsState()

    val context = LocalContext.current
    val analytics = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java)
            .analyticsManager()
    }
    LaunchedEffect(selectedType) {
        analytics.log(
            AnalyticsEvent.투표탭진입(
                지수타입 = if (selectedType == FearIndexType.MARKET) "시장" else "암호화폐",
            ),
        )
    }

    val selectedIndex = when (selectedType) {
        FearIndexType.MARKET -> 0
        FearIndexType.CRYPTO -> 1
    }

    val currentState = when (selectedType) {
        FearIndexType.MARKET -> uiState.marketState
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
            text = "투표",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SegmentedPicker(
            items = listOf("시장", "암호화폐"),
            selectedIndex = selectedIndex,
            onItemSelected = { index ->
                val newType = if (index == 0) FearIndexType.MARKET else FearIndexType.CRYPTO
                if (newType != selectedType) {
                    val previousLabel = if (selectedType == FearIndexType.MARKET) "시장" else "암호화폐"
                    val newLabel = if (newType == FearIndexType.MARKET) "시장" else "암호화폐"
                    analytics.log(
                        AnalyticsEvent.투표세그먼트전환(
                            지수타입 = newLabel,
                            이전타입 = previousLabel,
                        ),
                    )
                }
                viewModel.selectIndexType(newType)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentState is FearIndexState.Loaded) {
            CurrentScoreHeader(
                score = currentState.fearIndex.roundedScore,
                previousClose = currentState.fearIndex.previousClose,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stuck Counter Card — 실제 서버 데이터 + Optimistic 토글
        StuckCounterCard(
            stuckPercentage = stuckResult.stuckPercentage.toFloat(),
            myStatus = myStuckStatus.toUi(),
            onToggle = { newStatus ->
                val score = (currentState as? FearIndexState.Loaded)?.fearIndex?.roundedScore ?: 0
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
            onInfoClick = { /* TODO: show info sheet */ },
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CurrentScoreHeader(
    score: Int,
    previousClose: Double?,
) {
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
                text = "현재 지수",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            val diff = score - previousClose.toInt()
            val sign = if (diff >= 0) "+" else ""
            Text(
                text = "전일 대비 $sign${String.format("%.1f", score - previousClose)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// MARK: - Domain ↔ UI 매핑

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
