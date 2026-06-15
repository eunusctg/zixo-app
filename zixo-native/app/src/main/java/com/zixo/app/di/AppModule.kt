package com.zixo.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.data.local.room.ZixoDatabase
import com.zixo.app.data.local.room.ZixoMigrations
import com.zixo.app.data.local.room.dao.CallLogDao
import com.zixo.app.data.local.room.dao.ChatDao
import com.zixo.app.data.local.room.dao.ContactDao
import com.zixo.app.data.local.room.dao.MessageDao
import com.zixo.app.data.local.room.dao.StatusDao
import com.zixo.app.data.local.room.dao.UserDao
import com.zixo.app.data.remote.cloudflare.CloudflareApi
import com.zixo.app.data.remote.cloudflare.CloudflareApiService
import com.zixo.app.data.remote.firebase.FirestoreSyncWorker
import com.zixo.app.data.remote.webrtc.FirebaseSignalingClient
import com.zixo.app.data.remote.webrtc.PeerConnectionObserver
import com.zixo.app.data.remote.webrtc.WebRtcClient
import com.zixo.app.data.remote.webrtc.ZixoAudioManager
import com.zixo.app.data.sync.ConflictResolver
import com.zixo.app.domain.repository.AuthRepository
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.repository.SettingsRepository
import com.zixo.app.domain.repository.StatusRepository
import com.zixo.app.data.repository.AuthRepositoryImpl
import com.zixo.app.data.repository.CallRepositoryImpl
import com.zixo.app.data.repository.ChatRepositoryImpl
import com.zixo.app.data.repository.ContactRepositoryImpl
import com.zixo.app.data.repository.SettingsRepositoryImpl
import com.zixo.app.data.repository.StatusRepositoryImpl
import com.zixo.app.domain.usecase.EncryptMessageUseCase
import com.zixo.app.domain.usecase.GetContactsUseCase
import com.zixo.app.domain.usecase.InitiateCallUseCase
import com.zixo.app.domain.usecase.SendMessageUseCase
import com.zixo.app.domain.usecase.UpdateStatusUseCase
import com.zixo.app.domain.usecase.ValidatePasskeyUseCase
import com.zixo.app.ui.components.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt dependency injection module providing all application-scoped dependencies.
 *
 * Updated to include:
 * - All 6 Room DAOs (ChatDao, CallLogDao, MessageDao, ContactDao, StatusDao, UserDao)
 * - ZixoAudioManager singleton for WebRTC audio calibration
 * - PeerConnectionObserver singleton for WebRTC event handling
 * - NotificationHelper singleton for centralized notification management
 * - WorkManager instance for background sync scheduling
 * - FirestoreSyncWorker for sync orchestration
 * - ConflictResolver for server-wins timestamp resolution
 * - All 5 Use Cases (GetContacts, SendMessage, InitiateCall, UpdateStatus, ValidatePasskey)
 * - EncryptMessageUseCase for E2E encryption
 * - Proper Room migrations (no fallbackToDestructiveMigration)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val PREFERENCES_NAME = "zixo_preferences"
    private const val BASE_URL = "https://api.zixo.app/"

    // ════════════════════════════════════════════════════════
    // SharedPreferences (for legacy FCM service)
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: android.content.Context): android.content.SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)

    // ════════════════════════════════════════════════════════
    // DataStore
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        migrations = listOf<DataMigration<Preferences>>(),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) }
    )

    // ════════════════════════════════════════════════════════
    // Room Database — Proper Migrations, NO destructive fallback
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideZixoDatabase(
        @ApplicationContext context: Context
    ): ZixoDatabase = Room.databaseBuilder(
        context,
        ZixoDatabase::class.java,
        ZixoDatabase.DATABASE_NAME
    )
        .addMigrations(*ZixoMigrations.ALL_MIGRATIONS.toTypedArray())
        .build()

    @Provides fun provideChatDao(db: ZixoDatabase): ChatDao = db.chatDao()
    @Provides fun provideCallLogDao(db: ZixoDatabase): CallLogDao = db.callLogDao()
    @Provides fun provideMessageDao(db: ZixoDatabase): MessageDao = db.messageDao()
    @Provides fun provideContactDao(db: ZixoDatabase): ContactDao = db.contactDao()
    @Provides fun provideStatusDao(db: ZixoDatabase): StatusDao = db.statusDao()
    @Provides fun provideUserDao(db: ZixoDatabase): UserDao = db.userDao()

    // ════════════════════════════════════════════════════════
    // Networking
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideCloudflareApi(retrofit: Retrofit): CloudflareApi =
        retrofit.create(CloudflareApi::class.java)

    @Provides
    @Singleton
    fun provideCloudflareApiService(api: CloudflareApi): CloudflareApiService =
        CloudflareApiService(api)

    // ════════════════════════════════════════════════════════
    // WebRTC — Pure Peer-to-Peer Calling
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideWebRtcClient(@ApplicationContext context: Context): WebRtcClient =
        WebRtcClient(context)

    @Provides
    @Singleton
    fun provideFirebaseSignalingClient(firebaseDatabase: FirebaseDatabase): FirebaseSignalingClient =
        FirebaseSignalingClient(firebaseDatabase)

    @Provides
    @Singleton
    fun provideZixoAudioManager(@ApplicationContext context: Context): ZixoAudioManager =
        ZixoAudioManager(context)

    @Provides
    @Singleton
    fun providePeerConnectionObserver(): PeerConnectionObserver =
        PeerConnectionObserver()

    // ════════════════════════════════════════════════════════
    // WorkManager & Sync Engine
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideFirestoreSyncWorker(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): FirestoreSyncWorker = FirestoreSyncWorker(context, workManager)

    @Provides
    @Singleton
    fun provideConflictResolver(): ConflictResolver = ConflictResolver()

    // ════════════════════════════════════════════════════════
    // Notification Helper
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)

    // ════════════════════════════════════════════════════════
    // Domain Use Cases
    // ════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideEncryptMessageUseCase(
        contactRepository: ContactRepository
    ): EncryptMessageUseCase = EncryptMessageUseCase(contactRepository)

    @Provides
    @Singleton
    fun provideGetContactsUseCase(
        contactRepository: ContactRepository
    ): GetContactsUseCase = GetContactsUseCase(contactRepository)

    @Provides
    @Singleton
    fun provideSendMessageUseCase(
        chatRepository: ChatRepository,
        contactRepository: ContactRepository,
        encryptMessageUseCase: EncryptMessageUseCase
    ): SendMessageUseCase = SendMessageUseCase(chatRepository, contactRepository, encryptMessageUseCase)

    @Provides
    @Singleton
    fun provideInitiateCallUseCase(
        callRepository: CallRepository,
        contactRepository: ContactRepository
    ): InitiateCallUseCase = InitiateCallUseCase(callRepository, contactRepository)

    @Provides
    @Singleton
    fun provideUpdateStatusUseCase(
        statusRepository: StatusRepository,
        contactRepository: ContactRepository
    ): UpdateStatusUseCase = UpdateStatusUseCase(statusRepository)

    @Provides
    @Singleton
    fun provideValidatePasskeyUseCase(
        authRepository: AuthRepository
    ): ValidatePasskeyUseCase = ValidatePasskeyUseCase(authRepository)

    // ════════════════════════════════════════════════════════
    // Repository Bindings
    // ════════════════════════════════════════════════════════

    @Provides @Singleton
    fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository = impl

    @Provides @Singleton
    fun provideContactRepository(impl: ContactRepositoryImpl): ContactRepository = impl

    @Provides @Singleton
    fun provideChatRepository(impl: ChatRepositoryImpl): ChatRepository = impl

    @Provides @Singleton
    fun provideCallRepository(impl: CallRepositoryImpl): CallRepository = impl

    @Provides @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    @Provides @Singleton
    fun provideStatusRepository(impl: StatusRepositoryImpl): StatusRepository = impl
}
