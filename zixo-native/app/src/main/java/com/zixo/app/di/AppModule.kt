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
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zixo.app.data.local.room.ZixoDatabase
import com.zixo.app.data.local.room.dao.CallLogDao
import com.zixo.app.data.local.room.dao.ChatDao
import com.zixo.app.data.remote.cloudflare.CloudflareApiService
import com.zixo.app.domain.repository.CallRepository
import com.zixo.app.domain.repository.ChatRepository
import com.zixo.app.domain.repository.ContactRepository
import com.zixo.app.domain.repository.SettingsRepository
import com.zixo.app.domain.repository.StatusRepository
import com.zixo.app.data.repository.CallRepositoryImpl
import com.zixo.app.data.repository.ChatRepositoryImpl
import com.zixo.app.data.repository.ContactRepositoryImpl
import com.zixo.app.data.repository.SettingsRepositoryImpl
import com.zixo.app.data.repository.StatusRepositoryImpl
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

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val PREFERENCES_NAME = "zixo_preferences"
    private const val BASE_URL = "https://api.zixo.app/"

    // ── DataStore ───────────────────────────────────────────────────────────

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

    // ── Room Database ───────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideZixoDatabase(
        @ApplicationContext context: Context
    ): ZixoDatabase = Room.databaseBuilder(
        context,
        ZixoDatabase::class.java,
        ZixoDatabase.DATABASE_NAME
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideChatDao(database: ZixoDatabase): ChatDao =
        database.chatDao()

    @Provides
    fun provideCallLogDao(database: ZixoDatabase): CallLogDao =
        database.callLogDao()

    // ── Networking ──────────────────────────────────────────────────────────

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
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideCloudflareApiService(retrofit: Retrofit): CloudflareApiService =
        retrofit.create(CloudflareApiService::class.java)

    // ── Repository Bindings ─────────────────────────────────────────────────
    // Binds domain repository interfaces to their concrete implementations.
    // All implementations are @Singleton and injected via @Inject constructor().

    @Provides
    @Singleton
    fun provideContactRepository(impl: ContactRepositoryImpl): ContactRepository = impl

    @Provides
    @Singleton
    fun provideChatRepository(impl: ChatRepositoryImpl): ChatRepository = impl

    @Provides
    @Singleton
    fun provideCallRepository(impl: CallRepositoryImpl): CallRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    @Provides
    @Singleton
    fun provideStatusRepository(impl: StatusRepositoryImpl): StatusRepository = impl
}
