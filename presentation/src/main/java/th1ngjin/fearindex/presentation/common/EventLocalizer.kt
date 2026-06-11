package th1ngjin.fearindex.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import th1ngjin.fearindex.presentation.R

/**
 * Firestore titleKey → Android strings.xml 매핑.
 * 서버가 보내는 Localizable.strings 키를 Android 리소스 ID로 변환.
 */
@Composable
fun localizedEventTitle(titleKey: String): String {
    val resId = eventTitleResourceId(titleKey)
    return if (resId != null) stringResource(resId) else titleKey
}

internal fun eventTitleResourceId(titleKey: String): Int? = eventTitleKeyMap[titleKey]

private val eventTitleKeyMap: Map<String, Int> = mapOf(
    "insight.event.covid" to R.string.event_covid,
    "event_covid" to R.string.event_covid,
    "insight.event.tariff" to R.string.event_tariff,
    "event_tariff" to R.string.event_tariff,
    "insight.event.inflation" to R.string.event_inflation,
    "event_inflation" to R.string.event_inflation,
    "insight.event.delta" to R.string.event_delta,
    "event_delta" to R.string.event_delta,
    "insight.event.tradeWar" to R.string.event_trade_war,
    "event_trade_war" to R.string.event_trade_war,
    "insight.event.svb" to R.string.event_svb,
    "event_svb" to R.string.event_svb,
    "insight.event.reopening" to R.string.event_reopening,
    "event_reopening" to R.string.event_reopening,
    "insight.event.postElection" to R.string.event_post_election,
    "event_post_election" to R.string.event_post_election,
    "insight.event.2021bull" to R.string.event_2021bull,
    "event_2021bull" to R.string.event_2021bull,
    "insight.event.aiRally" to R.string.event_ai_rally,
    "event_ai_rally" to R.string.event_ai_rally,
    "insight.event.preCovid" to R.string.event_pre_covid,
    "event_pre_covid" to R.string.event_pre_covid,
    "insight.event.dotcom" to R.string.event_dotcom,
    "event_dotcom" to R.string.event_dotcom,
    "insight.event.iranWar2026" to R.string.event_iran_war_2026,
    "event_iran_war_2026" to R.string.event_iran_war_2026,
    "insight.event.twelveDayWar2025" to R.string.event_twelve_day_war_2025,
    "event_twelve_day_war_2025" to R.string.event_twelve_day_war_2025,
    "insight.crypto.event.covid" to R.string.crypto_event_covid,
    "crypto_event_covid" to R.string.crypto_event_covid,
    "insight.crypto.event.luna" to R.string.crypto_event_luna,
    "crypto_event_luna" to R.string.crypto_event_luna,
    "insight.crypto.event.ftx" to R.string.crypto_event_ftx,
    "crypto_event_ftx" to R.string.crypto_event_ftx,
    "insight.crypto.event.chinaBan" to R.string.crypto_event_china_ban,
    "crypto_event_china_ban" to R.string.crypto_event_china_ban,
    "insight.crypto.event.svb" to R.string.crypto_event_svb,
    "crypto_event_svb" to R.string.crypto_event_svb,
    "insight.crypto.event.sec" to R.string.crypto_event_sec,
    "crypto_event_sec" to R.string.crypto_event_sec,
    "insight.crypto.event.halving2024" to R.string.crypto_event_halving_2024,
    "crypto_event_halving_2024" to R.string.crypto_event_halving_2024,
    "insight.crypto.event.etfApproval" to R.string.crypto_event_etf_approval,
    "crypto_event_etf_approval" to R.string.crypto_event_etf_approval,
    "insight.crypto.event.defiSummer" to R.string.crypto_event_defi_summer,
    "crypto_event_defi_summer" to R.string.crypto_event_defi_summer,
    "insight.crypto.event.peak2021" to R.string.crypto_event_2021_peak,
    "crypto_event_2021_peak" to R.string.crypto_event_2021_peak,
    "insight.crypto.event.trumpPump" to R.string.crypto_event_trump_pump,
    "crypto_event_trump_pump" to R.string.crypto_event_trump_pump,
    "insight.crypto.event.peak2017" to R.string.crypto_event_2017_peak,
    "crypto_event_2017_peak" to R.string.crypto_event_2017_peak,
    "insight.crypto.event.iranWar2026" to R.string.crypto_event_iran_war_2026,
    "crypto_event_iran_war_2026" to R.string.crypto_event_iran_war_2026,
    "insight.crypto.event.twelveDayWar2025" to R.string.crypto_event_twelve_day_war_2025,
    "crypto_event_twelve_day_war_2025" to R.string.crypto_event_twelve_day_war_2025,
    "insight.kospi.event.yenCarry2024" to R.string.kospi_event_yen_carry_2024,
    "kospi_event_yen_carry_2024" to R.string.kospi_event_yen_carry_2024,
    "insight.kospi.event.tariff2025" to R.string.kospi_event_tariff_2025,
    "kospi_event_tariff_2025" to R.string.kospi_event_tariff_2025,
    "insight.kospi.event.hamasIsrael2023" to R.string.kospi_event_hamas_israel_2023,
    "kospi_event_hamas_israel_2023" to R.string.kospi_event_hamas_israel_2023,
    "insight.kospi.event.iranWar2026" to R.string.kospi_event_iran_war_2026,
    "kospi_event_iran_war_2026" to R.string.kospi_event_iran_war_2026,
    "insight.kospi.event.martialLawAftermath" to R.string.kospi_event_martial_law_aftermath,
    "kospi_event_martial_law_aftermath" to R.string.kospi_event_martial_law_aftermath,
    "insight.kospi.event.legolandPf" to R.string.kospi_event_legoland_pf,
    "kospi_event_legoland_pf" to R.string.kospi_event_legoland_pf,
    "insight.kospi.event.twelveDayWar2025" to R.string.kospi_event_twelve_day_war_2025,
    "kospi_event_twelve_day_war_2025" to R.string.kospi_event_twelve_day_war_2025,
)
