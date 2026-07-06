package th1ngjin.fearindex.presentation.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.core.remoteconfig.RemoteConfigManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun analyticsManager(): AnalyticsManager
    fun remoteConfigManager(): RemoteConfigManager
    fun purchaseManager(): PurchaseManager
}
