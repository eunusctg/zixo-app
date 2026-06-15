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
public final class EncryptMessageUseCase_Factory implements Factory<EncryptMessageUseCase> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  public EncryptMessageUseCase_Factory(Provider<ContactRepository> contactRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public EncryptMessageUseCase get() {
    return newInstance(contactRepositoryProvider.get());
  }

  public static EncryptMessageUseCase_Factory create(
      Provider<ContactRepository> contactRepositoryProvider) {
    return new EncryptMessageUseCase_Factory(contactRepositoryProvider);
  }

  public static EncryptMessageUseCase newInstance(ContactRepository contactRepository) {
    return new EncryptMessageUseCase(contactRepository);
  }
}
