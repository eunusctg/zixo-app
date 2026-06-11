package com.zexo.app.ui.screens.admin;

import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.UserRepository;
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
public final class AdminViewModel_Factory implements Factory<AdminViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  public AdminViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public AdminViewModel get() {
    return newInstance(authRepositoryProvider.get(), userRepositoryProvider.get(), firestoreProvider.get());
  }

  public static AdminViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider,
      javax.inject.Provider<FirebaseFirestore> firestoreProvider) {
    return new AdminViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(userRepositoryProvider), Providers.asDaggerProvider(firestoreProvider));
  }

  public static AdminViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    return new AdminViewModel_Factory(authRepositoryProvider, userRepositoryProvider, firestoreProvider);
  }

  public static AdminViewModel newInstance(AuthRepository authRepository,
      UserRepository userRepository, FirebaseFirestore firestore) {
    return new AdminViewModel(authRepository, userRepository, firestore);
  }
}
