package th1ngjin.fearindex.presentation.feature.history

import th1ngjin.fearindex.domain.entity.NotificationRecord
import java.time.LocalDate
import java.time.ZoneId

/**
 * 알림 내역 리스트의 순수 레이아웃 계산 — 배너 삽입 위치 / 날짜 섹션 그룹핑 / 날짜 헤더 라벨.
 * iOS `NotificationHistoryLayout` 1:1. UI 프레임워크 무관(JVM 유닛 테스트 대상).
 */
object NotificationHistoryLayout {

    /** 무료 유저: 첫 배너는 3번째 row 뒤(첫 화면에 1개 보장), 이후 7개마다 */
    const val FIRST_BANNER_AFTER = 3
    const val BANNER_INTERVAL = 7

    /** 리스트 row — 레코드 또는 배너 슬롯 */
    sealed interface Row {
        val id: String

        data class Record(val record: NotificationRecord) : Row {
            override val id: String get() = record.id
        }

        /** [position] = 직전 레코드의 1-based 순번 (배너 슬롯 id 안정화용) */
        data class Banner(val position: Int) : Row {
            override val id: String get() = "history-banner-$position"
        }
    }

    /** 날짜(자정 기준) 섹션 — 배너는 직전 레코드의 날짜 섹션에 붙는다 */
    data class DaySection(val day: LocalDate, val rows: List<Row>)

    /** 날짜 헤더 라벨 (localize 는 View 가 담당) */
    sealed interface DayLabel {
        data object Today : DayLabel
        data object Yesterday : DayLabel
        data class Date(val day: LocalDate) : DayLabel
    }

    /** [rowCount] 개 레코드에서 배너가 들어가는 위치(직전 레코드의 1-based 순번): 3, 10, 17, … */
    fun bannerPositions(rowCount: Int): List<Int> {
        if (rowCount < FIRST_BANNER_AFTER) return emptyList()
        return (FIRST_BANNER_AFTER..rowCount step BANNER_INTERVAL).toList()
    }

    /** 레코드(최신순) → 배너 interleave 된 row 목록. [includeAds] false 면 레코드만. */
    fun rows(records: List<NotificationRecord>, includeAds: Boolean): List<Row> {
        if (!includeAds) return records.map { Row.Record(it) }
        val positions = bannerPositions(records.size).toSet()
        return buildList {
            records.forEachIndexed { index, record ->
                add(Row.Record(record))
                if (index + 1 in positions) add(Row.Banner(position = index + 1))
            }
        }
    }

    /** row 목록 → 날짜별 섹션 (입력 순서 유지, 배너는 직전 섹션에 귀속) */
    fun sections(rows: List<Row>, zone: ZoneId = ZoneId.systemDefault()): List<DaySection> {
        val sections = mutableListOf<DaySection>()
        for (row in rows) {
            val day = (row as? Row.Record)?.record?.receivedAt?.atZone(zone)?.toLocalDate()
            val last = sections.lastOrNull()
            if (day != null && last?.day != day) {
                sections += DaySection(day, listOf(row))
            } else if (last != null) {
                sections[sections.lastIndex] = last.copy(rows = last.rows + row)
            }
        }
        return sections
    }

    fun dayLabel(day: LocalDate, today: LocalDate): DayLabel = when (day) {
        today -> DayLabel.Today
        today.minusDays(1) -> DayLabel.Yesterday
        else -> DayLabel.Date(day)
    }
}
