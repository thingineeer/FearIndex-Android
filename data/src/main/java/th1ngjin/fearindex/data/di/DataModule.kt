package th1ngjin.fearindex.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import th1ngjin.fearindex.data.datasource.CNNFearGreedApi
import th1ngjin.fearindex.data.datasource.CryptoFearIndexApi
import th1ngjin.fearindex.data.datasource.MarketIndexApi
import th1ngjin.fearindex.data.repository.CryptoFearIndexRepositoryImpl
import th1ngjin.fearindex.data.repository.FearIndexRepositoryImpl
import th1ngjin.fearindex.data.repository.MarketIndexRepositoryImpl
import th1ngjin.fearindex.data.repository.ReturnDataRepositoryImpl
import th1ngjin.fearindex.data.repository.StuckCounterRepositoryImpl
import th1ngjin.fearindex.data.repository.NotificationRepositoryImpl
import th1ngjin.fearindex.data.repository.VoteRepositoryImpl
import th1ngjin.fearindex.data.service.StuckStatusDebouncerImpl
import th1ngjin.fearindex.domain.repository.CryptoFearIndexRepository
import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.domain.repository.FearIndexRepository
import th1ngjin.fearindex.domain.repository.MarketIndexRepository
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.repository.ReturnDataRepository
import th1ngjin.fearindex.domain.repository.StuckCounterRepository
import th1ngjin.fearindex.domain.repository.VoteRepository
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import th1ngjin.fearindex.domain.service.StuckStatusDebouncer
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetMarketIndicesUseCase
import th1ngjin.fearindex.domain.usecase.GetVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.ObserveStuckCounterUseCase
import th1ngjin.fearindex.domain.usecase.ObserveVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.SubmitStuckStatusUseCase
import th1ngjin.fearindex.domain.usecase.SubmitVoteUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {
    @Binds
    @Singleton
    abstract fun bindFearIndexRepository(impl: FearIndexRepositoryImpl): FearIndexRepository

    @Binds
    @Singleton
    abstract fun bindCryptoFearIndexRepository(impl: CryptoFearIndexRepositoryImpl): CryptoFearIndexRepository

    @Binds
    @Singleton
    abstract fun bindStuckCounterRepository(impl: StuckCounterRepositoryImpl): StuckCounterRepository

    @Binds
    @Singleton
    abstract fun bindMarketIndexRepository(impl: MarketIndexRepositoryImpl): MarketIndexRepository

    @Binds
    @Singleton
    abstract fun bindVoteRepository(impl: VoteRepositoryImpl): VoteRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindReturnDataRepository(impl: ReturnDataRepositoryImpl): ReturnDataRepository

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: StuckCounterStorage): DeviceIdProvider

    @Binds
    @Singleton
    abstract fun bindStuckStatusDebouncer(impl: StuckStatusDebouncerImpl): StuckStatusDebouncer
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    @Named("cnn")
    fun provideCNNOkHttpClient(baseClient: OkHttpClient): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .header("Referer", "https://edition.cnn.com/markets/fear-and-greed")
                    .header("Origin", "https://edition.cnn.com")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideCNNApi(@Named("cnn") client: OkHttpClient): CNNFearGreedApi {
        return Retrofit.Builder()
            .baseUrl("https://production.dataviz.cnn.io/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CNNFearGreedApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMarketIndexApi(client: OkHttpClient): MarketIndexApi {
        return Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MarketIndexApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCryptoApi(client: OkHttpClient): CryptoFearIndexApi {
        return Retrofit.Builder()
            .baseUrl("https://api.alternative.me/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CryptoFearIndexApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGetFearIndexUseCase(repository: FearIndexRepository): GetFearIndexUseCase =
        GetFearIndexUseCase(repository)

    @Provides
    @Singleton
    fun provideGetFearIndexHistoryUseCase(repository: FearIndexRepository): GetFearIndexHistoryUseCase =
        GetFearIndexHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCryptoFearIndexUseCase(repository: CryptoFearIndexRepository): GetCryptoFearIndexUseCase =
        GetCryptoFearIndexUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCryptoFearIndexHistoryUseCase(repository: CryptoFearIndexRepository): GetCryptoFearIndexHistoryUseCase =
        GetCryptoFearIndexHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideGetMarketIndicesUseCase(repository: MarketIndexRepository): GetMarketIndicesUseCase =
        GetMarketIndicesUseCase(repository)

    // ============================================================
    // Firebase (Stuck Counter)
    // ============================================================

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-northeast3")

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideSubmitStuckStatusUseCase(repository: StuckCounterRepository): SubmitStuckStatusUseCase =
        SubmitStuckStatusUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveStuckCounterUseCase(repository: StuckCounterRepository): ObserveStuckCounterUseCase =
        ObserveStuckCounterUseCase(repository)

    // ============================================================
    // Firebase (Vote — Buy/Hold/Sell)
    // ============================================================

    @Provides
    @Singleton
    fun provideSubmitVoteUseCase(repository: VoteRepository): SubmitVoteUseCase =
        SubmitVoteUseCase(repository)

    @Provides
    @Singleton
    fun provideGetVoteResultUseCase(repository: VoteRepository): GetVoteResultUseCase =
        GetVoteResultUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveVoteResultUseCase(repository: VoteRepository): ObserveVoteResultUseCase =
        ObserveVoteResultUseCase(repository)
}
