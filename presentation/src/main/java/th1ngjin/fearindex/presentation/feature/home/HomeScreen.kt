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
import androidx.compose.material3.CircularProgressIndicator
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
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.ratingLabel
import th1ngjin.fearindex.presentation.component.AdBanner
import th1ngjin.fearindex.presentation.component.ComparisonCard
import th1ngjin.fearindex.presentation.component.FearGaugeView
import th1ngjin.fearindex.presentation.component.SegmentedPicker
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedIndex = when (uiState.selectedType) {
        FearIndexType.MARKET -> 0
        FearIndexType.CRYPTO -> 1
    }
    val currentState = when (uiState.selectedType) {
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
        // 1. Title: "공포 탐욕 지수" (center) + share button (right)
        // ratingLabel(score)는 Composable이므로 여기서 호출해 현재 locale의 번역값을 받는다.
        val loadedScore = (currentState as? FearIndexState.Loaded)?.fearIndex?.roundedScore
        val loadedRating = loadedScore?.let { ratingLabel(it) }
        TitleBar(currentScore = loadedScore, ratingLabel = loadedRating)

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
        TickerView()

        Spacer(modifier = Modifier.height(16.dp))

        // 4-6. Content (gauge + comparison + timestamp)
        when (currentState) {
            is FearIndexState.Loading -> LoadingContent()
            is FearIndexState.Loaded -> LoadedContent(
                fearIndex = currentState.fearIndex,
                indexType = uiState.selectedType,
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
private fun TitleBar(currentScore: Int? = null, ratingLabel: String? = null) {
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
                // Android Intent.ACTION_SEND — 카카오톡/문자/메모 등 공유 가능 대상 전체로 전달.
                // 모든 문자열은 strings.xml(45 locale)에서 가져오므로 하드코딩 금지.
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareTemplate)
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

private data class TickerItem(
    val name: String,
    val price: String,
    val changePercent: String,
    val isPositive: Boolean,
)

@Composable
private fun TickerView(modifier: Modifier = Modifier) {
    // TODO: Replace with real market index data when data pipeline is ready
    val items = remember {
        listOf(
            TickerItem("코스피", "2,756.82", "▲0.42%", true),
            TickerItem("S&P 500", "5,968.82", "▲2.76%", true),
            TickerItem("나스닥", "18,342.94", "▼0.18%", false),
            TickerItem("다우존스", "42,840.26", "▲1.56%", true),
        )
    }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3_000)
            currentIndex = (currentIndex + 1) % items.size
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
            val item = items[index]
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.bodySmall,
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
                    text = item.changePercent,
                    style = MaterialTheme.typography.labelSmall,
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

// ---------------------------------------------------------------------------
// Loading / Error states
// ---------------------------------------------------------------------------

@Composable
private fun LoadingContent() {
    Spacer(modifier = Modifier.height(80.dp))
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "불러오는 중...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

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
// Loaded Content: Gauge + Comparison + Timestamp
// ---------------------------------------------------------------------------

@Composable
private fun LoadedContent(fearIndex: FearIndex, indexType: FearIndexType) {
    // 4. Fear Gauge
    FearGaugeView(score = fearIndex.roundedScore)

    Spacer(modifier = Modifier.height(20.dp))

    // 5. Comparison Card
    ComparisonCard(
        currentScore = fearIndex.roundedScore,
        previousClose = fearIndex.previousClose,
        previous1Week = fearIndex.previous1Week,
        previous1Month = fearIndex.previous1Month,
        previous1Year = if (indexType == FearIndexType.CRYPTO) null else fearIndex.previous1Year,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 6. Timestamp
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
    Text(
        text = "Updated: ${formatter.format(fearIndex.timestamp)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 7. AdMob 배너 광고 (테스트 ID — 프로덕션 시 교체 필요)
    AdBanner()
}
