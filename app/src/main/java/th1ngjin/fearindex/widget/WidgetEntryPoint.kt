package th1ngjin.fearindex.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetKospiFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetKospiFearIndexUseCase

/**
 * Glance 위젯 클래스는 Hilt @Inject 를 쓸 수 없으므로 applicationContext 로부터
 * EntryPoint 를 열어 UseCase 들을 얻는다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getFearIndex(): GetFearIndexUseCase
    fun getKospiFearIndex(): GetKospiFearIndexUseCase
    fun getCryptoFearIndex(): GetCryptoFearIndexUseCase
    fun getFearIndexHistory(): GetFearIndexHistoryUseCase
    fun getKospiFearIndexHistory(): GetKospiFearIndexHistoryUseCase
    fun getCryptoFearIndexHistory(): GetCryptoFearIndexHistoryUseCase
}
