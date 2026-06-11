package com.zexo.app.ui.screens.contacts;

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
public final class NewChatViewModel_Factory implements Factory<NewChatViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public NewChatViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public NewChatViewModel get() {
    return newInstance(authRepositoryProvider.get(), chatRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static NewChatViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<ChatRepository> chatRepositoryProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider) {
    return new NewChatViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(chatRepositoryProvider), Providers.asDaggerProvider(userRepositoryProvider));
  }

  public static NewChatViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new NewChatViewModel_Factory(authRepositoryProvider, chatRepositoryProvider, userRepositoryProvider);
  }

  public static NewChatViewModel newInstance(AuthRepository authRepository,
      ChatRepository chatRepository, UserRepository userRepository) {
    return new NewChatViewModel(authRepository, chatRepository, userRepository);
  }
}
