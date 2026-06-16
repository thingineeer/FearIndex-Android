package th1ngjin.fearindex.presentation.feature.marketdetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import th1ngjin.fearindex.core.util.MarketQuoteFormat
import th1ngjin.fearindex.domain.entity.CryptoPrice
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.component.SegmentedPicker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// 한국식 등락 색상 (지수·환율: 상승 빨강 / 하락 파랑)
private val UpColorKr = Color(0xFFE53935)
private val DownColorKr = Color(0xFF1E88E5)
// 암호화폐 (서양식: 상승 초록 / 하락 빨강)
private val UpColorCrypto = Color(0xFF43A047)
private val DownColorCrypto = Color(0xFFE53935)

private const val INITIAL_TAB_INDICES = 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDetailScreen(
    onBack: () -> Unit,
    initialTab: MarketDetailTab = MarketDetailTab.INDICES,
    viewModel: MarketDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val tabs = listOf(
        stringResource(R.string.market_detail_tab_indices),
        stringResource(R.string.market_detail_tab_exchange),
        stringResource(R.string.market_detail_tab_crypto),
    )
    var selectedIndex by remember {
        mutableStateOf(if (initialTab == MarketDetailTab.CRYPTO) 2 else INITIAL_TAB_INDICES)
    }
    val selectedTab = when (selectedIndex) {
        1 -> MarketDetailTab.EXCHANGE
        2 -> MarketDetailTab.CRYPTO
        else -> MarketDetailTab.INDICES
    }

    // 쿨다운 시계 — 15초마다 now 갱신 (iOS keepCooldownClockFresh).
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val cooldownRemaining = MarketDetailViewModel.cooldownRemainingMillis(uiState, nowMillis)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.market_detail_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SegmentedPicker(
                items = tabs,
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 갱신 시각 + 새로고침
            UpdatedRow(
                updatedAt = MarketDetailViewModel.latestUpdatedAt(uiState, selectedTab),
                isRefreshing = uiState.isRefreshing,
                cooldownRemainingMillis = cooldownRemaining,
                onRefresh = { viewModel.refresh(System.currentTimeMillis()) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                LoadingBox()
            } else {
                when (selectedTab) {
                    // 지수 탭은 DXY(환율 탭 전용) 제외
                    MarketDetailTab.INDICES -> IndicesContent(uiState.indices.filter { it.symbol != "DX-Y.NYB" })
                    MarketDetailTab.EXCHANGE -> ExchangeContent(uiState.usdKrwRate, uiState.indices)
                    MarketDetailTab.CRYPTO -> CryptoContent(uiState.cryptoPrices)
                }
            }
        }
    }
}

@Composable
private fun UpdatedRow(
    updatedAt: Instant?,
    isRefreshing: Boolean,
    cooldownRemainingMillis: Long,
    onRefresh: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (updatedAt != null) {
                Text(
                    text = stringResource(R.string.market_detail_updated_at, formatTimestamp(updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing && cooldownRemainingMillis <= 0L,
                modifier = Modifier.size(32.dp),
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.market_detail_refresh),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (cooldownRemainingMillis > 0L) {
            Text(
                text = stringResource(R.string.market_detail_refresh_cooldown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IndicesContent(indices: List<MarketIndex>) {
    if (indices.isEmpty()) {
        EmptyBox(stringResource(R.string.market_detail_empty_indices))
        return
    }
    RowGroup {
        indices.forEachIndexed { i, index ->
            MarketIndexRow(index)
            if (i < indices.lastIndex) RowDivider()
        }
    }
}

@Composable
private fun ExchangeContent(rate: ExchangeRateQuote?, indices: List<MarketIndex>) {
    val dollarIndex = indices.firstOrNull { it.symbol == "DX-Y.NYB" }
    if (rate == null && dollarIndex == null) {
        EmptyBox(stringResource(R.string.market_detail_empty_exchange))
        return
    }
    RowGroup {
        if (rate != null) {
            ExchangeRow(rate)
            if (dollarIndex != null) RowDivider()
        }
        if (dollarIndex != null) MarketIndexRow(dollarIndex)
    }
}

@Composable
private fun CryptoContent(prices: List<CryptoPrice>) {
    if (prices.isEmpty()) {
        EmptyBox(stringResource(R.string.market_detail_empty_crypto))
        return
    }
    RowGroup {
        prices.forEachIndexed { i, p ->
            CryptoRow(p)
            if (i < prices.lastIndex) RowDivider()
        }
    }
}

// ── Rows ──

@Composable
private fun MarketIndexRow(index: MarketIndex) {
    val type = index.type
    val title = type?.let { stringResource(stringIdFor(it.detailDisplayNameKey)) } ?: index.name
    val subtitle = type?.detailSubtitle ?: index.symbol.removePrefix("^")
    QuoteRow(
        title = title,
        subtitle = subtitle,
        price = MarketQuoteFormat.indexPriceText(index.price),
        change = MarketQuoteFormat.changePercentText(index.changePercent),
        tint = if (index.isPositive) UpColorKr else DownColorKr,
    )
}

@Composable
private fun ExchangeRow(rate: ExchangeRateQuote) {
    QuoteRow(
        title = "${rate.baseCode}/${rate.targetCode}",
        subtitle = "${rate.baseCode}/${rate.targetCode}",
        price = MarketQuoteFormat.exchangePriceText(rate.rate),
        change = MarketQuoteFormat.changePercentText(rate.changePercent),
        tint = if (rate.isPositive) UpColorKr else DownColorKr,
    )
}

@Composable
private fun CryptoRow(p: CryptoPrice) {
    QuoteRow(
        title = p.name,
        subtitle = p.symbol,
        price = MarketQuoteFormat.cryptoPriceText(p.price),
        change = MarketQuoteFormat.changePercentText(p.change24h),
        tint = if (p.isPositive) UpColorCrypto else DownColorCrypto,
    )
}

@Composable
private fun QuoteRow(title: String, subtitle: String, price: String, change: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = price,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = change,
                style = MaterialTheme.typography.bodySmall,
                color = tint,
            )
        }
    }
}

// ── Containers ──

@Composable
private fun RowGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
    ) { content() }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun EmptyBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp)
    }
}

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun formatTimestamp(instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val zoned = instant.atZone(zone)
    val abbrev = zone.rules.getStandardOffset(instant).let { "GMT${it.id.replace(":00", "")}" }
        .let { if (it == "GMT") "GMT+0" else it }
    return "${timestampFormatter.withLocale(Locale.getDefault()).format(zoned)} $abbrev"
}

/** displayNameKey(문자열) → R.string resId. presentation 에서만 매핑. */
@Composable
private fun stringIdFor(key: String): Int {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return ctx.resources.getIdentifier(key, "string", ctx.packageName)
}
