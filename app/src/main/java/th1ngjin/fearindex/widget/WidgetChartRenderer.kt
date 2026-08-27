package th1ngjin.fearindex.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * 위젯용 30일 라인차트 비트맵.
 * - y축: 데이터 범위 기준 상/중/하 3눈금 + 은은한 그리드
 * - x축: 시작/중간/끝 날짜 라벨(M.d)
 * - 마지막 점 강조
 */
object WidgetChartRenderer {

    private val gridColor = Color.parseColor("#33FFFFFF")
    private val labelColor = Color.parseColor("#8A8F98")

    fun render(
        scores: List<Int>,
        xLabels: List<String>, // [시작, 중간, 끝] M.d
        widthPx: Int,
        heightPx: Int,
        colorArgb: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        if (scores.size < 2) return bitmap
        val canvas = Canvas(bitmap)
        val labelSize = heightPx * 0.085f
        val stroke = heightPx * 0.035f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = labelColor; textSize = labelSize }
        val yLabelWidth = labelPaint.measureText("100") + stroke * 2
        val left = yLabelWidth
        val right = widthPx - stroke * 2
        val top = stroke * 2
        val bottom = heightPx - labelSize - stroke * 3 // x축 라벨 공간

        val min = (scores.min() - 3).coerceAtLeast(0)
        val max = (scores.max() + 3).coerceAtMost(100)
        val range = (max - min).coerceAtLeast(1)
        fun x(i: Int) = left + (right - left) * i / (scores.size - 1)
        fun y(v: Int) = top + (bottom - top) * (1f - (v - min).toFloat() / range)

        // y 그리드 + 라벨 (상/중/하)
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gridColor; strokeWidth = 1.5f }
        val mid = (min + max) / 2
        for (v in listOf(max, mid, min)) {
            val yy = y(v)
            canvas.drawLine(left, yy, right, yy, gridPaint)
            canvas.drawText(v.toString(), stroke, yy + labelSize * 0.35f, labelPaint)
        }

        // x 라벨 (시작/중간/끝)
        if (xLabels.size >= 3) {
            val yText = heightPx - stroke
            canvas.drawText(xLabels[0], left, yText, labelPaint)
            val midLabel = xLabels[1]
            canvas.drawText(midLabel, (left + right) / 2 - labelPaint.measureText(midLabel) / 2, yText, labelPaint)
            val endLabel = xLabels[2]
            canvas.drawText(endLabel, right - labelPaint.measureText(endLabel), yText, labelPaint)
        }

        // 라인
        val path = Path().apply {
            moveTo(x(0), y(scores[0]))
            for (i in 1 until scores.size) lineTo(x(i), y(scores[i]))
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = colorArgb
        }
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x(scores.size - 1), y(scores.last()), stroke * 1.7f, paint)
        return bitmap
    }
}
