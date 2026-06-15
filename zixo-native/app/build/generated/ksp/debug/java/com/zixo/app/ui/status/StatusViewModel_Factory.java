package com.zixo.app.ui.status;

import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.repository.SettingsRepository;
import com.zixo.app.domain.repository.StatusRepository;
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
public final class StatusViewModel_Factory implements Factory<StatusViewModel> {
  private final Provider<StatusRepository> statusRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public StatusViewModel_Factory(Provider<StatusRepository> statusRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.statusRepositoryProvider = statusRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public StatusViewModel get() {
    return newInstance(statusRepositoryProvider.get(), contactRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static StatusViewModel_Factory create(Provider<StatusRepository> statusRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new StatusViewModel_Factory(statusRepositoryProvider, contactRepositoryProvider, settingsRepositoryProvider);
  }

  public static StatusViewModel newInstance(StatusRepository statusRepository,
      ContactRepository contactRepository, SettingsRepository settingsRepository) {
    return new StatusViewModel(statusRepository, contactRepository, settingsRepository);
  }
}
