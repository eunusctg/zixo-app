package com.zexo.app.ui.screens.profile;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.ChatRepository;
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public ProfileViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(userRepositoryProvider.get(), chatRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static ProfileViewModel_Factory create(
      javax.inject.Provider<UserRepository> userRepositoryProvider,
      javax.inject.Provider<ChatRepository> chatRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider) {
    return new ProfileViewModel_Factory(Providers.asDaggerProvider(userRepositoryProvider), Providers.asDaggerProvider(chatRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider));
  }

  public static ProfileViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new ProfileViewModel_Factory(userRepositoryProvider, chatRepositoryProvider, authRepositoryProvider);
  }

  public static ProfileViewModel newInstance(UserRepository userRepository,
      ChatRepository chatRepository, AuthRepository authRepository) {
    return new ProfileViewModel(userRepository, chatRepository, authRepository);
  }
}
