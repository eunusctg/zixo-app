package com.zixo.app.di;

import com.zixo.app.domain.repository.CallRepository;
import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.usecase.InitiateCallUseCase;
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
public final class AppModule_ProvideInitiateCallUseCaseFactory implements Factory<InitiateCallUseCase> {
  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public AppModule_ProvideInitiateCallUseCaseFactory(
      Provider<CallRepository> callRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.callRepositoryProvider = callRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public InitiateCallUseCase get() {
    return provideInitiateCallUseCase(callRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static AppModule_ProvideInitiateCallUseCaseFactory create(
      Provider<CallRepository> callRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new AppModule_ProvideInitiateCallUseCaseFactory(callRepositoryProvider, contactRepositoryProvider);
  }

  public static InitiateCallUseCase provideInitiateCallUseCase(CallRepository callRepository,
      ContactRepository contactRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideInitiateCallUseCase(callRepository, contactRepository));
  }
}
