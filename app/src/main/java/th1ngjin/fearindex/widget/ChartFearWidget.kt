package th1ngjin.fearindex.widget

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import th1ngjin.fearindex.domain.entity.FearIndexType

/** 차트 렌더에 필요한 로드 결과. */
data class ChartPayload(
    val data: WidgetIndexData?,
    val history: List<Int>,
    val xLabels: List<String>,
)

/**
 * 차트 위젯 — 지수별 3종(Global/KOSPI/Crypto), 기간(3M~5Y) 탭 전환.
 * 지수 스왑은 로드 지연 때문에 폐기하고 위젯을 분리했다(2026-08-28 사용자 결정).
 *
 * ⚠️ 로드는 provideContent **안에서** LaunchedEffect 로 한다. provideGlance 에서 미리 로드하면
 * 탭 → 수 초간 무반응(로드 끝나야 첫 프레임)이라 "안 눌린다"로 느껴진다(2026-08-28 사용자 제보).
 * currentState 를 쓰면 탭 즉시 선택 강조가 바뀌고, 데이터는 스피너를 보여주며 뒤따라온다.
 */
abstract class ChartWidgetBase(private val fixedIndexType: FearIndexType) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val prefs = currentState<Preferences>()
            val period = WidgetChartPeriod.fromName(prefs[SetChartPeriodAction.CHART_PERIOD_KEY])
            val indexType = fixedIndexType
            val refreshing = prefs[RefreshWidgetsAction.REFRESHING_KEY] ?: false

            var payload by remember { mutableStateOf<ChartPayload?>(null) }
            // 지수/기간이 바뀌거나 새로고침이 끝나면(refreshing false 전환) 다시 로드
            LaunchedEffect(indexType, period, refreshing) {
                if (!refreshing) {
                    val loaded = loadChartPayload(context, indexType, period)
                    payload = loaded
                    if (loaded.data == null || loaded.history.size < 2) {
                        FearWidgetUpdateWorker.enqueueRetry(context)
                    }
                }
            }

            ChartWidgetContent(
                context = context,
                indexType = indexType,
                data = payload?.data,
                history = payload?.history ?: emptyList(),
                xLabels = payload?.xLabels ?: emptyList(),
                period = period,
                large = size.width.value >= 250f,
                refreshing = refreshing || payload == null,
            )
        }
    }
}

private suspend fun loadChartPayload(
    context: Context,
    indexType: FearIndexType,
    period: WidgetChartPeriod,
): ChartPayload = withContext(Dispatchers.IO) {
    val data = loadWidgetIndex(context, indexType)
    val entries = runCatching {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        when (indexType) {
            FearIndexType.MARKET -> entry.getFearIndexHistory().invoke(days = period.days)
            FearIndexType.KOSPI -> entry.getKospiFearIndexHistory().invoke(days = period.days)
            FearIndexType.CRYPTO -> entry.getCryptoFearIndexHistory().invoke(days = period.days)
        }
    }.getOrDefault(emptyList()).let { list ->
        // 기간 필터 + peak 보존 다운샘플 — 앱 차트와 동일한 코어 로직 재사용
        th1ngjin.fearindex.core.util.ChartDataFilter.filter(list, period.days, period.maxSamplePoints)
    }
    val history = entries.map { it.roundedScore }
    val dateFmt = java.time.format.DateTimeFormatter.ofPattern(if (period.useYearMonthAxis) "yy.M" else "M.d")
    val xLabels = if (entries.size >= 2) {
        listOf(0, entries.size / 2, entries.size - 1).map {
            entries[it].timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFmt)
        }
    } else emptyList()
    ChartPayload(data, history, xLabels)
}

class ChartFearWidget : ChartWidgetBase(FearIndexType.MARKET)
class KospiChartWidget : ChartWidgetBase(FearIndexType.KOSPI)
class CryptoChartWidget : ChartWidgetBase(FearIndexType.CRYPTO)

class ChartFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChartFearWidget()
}

class KospiChartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KospiChartWidget()
}

class CryptoChartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CryptoChartWidget()
}
