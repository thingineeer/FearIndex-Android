package th1ngjin.fearindex.presentation.feature.chart

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.util.ScoreExplorerStats

/**
 * 점수별 과거 수익률 슬라이더의 순수 선택 상태 (v1.9.4). iOS `FearIndexInteractor` 의
 * `explorerSelections` / `explorerScore` / `explorerAnchorScore` / `moveExplorer` / `resetExplorer` 1:1.
 *
 * - [userSelections] 는 자산별로 사용자가 **직접 고른** 점수. 없으면(= 건드리지 않음/리셋) 그 자산의
 *   현재 점수를 따라간다 → 데이터 로드·세그먼트 전환 시 자동으로 현재 점수로 리셋된다.
 * - 표시 점수는 항상 현재 테이블 범위로 클램프한다 — 번들 fallback 에서 고른 뒤 더 좁은 Firestore
 *   테이블이 도착해도 범위 밖에 남지 않는다.
 * - 불변 값 객체: 모든 조작은 새 인스턴스를 반환한다 (ViewModel 이 보관, UI 는 파생값만 읽음).
 */
data class ScoreExplorerSelection(
    val indexType: FearIndexType = FearIndexType.MARKET,
    /** 슬라이더 범위 (표본 n>0 최소..최대). null = 표시할 데이터 없음. */
    val range: IntRange? = null,
    /** 그 자산의 현재 반올림 점수. null = 아직 미로드. */
    val currentScore: Int? = null,
    val userSelections: Map<FearIndexType, Int> = emptyMap(),
) {

    /** "현재 점수로" 기준값 = 현재 점수(미로드 시 범위 하한, 범위도 없으면 50)를 범위로 클램프. */
    val anchorScore: Int
        get() {
            val range = range ?: return currentScore ?: DEFAULT_SCORE
            return ScoreExplorerStats.clamp(currentScore ?: range.first, range)
        }

    /** 슬라이더가 가리키는 점수. 사용자가 움직이지 않았으면 [anchorScore]. */
    val selectedScore: Int
        get() {
            val selected = userSelections[indexType] ?: return anchorScore
            return range?.let { ScoreExplorerStats.clamp(selected, it) } ?: selected
        }

    /** 슬라이더가 현재 점수 위치인가 (리셋 버튼 비활성 조건). 표시값 기준이라 클램프 결과와 항상 일치. */
    val isAtCurrent: Boolean
        get() = selectedScore == anchorScore

    /**
     * 자산/현재 점수/범위 갱신. 자산이 바뀌면 그 자산의 선택값(없으면 현재 점수)이 표시된다.
     * 다른 자산의 선택값은 보존한다 (iOS: 이동한 자산은 선택값 유지, 건드리지 않은 자산은 현재 점수).
     */
    fun bind(indexType: FearIndexType, currentScore: Int?, range: IntRange?): ScoreExplorerSelection =
        copy(indexType = indexType, currentScore = currentScore, range = range)

    /** 슬라이더 이동. 범위 밖은 클램프. 현재 점수 위치로 돌아오면 리셋 상태로 본다. */
    fun move(score: Int): ScoreExplorerSelection {
        val clamped = range?.let { ScoreExplorerStats.clamp(score, it) } ?: score
        val next = userSelections.toMutableMap()
        if (clamped == anchorScore) next.remove(indexType) else next[indexType] = clamped
        return copy(userSelections = next)
    }

    /** "현재 점수로" 리셋 — 이후 현재 점수 갱신을 다시 따라간다. */
    fun reset(): ScoreExplorerSelection = copy(userSelections = userSelections - indexType)

    companion object {
        /** 현재 점수도 범위도 없을 때의 fallback (iOS 동일). */
        const val DEFAULT_SCORE = 50
    }
}
