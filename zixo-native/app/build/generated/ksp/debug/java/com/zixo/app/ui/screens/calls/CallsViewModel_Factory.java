package com.zixo.app.ui.screens.calls;

import com.google.firebase.auth.FirebaseAuth;
import com.zixo.app.domain.repository.CallRepository;
import com.zixo.app.domain.usecase.InitiateCallUseCase;
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
public final class CallsViewModel_Factory implements Factory<CallsViewModel> {
  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<InitiateCallUseCase> initiateCallUseCaseProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  public CallsViewModel_Factory(Provider<CallRepository> callRepositoryProvider,
      Provider<InitiateCallUseCase> initiateCallUseCaseProvider,
      Provider<FirebaseAuth> firebaseAuthProvider) {
    this.callRepositoryProvider = callRepositoryProvider;
    this.initiateCallUseCaseProvider = initiateCallUseCaseProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
  }

  @Override
  public CallsViewModel get() {
    return newInstance(callRepositoryProvider.get(), initiateCallUseCaseProvider.get(), firebaseAuthProvider.get());
  }

  public static CallsViewModel_Factory create(Provider<CallRepository> callRepositoryProvider,
      Provider<InitiateCallUseCase> initiateCallUseCaseProvider,
      Provider<FirebaseAuth> firebaseAuthProvider) {
    return new CallsViewModel_Factory(callRepositoryProvider, initiateCallUseCaseProvider, firebaseAuthProvider);
  }

  public static CallsViewModel newInstance(CallRepository callRepository,
      InitiateCallUseCase initiateCallUseCase, FirebaseAuth firebaseAuth) {
    return new CallsViewModel(callRepository, initiateCallUseCase, firebaseAuth);
  }
}
