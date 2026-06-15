package com.zixo.app.data.repository;

import com.zixo.app.data.remote.cloudflare.CloudflareApiService;
import com.zixo.app.data.remote.firebase.FirebaseAuthService;
import com.zixo.app.data.remote.firebase.FirestoreService;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<FirebaseAuthService> firebaseAuthServiceProvider;

  private final Provider<FirestoreService> firestoreServiceProvider;

  private final Provider<CloudflareApiService> cloudflareApiServiceProvider;

  public AuthRepositoryImpl_Factory(Provider<FirebaseAuthService> firebaseAuthServiceProvider,
      Provider<FirestoreService> firestoreServiceProvider,
      Provider<CloudflareApiService> cloudflareApiServiceProvider) {
    this.firebaseAuthServiceProvider = firebaseAuthServiceProvider;
    this.firestoreServiceProvider = firestoreServiceProvider;
    this.cloudflareApiServiceProvider = cloudflareApiServiceProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(firebaseAuthServiceProvider.get(), firestoreServiceProvider.get(), cloudflareApiServiceProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(
      Provider<FirebaseAuthService> firebaseAuthServiceProvider,
      Provider<FirestoreService> firestoreServiceProvider,
      Provider<CloudflareApiService> cloudflareApiServiceProvider) {
    return new AuthRepositoryImpl_Factory(firebaseAuthServiceProvider, firestoreServiceProvider, cloudflareApiServiceProvider);
  }

  public static AuthRepositoryImpl newInstance(FirebaseAuthService firebaseAuthService,
      FirestoreService firestoreService, CloudflareApiService cloudflareApiService) {
    return new AuthRepositoryImpl(firebaseAuthService, firestoreService, cloudflareApiService);
  }
}
