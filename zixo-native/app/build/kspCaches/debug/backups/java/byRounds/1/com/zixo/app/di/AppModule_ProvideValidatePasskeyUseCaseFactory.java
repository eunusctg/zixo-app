package com.zixo.app.di;

import com.zixo.app.domain.repository.AuthRepository;
import com.zixo.app.domain.usecase.ValidatePasskeyUseCase;
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
public final class AppModule_ProvideValidatePasskeyUseCaseFactory implements Factory<ValidatePasskeyUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public AppModule_ProvideValidatePasskeyUseCaseFactory(
      Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ValidatePasskeyUseCase get() {
    return provideValidatePasskeyUseCase(authRepositoryProvider.get());
  }

  public static AppModule_ProvideValidatePasskeyUseCaseFactory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new AppModule_ProvideValidatePasskeyUseCaseFactory(authRepositoryProvider);
  }

  public static ValidatePasskeyUseCase provideValidatePasskeyUseCase(
      AuthRepository authRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideValidatePasskeyUseCase(authRepository));
  }
}
