package com.zixo.app.di;

import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.repository.StatusRepository;
import com.zixo.app.domain.usecase.UpdateStatusUseCase;
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
public final class AppModule_ProvideUpdateStatusUseCaseFactory implements Factory<UpdateStatusUseCase> {
  private final Provider<StatusRepository> statusRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public AppModule_ProvideUpdateStatusUseCaseFactory(
      Provider<StatusRepository> statusRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.statusRepositoryProvider = statusRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public UpdateStatusUseCase get() {
    return provideUpdateStatusUseCase(statusRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static AppModule_ProvideUpdateStatusUseCaseFactory create(
      Provider<StatusRepository> statusRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new AppModule_ProvideUpdateStatusUseCaseFactory(statusRepositoryProvider, contactRepositoryProvider);
  }

  public static UpdateStatusUseCase provideUpdateStatusUseCase(StatusRepository statusRepository,
      ContactRepository contactRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUpdateStatusUseCase(statusRepository, contactRepository));
  }
}
