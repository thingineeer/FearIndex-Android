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
    val resId = eventTitleKeyMap[titleKey]
    return if (resId != null) stringResource(resId) else titleKey
}

private val eventTitleKeyMap: Map<String, Int> = mapOf(
    "insight.event.covid" to R.string.event_covid,
    "insight.event.tariff" to R.string.event_tariff,
    "insight.event.inflation" to R.string.event_inflation,
    "insight.event.delta" to R.string.event_delta,
    "insight.event.tradeWar" to R.string.event_trade_war,
    "insight.event.svb" to R.string.event_svb,
    "insight.event.reopening" to R.string.event_reopening,
    "insight.event.postElection" to R.string.event_post_election,
    "insight.event.2021bull" to R.string.event_2021bull,
    "insight.event.aiRally" to R.string.event_ai_rally,
    "insight.event.preCovid" to R.string.event_pre_covid,
    "insight.event.dotcom" to R.string.event_dotcom,
    "insight.event.iranWar2026" to R.string.event_iran_war_2026,
    "insight.crypto.event.covid" to R.string.crypto_event_covid,
    "insight.crypto.event.luna" to R.string.crypto_event_luna,
    "insight.crypto.event.ftx" to R.string.crypto_event_ftx,
    "insight.crypto.event.chinaBan" to R.string.crypto_event_china_ban,
    "insight.crypto.event.svb" to R.string.crypto_event_svb,
    "insight.crypto.event.sec" to R.string.crypto_event_sec,
    "insight.crypto.event.halving2024" to R.string.crypto_event_halving_2024,
    "insight.crypto.event.etfApproval" to R.string.crypto_event_etf_approval,
    "insight.crypto.event.defiSummer" to R.string.crypto_event_defi_summer,
    "insight.crypto.event.peak2021" to R.string.crypto_event_2021_peak,
    "insight.crypto.event.trumpPump" to R.string.crypto_event_trump_pump,
    "insight.crypto.event.peak2017" to R.string.crypto_event_2017_peak,
    "insight.crypto.event.iranWar2026" to R.string.crypto_event_iran_war_2026,
)
