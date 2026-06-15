package com.zixo.app.di;

import com.zixo.app.data.repository.AuthRepositoryImpl;
import com.zixo.app.domain.repository.AuthRepository;
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
public final class AppModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<AuthRepositoryImpl> implProvider;

  public AppModule_ProvideAuthRepositoryFactory(Provider<AuthRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(implProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryFactory create(
      Provider<AuthRepositoryImpl> implProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(implProvider);
  }

  public static AuthRepository provideAuthRepository(AuthRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepository(impl));
  }
}
