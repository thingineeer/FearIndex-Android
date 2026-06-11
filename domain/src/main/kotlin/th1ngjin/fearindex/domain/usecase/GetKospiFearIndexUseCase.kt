package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.repository.KospiFearIndexRepository

class GetKospiFearIndexUseCase(private val repository: KospiFearIndexRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): KospiFearIndex =
        repository.fetchCurrent(forceRefresh)
}
