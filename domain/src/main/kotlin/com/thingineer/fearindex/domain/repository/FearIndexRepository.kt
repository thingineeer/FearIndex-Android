package com.thingineer.fearindex.domain.repository

import com.thingineer.fearindex.domain.entity.FearIndex

interface FearIndexRepository {
    suspend fun fetchCurrent(forceRefresh: Boolean = false): FearIndex
    suspend fun fetchHistory(days: Int, forceRefresh: Boolean = false): List<FearIndex>
}

interface CryptoFearIndexRepository {
    suspend fun fetchCurrent(forceRefresh: Boolean = false): FearIndex
    suspend fun fetchHistory(days: Int, forceRefresh: Boolean = false): List<FearIndex>
}
