package com.zixo.app.di;

import com.zixo.app.data.repository.CallRepositoryImpl;
import com.zixo.app.domain.repository.CallRepository;
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
public final class AppModule_ProvideCallRepositoryFactory implements Factory<CallRepository> {
  private final Provider<CallRepositoryImpl> implProvider;

  public AppModule_ProvideCallRepositoryFactory(Provider<CallRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public CallRepository get() {
    return provideCallRepository(implProvider.get());
  }

  public static AppModule_ProvideCallRepositoryFactory create(
      Provider<CallRepositoryImpl> implProvider) {
    return new AppModule_ProvideCallRepositoryFactory(implProvider);
  }

  public static CallRepository provideCallRepository(CallRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCallRepository(impl));
  }
}
