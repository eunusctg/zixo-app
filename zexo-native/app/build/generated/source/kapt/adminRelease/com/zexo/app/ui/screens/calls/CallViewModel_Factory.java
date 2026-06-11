package com.zexo.app.ui.screens.calls;

import com.google.firebase.database.FirebaseDatabase;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
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
public final class CallViewModel_Factory implements Factory<CallViewModel> {
  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<FirebaseDatabase> rtdbProvider;

  public CallViewModel_Factory(Provider<CallRepository> callRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<FirebaseDatabase> rtdbProvider) {
    this.callRepositoryProvider = callRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.rtdbProvider = rtdbProvider;
  }

  @Override
  public CallViewModel get() {
    return newInstance(callRepositoryProvider.get(), authRepositoryProvider.get(), userRepositoryProvider.get(), rtdbProvider.get());
  }

  public static CallViewModel_Factory create(
      javax.inject.Provider<CallRepository> callRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider,
      javax.inject.Provider<FirebaseDatabase> rtdbProvider) {
    return new CallViewModel_Factory(Providers.asDaggerProvider(callRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(userRepositoryProvider), Providers.asDaggerProvider(rtdbProvider));
  }

  public static CallViewModel_Factory create(Provider<CallRepository> callRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider, Provider<FirebaseDatabase> rtdbProvider) {
    return new CallViewModel_Factory(callRepositoryProvider, authRepositoryProvider, userRepositoryProvider, rtdbProvider);
  }

  public static CallViewModel newInstance(CallRepository callRepository,
      AuthRepository authRepository, UserRepository userRepository, FirebaseDatabase rtdb) {
    return new CallViewModel(callRepository, authRepository, userRepository, rtdb);
  }
}
