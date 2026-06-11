package com.zexo.app.ui.screens.profile;

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
public final class ProfileEditViewModel_Factory implements Factory<ProfileEditViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public ProfileEditViewModel_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ProfileEditViewModel get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static ProfileEditViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider) {
    return new ProfileEditViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider));
  }

  public static ProfileEditViewModel_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new ProfileEditViewModel_Factory(authRepositoryProvider);
  }

  public static ProfileEditViewModel newInstance(AuthRepository authRepository) {
    return new ProfileEditViewModel(authRepository);
  }
}
