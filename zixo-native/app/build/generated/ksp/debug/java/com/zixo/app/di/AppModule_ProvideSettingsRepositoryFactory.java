package com.zixo.app.di;

import com.zixo.app.data.repository.SettingsRepositoryImpl;
import com.zixo.app.domain.repository.SettingsRepository;
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
public final class AppModule_ProvideSettingsRepositoryFactory implements Factory<SettingsRepository> {
  private final Provider<SettingsRepositoryImpl> implProvider;

  public AppModule_ProvideSettingsRepositoryFactory(Provider<SettingsRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public SettingsRepository get() {
    return provideSettingsRepository(implProvider.get());
  }

  public static AppModule_ProvideSettingsRepositoryFactory create(
      Provider<SettingsRepositoryImpl> implProvider) {
    return new AppModule_ProvideSettingsRepositoryFactory(implProvider);
  }

  public static SettingsRepository provideSettingsRepository(SettingsRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSettingsRepository(impl));
  }
}
