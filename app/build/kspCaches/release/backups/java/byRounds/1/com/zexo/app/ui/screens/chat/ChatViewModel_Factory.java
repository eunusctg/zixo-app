package com.zexo.app.ui.screens.chat;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.ChatRepository;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public ChatViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(authRepositoryProvider.get(), chatRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new ChatViewModel_Factory(authRepositoryProvider, chatRepositoryProvider, userRepositoryProvider);
  }

  public static ChatViewModel newInstance(AuthRepository authRepository,
      ChatRepository chatRepository, UserRepository userRepository) {
    return new ChatViewModel(authRepository, chatRepository, userRepository);
  }
}
