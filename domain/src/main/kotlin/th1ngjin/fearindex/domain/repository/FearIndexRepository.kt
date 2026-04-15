package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.FearIndex

interface FearIndexRepository {
    suspend fun fetchCurrent(forceRefresh: Boolean = false): FearIndex
    suspend fun fetchHistory(days: Int, forceRefresh: Boolean = false): List<FearIndex>
}

interface CryptoFearIndexRepository {
    suspend fun fetchCurrent(forceRefresh: Boolean = false): FearIndex
    suspend fun fetchHistory(days: Int, forceRefresh: Boolean = false): List<FearIndex>
}
