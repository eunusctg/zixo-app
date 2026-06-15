package com.zixo.app.ui.screens.chats;

import com.zixo.app.domain.repository.ChatRepository;
import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.usecase.GetContactsUseCase;
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
public final class ChatsViewModel_Factory implements Factory<ChatsViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<GetContactsUseCase> getContactsUseCaseProvider;

  public ChatsViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<GetContactsUseCase> getContactsUseCaseProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.getContactsUseCaseProvider = getContactsUseCaseProvider;
  }

  @Override
  public ChatsViewModel get() {
    return newInstance(chatRepositoryProvider.get(), contactRepositoryProvider.get(), getContactsUseCaseProvider.get());
  }

  public static ChatsViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<GetContactsUseCase> getContactsUseCaseProvider) {
    return new ChatsViewModel_Factory(chatRepositoryProvider, contactRepositoryProvider, getContactsUseCaseProvider);
  }

  public static ChatsViewModel newInstance(ChatRepository chatRepository,
      ContactRepository contactRepository, GetContactsUseCase getContactsUseCase) {
    return new ChatsViewModel(chatRepository, contactRepository, getContactsUseCase);
  }
}
