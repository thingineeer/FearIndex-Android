package th1ngjin.fearindex.presentation.common

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizedInsightStringsTest {
    @Test
    fun `localized insight strings are not left as English defaults`() {
        val resDir = listOf(
            File("presentation/src/main/res"),
            File("src/main/res"),
        ).first { it.exists() }
        val defaultStrings = stringsFrom(resDir.resolve("values/strings.xml"))
        val failures = resDir
            .listFiles { file -> file.isDirectory && file.name.startsWith("values-") }
            .orEmpty()
            .flatMap { localeDir ->
                val localeStrings = stringsFrom(localeDir.resolve("strings.xml"))
                insightKeys.mapNotNull { key ->
                    val defaultValue = defaultStrings[key]
                    val localeValue = localeStrings[key]
                    if (defaultValue != null && localeValue == defaultValue) {
                        "${localeDir.name}:$key"
                    } else {
                        null
                    }
                }
            }

        assertTrue(
            "English defaults remain in localized insight strings: ${failures.joinToString()}",
            failures.isEmpty(),
        )
    }

    private fun stringsFrom(file: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as Element
                put(element.getAttribute("name"), element.textContent)
            }
        }
    }

    private companion object {
        val insightKeys = listOf(
            "insight_similar_events_title",
            "insight_similar_events_score_label",
            "insight_similar_events_pinned_title",
            "insight_similar_events_empty",
            "insight_similar_events_one_year_return",
            "insight_similar_events_ongoing",
            "insight_similar_events_sim_very",
            "insight_similar_events_sim_close",
            "insight_similar_events_sim_moderate",
            "insight_similar_events_sim_far",
            "insight_similar_events_aggregate_title",
            "insight_similar_events_aggregate_summary",
            "insight_similar_events_disclaimer",
            "insight_current_score_title",
            "insight_current_score_avg_return",
            "insight_current_score_max_drawdown",
            "insight_current_score_best_return",
            "insight_current_score_sample_count",
            "insight_event_score",
        )
    }
}
