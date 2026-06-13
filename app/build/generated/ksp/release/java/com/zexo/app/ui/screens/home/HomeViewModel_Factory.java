package com.zexo.app.ui.screens.home;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public HomeViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.callRepositoryProvider = callRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(authRepositoryProvider.get(), chatRepositoryProvider.get(), callRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new HomeViewModel_Factory(authRepositoryProvider, chatRepositoryProvider, callRepositoryProvider, userRepositoryProvider);
  }

  public static HomeViewModel newInstance(AuthRepository authRepository,
      ChatRepository chatRepository, CallRepository callRepository, UserRepository userRepository) {
    return new HomeViewModel(authRepository, chatRepository, callRepository, userRepository);
  }
}
