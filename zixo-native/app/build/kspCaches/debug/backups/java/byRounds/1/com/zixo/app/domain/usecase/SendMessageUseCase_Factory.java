package com.zixo.app.domain.usecase;

import com.zixo.app.domain.repository.ChatRepository;
import com.zixo.app.domain.repository.ContactRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SendMessageUseCase_Factory implements Factory<SendMessageUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<EncryptMessageUseCase> encryptMessageUseCaseProvider;

  public SendMessageUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<EncryptMessageUseCase> encryptMessageUseCaseProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.encryptMessageUseCaseProvider = encryptMessageUseCaseProvider;
  }

  @Override
  public SendMessageUseCase get() {
    return newInstance(chatRepositoryProvider.get(), contactRepositoryProvider.get(), encryptMessageUseCaseProvider.get());
  }

  public static SendMessageUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<EncryptMessageUseCase> encryptMessageUseCaseProvider) {
    return new SendMessageUseCase_Factory(chatRepositoryProvider, contactRepositoryProvider, encryptMessageUseCaseProvider);
  }

  public static SendMessageUseCase newInstance(ChatRepository chatRepository,
      ContactRepository contactRepository, EncryptMessageUseCase encryptMessageUseCase) {
    return new SendMessageUseCase(chatRepository, contactRepository, encryptMessageUseCase);
  }
}
