package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FearIndexTest {

    @Test
    fun `roundedScore - 소수점 반올림`() {
        val index = createFearIndex(score = 45.7)
        assertEquals(46, index.roundedScore)
    }

    @Test
    fun `roundedScore - 정수 스코어`() {
        val index = createFearIndex(score = 50.0)
        assertEquals(50, index.roundedScore)
    }

    @Test
    fun `Rating from - 0~24는 EXTREME_FEAR`() {
        assertEquals(FearIndex.Rating.EXTREME_FEAR, FearIndex.Rating.from(0.0))
        assertEquals(FearIndex.Rating.EXTREME_FEAR, FearIndex.Rating.from(12.0))
        assertEquals(FearIndex.Rating.EXTREME_FEAR, FearIndex.Rating.from(24.9))
    }

    @Test
    fun `Rating from - 25~44는 FEAR`() {
        assertEquals(FearIndex.Rating.FEAR, FearIndex.Rating.from(25.0))
        assertEquals(FearIndex.Rating.FEAR, FearIndex.Rating.from(35.0))
        assertEquals(FearIndex.Rating.FEAR, FearIndex.Rating.from(44.9))
    }

    @Test
    fun `Rating from - 45~54는 NEUTRAL`() {
        assertEquals(FearIndex.Rating.NEUTRAL, FearIndex.Rating.from(45.0))
        assertEquals(FearIndex.Rating.NEUTRAL, FearIndex.Rating.from(50.0))
        assertEquals(FearIndex.Rating.NEUTRAL, FearIndex.Rating.from(54.999))
    }

    @Test
    fun `Rating from - 55~74는 GREED`() {
        assertEquals(FearIndex.Rating.GREED, FearIndex.Rating.from(55.0))
        assertEquals(FearIndex.Rating.GREED, FearIndex.Rating.from(60.0))
        assertEquals(FearIndex.Rating.GREED, FearIndex.Rating.from(74.999))
    }

    @Test
    fun `Rating from - 76~100은 EXTREME_GREED`() {
        assertEquals(FearIndex.Rating.EXTREME_GREED, FearIndex.Rating.from(75.1))
        assertEquals(FearIndex.Rating.EXTREME_GREED, FearIndex.Rating.from(90.0))
        assertEquals(FearIndex.Rating.EXTREME_GREED, FearIndex.Rating.from(100.0))
    }

    @Test
    fun `Rating from - 경계값 정확히 테스트`() {
        // 25 미만 → EXTREME_FEAR, 25 이상 → FEAR
        assertEquals(FearIndex.Rating.EXTREME_FEAR, FearIndex.Rating.from(24.999))
        assertEquals(FearIndex.Rating.FEAR, FearIndex.Rating.from(25.0))

        // 45 미만 → FEAR, 45 이상 → NEUTRAL
        assertEquals(FearIndex.Rating.FEAR, FearIndex.Rating.from(44.999))
        assertEquals(FearIndex.Rating.NEUTRAL, FearIndex.Rating.from(45.0))

        // 55 미만 → NEUTRAL, 55.0 = 경계값 윗 밴드 → GREED (iOS/서버 통일, 2026-08-27)
        assertEquals(FearIndex.Rating.NEUTRAL, FearIndex.Rating.from(54.999))
        assertEquals(FearIndex.Rating.GREED, FearIndex.Rating.from(55.0))

        // 75 미만 → GREED, 75.0 → EXTREME_GREED
        assertEquals(FearIndex.Rating.GREED, FearIndex.Rating.from(74.999))
        assertEquals(FearIndex.Rating.EXTREME_GREED, FearIndex.Rating.from(75.0))
    }

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.now(),
    )
}
