package th1ngjin.fearindex.notification.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase

/**
 * Hilt EntryPoint -- FirebaseMessagingService에서 DI 컴포넌트 접근용.
 *
 * FirebaseMessagingService는 @AndroidEntryPoint를 직접 사용할 수 없으므로
 * EntryPointAccessors.fromApplication()으로 접근한다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MessagingEntryPoint {
    fun notificationRepository(): NotificationRepository
    fun deviceIdProvider(): DeviceIdProvider
    fun notificationHistoryUseCase(): NotificationHistoryUseCase
}
