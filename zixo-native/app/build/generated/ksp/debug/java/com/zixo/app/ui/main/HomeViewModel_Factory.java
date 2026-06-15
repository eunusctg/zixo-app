package com.zixo.app.ui.main;

import com.zixo.app.domain.repository.AuthRepository;
import com.zixo.app.domain.repository.ChatRepository;
import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.repository.SettingsRepository;
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

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public HomeViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(authRepositoryProvider.get(), chatRepositoryProvider.get(), contactRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new HomeViewModel_Factory(authRepositoryProvider, chatRepositoryProvider, contactRepositoryProvider, settingsRepositoryProvider);
  }

  public static HomeViewModel newInstance(AuthRepository authRepository,
      ChatRepository chatRepository, ContactRepository contactRepository,
      SettingsRepository settingsRepository) {
    return new HomeViewModel(authRepository, chatRepository, contactRepository, settingsRepository);
  }
}
