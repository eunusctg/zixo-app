package com.zexo.app.ui.screens.profile;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.StorageRepository;
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
public final class EditProfileViewModel_Factory implements Factory<EditProfileViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<StorageRepository> storageRepositoryProvider;

  public EditProfileViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<StorageRepository> storageRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.storageRepositoryProvider = storageRepositoryProvider;
  }

  @Override
  public EditProfileViewModel get() {
    return newInstance(authRepositoryProvider.get(), userRepositoryProvider.get(), storageRepositoryProvider.get());
  }

  public static EditProfileViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<StorageRepository> storageRepositoryProvider) {
    return new EditProfileViewModel_Factory(authRepositoryProvider, userRepositoryProvider, storageRepositoryProvider);
  }

  public static EditProfileViewModel newInstance(AuthRepository authRepository,
      UserRepository userRepository, StorageRepository storageRepository) {
    return new EditProfileViewModel(authRepository, userRepository, storageRepository);
  }
}
