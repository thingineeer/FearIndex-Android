package th1ngjin.fearindex.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import th1ngjin.fearindex.presentation.R

class EventLocalizerTest {
    @Test
    fun `maps legacy market event keys to localized string resources`() {
        assertEquals(R.string.event_covid, eventTitleResourceId("event_covid"))
        assertEquals(R.string.event_trade_war, eventTitleResourceId("event_trade_war"))
        assertEquals(R.string.event_iran_war_2026, eventTitleResourceId("event_iran_war_2026"))
    }

    @Test
    fun `maps legacy crypto event keys to localized string resources`() {
        assertEquals(R.string.crypto_event_covid, eventTitleResourceId("crypto_event_covid"))
        assertEquals(R.string.crypto_event_china_ban, eventTitleResourceId("crypto_event_china_ban"))
        assertEquals(R.string.crypto_event_iran_war_2026, eventTitleResourceId("crypto_event_iran_war_2026"))
    }

    @Test
    fun `maps kospi event keys to localized string resources`() {
        assertEquals(R.string.kospi_event_yen_carry_2024, eventTitleResourceId("insight.kospi.event.yenCarry2024"))
        assertEquals(R.string.kospi_event_tariff_2025, eventTitleResourceId("insight.kospi.event.tariff2025"))
        assertEquals(R.string.kospi_event_twelve_day_war_2025, eventTitleResourceId("insight.kospi.event.twelveDayWar2025"))
    }

    @Test
    fun `returns null for unknown event keys`() {
        assertNull(eventTitleResourceId("unknown_event"))
    }
}
