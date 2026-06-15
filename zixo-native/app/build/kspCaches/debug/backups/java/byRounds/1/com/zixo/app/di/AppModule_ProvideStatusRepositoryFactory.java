package com.zixo.app.di;

import com.zixo.app.data.repository.StatusRepositoryImpl;
import com.zixo.app.domain.repository.StatusRepository;
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
public final class AppModule_ProvideStatusRepositoryFactory implements Factory<StatusRepository> {
  private final Provider<StatusRepositoryImpl> implProvider;

  public AppModule_ProvideStatusRepositoryFactory(Provider<StatusRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public StatusRepository get() {
    return provideStatusRepository(implProvider.get());
  }

  public static AppModule_ProvideStatusRepositoryFactory create(
      Provider<StatusRepositoryImpl> implProvider) {
    return new AppModule_ProvideStatusRepositoryFactory(implProvider);
  }

  public static StatusRepository provideStatusRepository(StatusRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideStatusRepository(impl));
  }
}
