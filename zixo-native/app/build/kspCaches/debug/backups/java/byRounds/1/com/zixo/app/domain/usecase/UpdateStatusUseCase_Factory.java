package com.zixo.app.domain.usecase;

import com.zixo.app.domain.repository.StatusRepository;
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
public final class UpdateStatusUseCase_Factory implements Factory<UpdateStatusUseCase> {
  private final Provider<StatusRepository> statusRepositoryProvider;

  public UpdateStatusUseCase_Factory(Provider<StatusRepository> statusRepositoryProvider) {
    this.statusRepositoryProvider = statusRepositoryProvider;
  }

  @Override
  public UpdateStatusUseCase get() {
    return newInstance(statusRepositoryProvider.get());
  }

  public static UpdateStatusUseCase_Factory create(
      Provider<StatusRepository> statusRepositoryProvider) {
    return new UpdateStatusUseCase_Factory(statusRepositoryProvider);
  }

  public static UpdateStatusUseCase newInstance(StatusRepository statusRepository) {
    return new UpdateStatusUseCase(statusRepository);
  }
}
