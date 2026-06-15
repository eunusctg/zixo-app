package com.zixo.app.di;

import com.zixo.app.domain.repository.ChatRepository;
import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.usecase.EncryptMessageUseCase;
import com.zixo.app.domain.usecase.SendMessageUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideSendMessageUseCaseFactory implements Factory<SendMessageUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<EncryptMessageUseCase> encryptMessageUseCaseProvider;

  public AppModule_ProvideSendMessageUseCaseFactory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<EncryptMessageUseCase> encryptMessageUseCaseProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.encryptMessageUseCaseProvider = encryptMessageUseCaseProvider;
  }

  @Override
  public SendMessageUseCase get() {
    return provideSendMessageUseCase(chatRepositoryProvider.get(), contactRepositoryProvider.get(), encryptMessageUseCaseProvider.get());
  }

  public static AppModule_ProvideSendMessageUseCaseFactory create(
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<EncryptMessageUseCase> encryptMessageUseCaseProvider) {
    return new AppModule_ProvideSendMessageUseCaseFactory(chatRepositoryProvider, contactRepositoryProvider, encryptMessageUseCaseProvider);
  }

  public static SendMessageUseCase provideSendMessageUseCase(ChatRepository chatRepository,
      ContactRepository contactRepository, EncryptMessageUseCase encryptMessageUseCase) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSendMessageUseCase(chatRepository, contactRepository, encryptMessageUseCase));
  }
}
