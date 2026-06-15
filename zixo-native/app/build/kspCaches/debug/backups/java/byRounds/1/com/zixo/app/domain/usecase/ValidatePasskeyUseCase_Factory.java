package com.zixo.app.domain.usecase;

import com.zixo.app.domain.repository.AuthRepository;
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
public final class ValidatePasskeyUseCase_Factory implements Factory<ValidatePasskeyUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public ValidatePasskeyUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ValidatePasskeyUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static ValidatePasskeyUseCase_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new ValidatePasskeyUseCase_Factory(authRepositoryProvider);
  }

  public static ValidatePasskeyUseCase newInstance(AuthRepository authRepository) {
    return new ValidatePasskeyUseCase(authRepository);
  }
}
