package com.zixo.app.ui.chat;

import androidx.lifecycle.SavedStateHandle;
import com.zixo.app.domain.repository.ChatRepository;
import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.usecase.SendMessageUseCase;
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
public final class GroupChatViewModel_Factory implements Factory<GroupChatViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<SendMessageUseCase> sendMessageUseCaseProvider;

  public GroupChatViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.sendMessageUseCaseProvider = sendMessageUseCaseProvider;
  }

  @Override
  public GroupChatViewModel get() {
    return newInstance(savedStateHandleProvider.get(), chatRepositoryProvider.get(), contactRepositoryProvider.get(), sendMessageUseCaseProvider.get());
  }

  public static GroupChatViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider) {
    return new GroupChatViewModel_Factory(savedStateHandleProvider, chatRepositoryProvider, contactRepositoryProvider, sendMessageUseCaseProvider);
  }

  public static GroupChatViewModel newInstance(SavedStateHandle savedStateHandle,
      ChatRepository chatRepository, ContactRepository contactRepository,
      SendMessageUseCase sendMessageUseCase) {
    return new GroupChatViewModel(savedStateHandle, chatRepository, contactRepository, sendMessageUseCase);
  }
}
