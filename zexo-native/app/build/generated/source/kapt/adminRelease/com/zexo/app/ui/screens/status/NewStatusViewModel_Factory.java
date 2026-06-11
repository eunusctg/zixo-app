package com.zexo.app.ui.screens.status;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.StatusRepository;
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
public final class NewStatusViewModel_Factory implements Factory<NewStatusViewModel> {
  private final Provider<StatusRepository> statusRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public NewStatusViewModel_Factory(Provider<StatusRepository> statusRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.statusRepositoryProvider = statusRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public NewStatusViewModel get() {
    return newInstance(statusRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static NewStatusViewModel_Factory create(
      javax.inject.Provider<StatusRepository> statusRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider) {
    return new NewStatusViewModel_Factory(Providers.asDaggerProvider(statusRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider));
  }

  public static NewStatusViewModel_Factory create(
      Provider<StatusRepository> statusRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new NewStatusViewModel_Factory(statusRepositoryProvider, authRepositoryProvider);
  }

  public static NewStatusViewModel newInstance(StatusRepository statusRepository,
      AuthRepository authRepository) {
    return new NewStatusViewModel(statusRepository, authRepository);
  }
}
