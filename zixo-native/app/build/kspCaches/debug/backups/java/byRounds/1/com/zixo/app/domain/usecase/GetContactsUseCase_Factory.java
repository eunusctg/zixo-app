package com.zixo.app.domain.usecase;

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
public final class GetContactsUseCase_Factory implements Factory<GetContactsUseCase> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  public GetContactsUseCase_Factory(Provider<ContactRepository> contactRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public GetContactsUseCase get() {
    return newInstance(contactRepositoryProvider.get());
  }

  public static GetContactsUseCase_Factory create(
      Provider<ContactRepository> contactRepositoryProvider) {
    return new GetContactsUseCase_Factory(contactRepositoryProvider);
  }

  public static GetContactsUseCase newInstance(ContactRepository contactRepository) {
    return new GetContactsUseCase(contactRepository);
  }
}
