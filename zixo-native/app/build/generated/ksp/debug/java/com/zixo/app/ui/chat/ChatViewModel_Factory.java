package com.zixo.app.ui.chat;

import androidx.lifecycle.SavedStateHandle;
import com.zixo.app.domain.repository.CallRepository;
import com.zixo.app.domain.repository.ChatRepository;
import com.zixo.app.domain.repository.ContactRepository;
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
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.callRepositoryProvider = callRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), contactRepositoryProvider.get(), callRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, contactRepositoryProvider, callRepositoryProvider, savedStateHandleProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      ContactRepository contactRepository, CallRepository callRepository,
      SavedStateHandle savedStateHandle) {
    return new ChatViewModel(chatRepository, contactRepository, callRepository, savedStateHandle);
  }
}
