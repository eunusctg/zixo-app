package com.zixo.app.ui.contacts;

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
public final class ContactListViewModel_Factory implements Factory<ContactListViewModel> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  public ContactListViewModel_Factory(Provider<ContactRepository> contactRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public ContactListViewModel get() {
    return newInstance(contactRepositoryProvider.get());
  }

  public static ContactListViewModel_Factory create(
      Provider<ContactRepository> contactRepositoryProvider) {
    return new ContactListViewModel_Factory(contactRepositoryProvider);
  }

  public static ContactListViewModel newInstance(ContactRepository contactRepository) {
    return new ContactListViewModel(contactRepository);
  }
}
