package th1ngjin.fearindex.presentation.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import th1ngjin.fearindex.core.analytics.AnalyticsManager

/**
 * Hilt EntryPoint — `@Composable` 함수처럼 ViewModel 외부에서 AnalyticsManager에 접근할 때 사용.
 *
 * Compose Navigation의 NavHost는 ViewModel을 직접 보유하지 않으므로 EntryPointAccessors로
 * 싱글톤 인스턴스를 가져온다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun analyticsManager(): AnalyticsManager
}
