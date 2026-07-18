package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.FearRSI
import th1ngjin.fearindex.domain.repository.AssetPriceClosesRepository

/**
 * 자산 가격 종가 → RSI(공포지수 보조 근거) — iOS `FetchAssetRSIUseCase` 대칭.
 * 종가 부족/미지원이면 null(카드 숨김).
 */
class GetAssetRSIUseCase(
    private val repository: AssetPriceClosesRepository,
    private val calculator: RSICalculator = RSICalculator(),
) {
    suspend operator fun invoke(type: FearIndexType): FearRSI? {
        val series = repository.dailyCloses(type)
        return calculator.calculate(closes = series.closes)
            ?.copy(sourceMetadata = series.sourceMetadata)
    }
}
