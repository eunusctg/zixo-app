package com.zixo.app.di;

import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.usecase.GetContactsUseCase;
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
public final class AppModule_ProvideGetContactsUseCaseFactory implements Factory<GetContactsUseCase> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  public AppModule_ProvideGetContactsUseCaseFactory(
      Provider<ContactRepository> contactRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public GetContactsUseCase get() {
    return provideGetContactsUseCase(contactRepositoryProvider.get());
  }

  public static AppModule_ProvideGetContactsUseCaseFactory create(
      Provider<ContactRepository> contactRepositoryProvider) {
    return new AppModule_ProvideGetContactsUseCaseFactory(contactRepositoryProvider);
  }

  public static GetContactsUseCase provideGetContactsUseCase(ContactRepository contactRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGetContactsUseCase(contactRepository));
  }
}
