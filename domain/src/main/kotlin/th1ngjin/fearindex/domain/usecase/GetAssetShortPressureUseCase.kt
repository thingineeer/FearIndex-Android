package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.ShortPressure
import th1ngjin.fearindex.domain.repository.AssetShortPressureRepository

/**
 * 자산 공매도 비중 시계열 → 공매도/숏커버링 신호 — iOS `FetchAssetShortPressureUseCase` 대칭.
 * 데이터 부족/미지원이면 null(카드 숨김).
 */
class GetAssetShortPressureUseCase(
    private val repository: AssetShortPressureRepository,
    private val calculator: ShortPressureCalculator = ShortPressureCalculator(),
) {
    suspend operator fun invoke(type: FearIndexType): ShortPressure? =
        calculator.calculate(dailyRatios = repository.dailyShortRatios(type))
}
