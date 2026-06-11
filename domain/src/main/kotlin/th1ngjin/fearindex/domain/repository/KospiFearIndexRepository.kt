package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.KospiFearIndex

interface KospiFearIndexRepository {
    suspend fun fetchCurrent(forceRefresh: Boolean = false): KospiFearIndex
    suspend fun fetchHistory(days: Int, forceRefresh: Boolean = false): List<FearIndex>
}
