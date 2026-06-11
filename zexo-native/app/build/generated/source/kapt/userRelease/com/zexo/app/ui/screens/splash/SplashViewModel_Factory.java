package com.zexo.app.ui.screens.splash;

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
public final class SplashViewModel_Factory implements Factory<SplashViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public SplashViewModel_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SplashViewModel get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static SplashViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider) {
    return new SplashViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider));
  }

  public static SplashViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new SplashViewModel_Factory(authRepositoryProvider);
  }

  public static SplashViewModel newInstance(AuthRepository authRepository) {
    return new SplashViewModel(authRepository);
  }
}
