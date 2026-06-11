package com.zexo.app.ui.screens.status;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.StatusRepository;
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
public final class StatusViewViewModel_Factory implements Factory<StatusViewViewModel> {
  private final Provider<StatusRepository> statusRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public StatusViewViewModel_Factory(Provider<StatusRepository> statusRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.statusRepositoryProvider = statusRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public StatusViewViewModel get() {
    return newInstance(statusRepositoryProvider.get(), authRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static StatusViewViewModel_Factory create(
      javax.inject.Provider<StatusRepository> statusRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider) {
    return new StatusViewViewModel_Factory(Providers.asDaggerProvider(statusRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(userRepositoryProvider));
  }

  public static StatusViewViewModel_Factory create(
      Provider<StatusRepository> statusRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new StatusViewViewModel_Factory(statusRepositoryProvider, authRepositoryProvider, userRepositoryProvider);
  }

  public static StatusViewViewModel newInstance(StatusRepository statusRepository,
      AuthRepository authRepository, UserRepository userRepository) {
    return new StatusViewViewModel(statusRepository, authRepository, userRepository);
  }
}
