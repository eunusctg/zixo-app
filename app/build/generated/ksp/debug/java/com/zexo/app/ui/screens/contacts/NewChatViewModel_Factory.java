package com.zexo.app.ui.screens.contacts;

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
public final class NewChatViewModel_Factory implements Factory<NewChatViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  public NewChatViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public NewChatViewModel get() {
    return newInstance(userRepositoryProvider.get(), authRepositoryProvider.get(), chatRepositoryProvider.get());
  }

  public static NewChatViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider) {
    return new NewChatViewModel_Factory(userRepositoryProvider, authRepositoryProvider, chatRepositoryProvider);
  }

  public static NewChatViewModel newInstance(UserRepository userRepository,
      AuthRepository authRepository, ChatRepository chatRepository) {
    return new NewChatViewModel(userRepository, authRepository, chatRepository);
  }
}
