package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import th1ngjin.fearindex.domain.entity.FearIndexType

/** 차트 위젯 — Global 지수 30일 추이 + 현재 점수 (4×2 기본, 2×2 축소 지원). */
class ChartFearWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 기간·지수는 위젯 인스턴스별 state (세그먼트/이름 탭이 저장)
        val state = runCatching {
            androidx.glance.appwidget.state.getAppWidgetState(
                context,
                androidx.glance.state.PreferencesGlanceStateDefinition,
                id,
            )
        }.getOrNull()
        val period = WidgetChartPeriod.fromName(state?.get(SetChartPeriodAction.CHART_PERIOD_KEY))
        val refreshing = state?.get(RefreshWidgetsAction.REFRESHING_KEY) ?: false
        val indexType = CycleChartIndexAction.fromName(state?.get(CycleChartIndexAction.CHART_INDEX_KEY))
        val data = loadWidgetIndex(context, indexType)
        val historyEntries = withContext(Dispatchers.IO) {
            runCatching {
                val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                when (indexType) {
                    FearIndexType.MARKET -> entry.getFearIndexHistory().invoke(days = period.days)
                    FearIndexType.KOSPI -> entry.getKospiFearIndexHistory().invoke(days = period.days)
                    FearIndexType.CRYPTO -> entry.getCryptoFearIndexHistory().invoke(days = period.days)
                }
            }.getOrDefault(emptyList())
        }.let { entries ->
            // 기간 필터 + peak 보존 다운샘플 — 앱 차트와 동일한 코어 로직 재사용
            th1ngjin.fearindex.core.util.ChartDataFilter.filter(entries, period.days, period.maxSamplePoints)
        }
        val history = historyEntries.map { it.roundedScore }
        val dateFmt = java.time.format.DateTimeFormatter.ofPattern(if (period.useYearMonthAxis) "yy.M" else "M.d")
        val xLabels = if (historyEntries.size >= 2) {
            listOf(0, historyEntries.size / 2, historyEntries.size - 1).map {
                historyEntries[it].timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFmt)
            }
        } else emptyList()
        if (data == null || history.size < 2) FearWidgetUpdateWorker.enqueueRetry(context)
        provideContent {
            val size = LocalSize.current
            ChartWidgetContent(context, indexType, data, history, xLabels, period, large = size.width.value >= 250f, refreshing = refreshing)
        }
    }
}

class ChartFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChartFearWidget()
}
