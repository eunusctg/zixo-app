package com.zexo.app.ui.screens.calls;

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
public final class DialPadViewModel_Factory implements Factory<DialPadViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public DialPadViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public DialPadViewModel get() {
    return newInstance(userRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static DialPadViewModel_Factory create(
      javax.inject.Provider<UserRepository> userRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider) {
    return new DialPadViewModel_Factory(Providers.asDaggerProvider(userRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider));
  }

  public static DialPadViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new DialPadViewModel_Factory(userRepositoryProvider, authRepositoryProvider);
  }

  public static DialPadViewModel newInstance(UserRepository userRepository,
      AuthRepository authRepository) {
    return new DialPadViewModel(userRepository, authRepository);
  }
}
