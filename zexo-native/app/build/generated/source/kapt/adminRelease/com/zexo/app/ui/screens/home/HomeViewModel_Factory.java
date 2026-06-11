package com.zexo.app.ui.screens.home;

import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
import com.zexo.app.data.repository.ChatRepository;
import com.zexo.app.data.repository.StatusRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<StatusRepository> statusRepositoryProvider;

  public HomeViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<StatusRepository> statusRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.callRepositoryProvider = callRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.statusRepositoryProvider = statusRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(authRepositoryProvider.get(), chatRepositoryProvider.get(), callRepositoryProvider.get(), userRepositoryProvider.get(), statusRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<ChatRepository> chatRepositoryProvider,
      javax.inject.Provider<CallRepository> callRepositoryProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider,
      javax.inject.Provider<StatusRepository> statusRepositoryProvider) {
    return new HomeViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(chatRepositoryProvider), Providers.asDaggerProvider(callRepositoryProvider), Providers.asDaggerProvider(userRepositoryProvider), Providers.asDaggerProvider(statusRepositoryProvider));
  }

  public static HomeViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<StatusRepository> statusRepositoryProvider) {
    return new HomeViewModel_Factory(authRepositoryProvider, chatRepositoryProvider, callRepositoryProvider, userRepositoryProvider, statusRepositoryProvider);
  }

  public static HomeViewModel newInstance(AuthRepository authRepository,
      ChatRepository chatRepository, CallRepository callRepository, UserRepository userRepository,
      StatusRepository statusRepository) {
    return new HomeViewModel(authRepository, chatRepository, callRepository, userRepository, statusRepository);
  }
}
