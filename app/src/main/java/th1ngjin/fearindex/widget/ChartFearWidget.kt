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
        val data = loadWidgetIndex(context, FearIndexType.MARKET)
        val historyEntries = withContext(Dispatchers.IO) {
            runCatching {
                EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                    .getFearIndexHistory()
                    .invoke(days = 30)
            }.getOrDefault(emptyList())
        }.let { entries ->
            // 데이터소스가 days를 무시하고 전체 캐시(1년)를 반환할 수 있어 최근 30일로 잘라낸다.
            val cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(30))
            val recent = entries.filter { !it.timestamp.isBefore(cutoff) }
            if (recent.size >= 2) recent else entries
        }
        val history = historyEntries.map { it.roundedScore }
        val dateFmt = java.time.format.DateTimeFormatter.ofPattern("M.d")
        val xLabels = if (historyEntries.size >= 2) {
            listOf(0, historyEntries.size / 2, historyEntries.size - 1).map {
                historyEntries[it].timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFmt)
            }
        } else emptyList()
        if (data == null || history.size < 2) FearWidgetUpdateWorker.enqueueRetry(context)
        provideContent {
            val size = LocalSize.current
            ChartWidgetContent(context, FearIndexType.MARKET, data, history, xLabels, large = size.width.value >= 250f)
        }
    }
}

class ChartFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChartFearWidget()
}
