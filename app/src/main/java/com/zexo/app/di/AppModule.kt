package com.zexo.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import com.zexo.app.data.local.PreferencesManager
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.CallRepository
import com.zexo.app.data.repository.ChatRepository
import com.zexo.app.data.repository.StatusRepository
import com.zexo.app.data.repository.StorageRepository
import com.zexo.app.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        db.firestoreSettings = settings
        return db
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance("https://zixo-call-default-rtdb.firebaseio.com")
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager =
        PreferencesManager(context)

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth, firestore: FirebaseFirestore): AuthRepository =
        AuthRepository(auth, firestore)

    @Provides
    @Singleton
    fun provideChatRepository(firestore: FirebaseFirestore): ChatRepository =
        ChatRepository(firestore)

    @Provides
    @Singleton
    fun provideCallRepository(firestore: FirebaseFirestore): CallRepository =
        CallRepository(firestore)

    @Provides
    @Singleton
    fun provideStatusRepository(firestore: FirebaseFirestore): StatusRepository =
        StatusRepository(firestore)

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository =
        UserRepository(firestore)

    @Provides
    @Singleton
    fun provideStorageRepository(storage: FirebaseStorage): StorageRepository =
        StorageRepository(storage)
}
