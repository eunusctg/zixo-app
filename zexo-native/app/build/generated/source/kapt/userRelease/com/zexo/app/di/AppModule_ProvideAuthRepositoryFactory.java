package com.zexo.app.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.FirebaseAuthRestApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> rtdbProvider;

  private final Provider<FirebaseAuthRestApi> restApiProvider;

  public AppModule_ProvideAuthRepositoryFactory(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseDatabase> rtdbProvider,
      Provider<FirebaseAuthRestApi> restApiProvider) {
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
    this.rtdbProvider = rtdbProvider;
    this.restApiProvider = restApiProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(authProvider.get(), firestoreProvider.get(), rtdbProvider.get(), restApiProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryFactory create(
      javax.inject.Provider<FirebaseAuth> authProvider,
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<FirebaseDatabase> rtdbProvider,
      javax.inject.Provider<FirebaseAuthRestApi> restApiProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(Providers.asDaggerProvider(authProvider), Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(rtdbProvider), Providers.asDaggerProvider(restApiProvider));
  }

  public static AppModule_ProvideAuthRepositoryFactory create(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseDatabase> rtdbProvider,
      Provider<FirebaseAuthRestApi> restApiProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(authProvider, firestoreProvider, rtdbProvider, restApiProvider);
  }

  public static AuthRepository provideAuthRepository(FirebaseAuth auth, FirebaseFirestore firestore,
      FirebaseDatabase rtdb, FirebaseAuthRestApi restApi) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepository(auth, firestore, rtdb, restApi));
  }
}
