package com.zixo.app.data.repository;

import android.content.Context;
import com.zixo.app.data.local.PreferencesDataStore;
import com.zixo.app.data.local.room.ZixoDatabase;
import com.zixo.app.data.remote.firebase.FirebaseAuthService;
import com.zixo.app.data.remote.firebase.FirestoreService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SettingsRepositoryImpl_Factory implements Factory<SettingsRepositoryImpl> {
  private final Provider<PreferencesDataStore> preferencesDataStoreProvider;

  private final Provider<ZixoDatabase> databaseProvider;

  private final Provider<FirestoreService> firestoreServiceProvider;

  private final Provider<FirebaseAuthService> firebaseAuthServiceProvider;

  private final Provider<Context> contextProvider;

  public SettingsRepositoryImpl_Factory(Provider<PreferencesDataStore> preferencesDataStoreProvider,
      Provider<ZixoDatabase> databaseProvider, Provider<FirestoreService> firestoreServiceProvider,
      Provider<FirebaseAuthService> firebaseAuthServiceProvider,
      Provider<Context> contextProvider) {
    this.preferencesDataStoreProvider = preferencesDataStoreProvider;
    this.databaseProvider = databaseProvider;
    this.firestoreServiceProvider = firestoreServiceProvider;
    this.firebaseAuthServiceProvider = firebaseAuthServiceProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsRepositoryImpl get() {
    return newInstance(preferencesDataStoreProvider.get(), databaseProvider.get(), firestoreServiceProvider.get(), firebaseAuthServiceProvider.get(), contextProvider.get());
  }

  public static SettingsRepositoryImpl_Factory create(
      Provider<PreferencesDataStore> preferencesDataStoreProvider,
      Provider<ZixoDatabase> databaseProvider, Provider<FirestoreService> firestoreServiceProvider,
      Provider<FirebaseAuthService> firebaseAuthServiceProvider,
      Provider<Context> contextProvider) {
    return new SettingsRepositoryImpl_Factory(preferencesDataStoreProvider, databaseProvider, firestoreServiceProvider, firebaseAuthServiceProvider, contextProvider);
  }

  public static SettingsRepositoryImpl newInstance(PreferencesDataStore preferencesDataStore,
      ZixoDatabase database, FirestoreService firestoreService,
      FirebaseAuthService firebaseAuthService, Context context) {
    return new SettingsRepositoryImpl(preferencesDataStore, database, firestoreService, firebaseAuthService, context);
  }
}
