package th1ngjin.fearindex.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient

/**
 * 아크 게이지 비트맵 렌더러 — Glance 는 커스텀 드로잉이 없어 Bitmap 으로 그려 Image 로 꽂는다.
 * iOS 홈 위젯과 동일: 270° 아크(좌하단 시작), 빨강→주황→노랑→초록 그라데이션, 라운드 캡,
 * 미채움 구간은 어두운 트랙.
 */
object WidgetGaugeRenderer {

    private val trackColor = Color.parseColor("#3A3A3C")
    private val gradientColors = intArrayOf(
        Color.parseColor("#E53935"), // 빨강 (0, 극단적 공포)
        Color.parseColor("#F57C00"), // 주황
        Color.parseColor("#FDD835"), // 노랑
        Color.parseColor("#43A047"), // 초록 (270°, 극단적 탐욕)
    )

    /** [sizePx] 정사각 비트맵에 score 게이지를 그린다. */
    fun render(score: Int, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * 0.09f
        val inset = stroke / 2f + sizePx * 0.02f
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            // BUTT: 라운드 캡은 아크 끝에 동그란 혹('o')처럼 보인다 — 2026-08-27 사용자 요청으로 제거
            strokeCap = Paint.Cap.BUTT
        }

        // 트랙 (전체 270°)
        paint.color = trackColor
        canvas.drawArc(rect, WidgetGaugeSpec.START_ANGLE_DEG, WidgetGaugeSpec.TOTAL_SWEEP_DEG, false, paint)

        // 진행 아크 — 시작각에 맞춰 회전시킨 SweepGradient 를 270° 범위에 매핑
        val sweep = WidgetGaugeSpec.sweepAngle(score)
        if (sweep > 0f) {
            val positions = floatArrayOf(0f, 0.33f, 0.66f, WidgetGaugeSpec.TOTAL_SWEEP_DEG / 360f)
            val shader = SweepGradient(rect.centerX(), rect.centerY(), gradientColors, positions)
            val matrix = Matrix().apply { postRotate(WidgetGaugeSpec.START_ANGLE_DEG, rect.centerX(), rect.centerY()) }
            shader.setLocalMatrix(matrix)
            paint.shader = shader
            canvas.drawArc(rect, WidgetGaugeSpec.START_ANGLE_DEG, sweep, false, paint)
            paint.shader = null
        }
        return bitmap
    }
}
