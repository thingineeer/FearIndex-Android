package th1ngjin.fearindex.presentation.common

/**
 * 탐욕 구간 카피 프레임 판정 (2026-08-27 회장님 지시, 전 플랫폼 통일).
 *
 * - 공포 = 매수 기회 가능 → 기존 "샀다면/매수" 프레임 유지
 * - 탐욕 = 시장 과열, 추가 매수 비권장 → "그때 이후 변동" 등 과열·주의 프레임
 *
 * 임계값 70 은 알림 상한 기본값(upperThreshold 70)과 같다 — 탐욕 알림(예: 암호화폐 71)을 받은 사용자가
 * 앱에서 같은 점수를 보면 매수 프레임이 아닌 과열 프레임을 만나야 한다. (점수 탐색기 통계 라벨도 이 기준으로 통일.)
 */
object GreedFrame {
    const val THRESHOLD = 70

    fun isGreed(score: Int): Boolean = score >= THRESHOLD
}
