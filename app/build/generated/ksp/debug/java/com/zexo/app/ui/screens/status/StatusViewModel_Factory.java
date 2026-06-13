package com.zexo.app.ui.screens.status;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.StatusRepository;
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
public final class StatusViewModel_Factory implements Factory<StatusViewModel> {
  private final Provider<StatusRepository> statusRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public StatusViewModel_Factory(Provider<StatusRepository> statusRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.statusRepositoryProvider = statusRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public StatusViewModel get() {
    return newInstance(statusRepositoryProvider.get(), authRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static StatusViewModel_Factory create(Provider<StatusRepository> statusRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new StatusViewModel_Factory(statusRepositoryProvider, authRepositoryProvider, userRepositoryProvider);
  }

  public static StatusViewModel newInstance(StatusRepository statusRepository,
      AuthRepository authRepository, UserRepository userRepository) {
    return new StatusViewModel(statusRepository, authRepository, userRepository);
  }
}
