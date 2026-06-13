package com.zexo.app.ui.screens.calls;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
import com.zexo.app.data.repository.UserRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CallsViewModel_Factory implements Factory<CallsViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public CallsViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.callRepositoryProvider = callRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public CallsViewModel get() {
    return newInstance(authRepositoryProvider.get(), callRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static CallsViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new CallsViewModel_Factory(authRepositoryProvider, callRepositoryProvider, userRepositoryProvider);
  }

  public static CallsViewModel newInstance(AuthRepository authRepository,
      CallRepository callRepository, UserRepository userRepository) {
    return new CallsViewModel(authRepository, callRepository, userRepository);
  }
}
