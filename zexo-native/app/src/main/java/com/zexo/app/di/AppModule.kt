package com.zexo.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.zexo.app.data.repository.*
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
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideRtdb(): FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://zixo-call-default-rtdb.firebaseio.com/"
    ).apply {
        setPersistenceEnabled(true)
    }

    @Provides
    @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideAuthRestApi(): FirebaseAuthRestApi = FirebaseAuthRestApi()

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        rtdb: FirebaseDatabase,
        restApi: FirebaseAuthRestApi
    ): AuthRepository = AuthRepository(auth, firestore, rtdb, restApi)

    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore,
        rtdb: FirebaseDatabase
    ): ChatRepository = ChatRepository(firestore, rtdb)

    @Provides
    @Singleton
    fun provideCallRepository(
        firestore: FirebaseFirestore,
        rtdb: FirebaseDatabase
    ): CallRepository = CallRepository(firestore, rtdb)

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore
    ): UserRepository = UserRepository(firestore)

    @Provides
    @Singleton
    fun provideStatusRepository(
        firestore: FirebaseFirestore
    ): StatusRepository = StatusRepository(firestore)

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
