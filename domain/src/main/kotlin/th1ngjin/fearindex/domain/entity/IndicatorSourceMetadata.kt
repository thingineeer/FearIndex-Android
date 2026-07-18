package th1ngjin.fearindex.domain.entity

/**
 * RSI/공매도 지표의 데이터 출처 메타데이터 — iOS `IndicatorSourceMetadata` 대칭.
 *
 * sourceName/basisLabel/methodology는 서버(official endpoint) 또는 클라 하드코딩의
 * locale-neutral 영문 고정 문자열 — strings.xml로 번역하지 않는다 (iOS 규칙 동일).
 */
data class IndicatorSourceMetadata(
    val sourceName: String,
    val basisLabel: String,
    val asOf: String?,
    val isOfficial: Boolean,
    val methodology: String,
) {
    /** 카드 출처 라벨 — iOS sourceText: "source · basis · asOf" (빈 값 제외). */
    val cardLabel: String
        get() = listOfNotNull(
            sourceName.takeIf { it.isNotBlank() },
            basisLabel.takeIf { it.isNotBlank() },
            asOf?.takeIf { it.isNotBlank() },
        ).joinToString(SEPARATOR)

    /** info sheet 출처 섹션 본문 — iOS infoRow body: "source · asOf · methodology" (빈 값 제외). */
    val sheetBody: String
        get() = listOfNotNull(
            sourceName.takeIf { it.isNotBlank() },
            asOf?.takeIf { it.isNotBlank() },
            methodology.takeIf { it.isNotBlank() },
        ).joinToString(SEPARATOR)

    private companion object {
        const val SEPARATOR = " · "
    }
}
