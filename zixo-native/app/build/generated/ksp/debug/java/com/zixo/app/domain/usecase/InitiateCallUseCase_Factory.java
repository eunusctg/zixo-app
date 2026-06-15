package com.zixo.app.domain.usecase;

import com.zixo.app.domain.repository.CallRepository;
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
public final class InitiateCallUseCase_Factory implements Factory<InitiateCallUseCase> {
  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public InitiateCallUseCase_Factory(Provider<CallRepository> callRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.callRepositoryProvider = callRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public InitiateCallUseCase get() {
    return newInstance(callRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static InitiateCallUseCase_Factory create(Provider<CallRepository> callRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new InitiateCallUseCase_Factory(callRepositoryProvider, contactRepositoryProvider);
  }

  public static InitiateCallUseCase newInstance(CallRepository callRepository,
      ContactRepository contactRepository) {
    return new InitiateCallUseCase(callRepository, contactRepository);
  }
}
