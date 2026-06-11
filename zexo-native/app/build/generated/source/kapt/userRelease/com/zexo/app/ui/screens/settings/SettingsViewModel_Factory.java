package com.zexo.app.ui.screens.settings;

import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  public SettingsViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(authRepositoryProvider.get(), firestoreProvider.get());
  }

  public static SettingsViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<FirebaseFirestore> firestoreProvider) {
    return new SettingsViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(firestoreProvider));
  }

  public static SettingsViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    return new SettingsViewModel_Factory(authRepositoryProvider, firestoreProvider);
  }

  public static SettingsViewModel newInstance(AuthRepository authRepository,
      FirebaseFirestore firestore) {
    return new SettingsViewModel(authRepository, firestore);
  }
}
