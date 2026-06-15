package com.zixo.app.data.remote.firebase;

import com.google.firebase.auth.FirebaseAuth;
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
public final class FirebaseAuthService_Factory implements Factory<FirebaseAuthService> {
  private final Provider<FirebaseAuth> firebaseAuthProvider;

  public FirebaseAuthService_Factory(Provider<FirebaseAuth> firebaseAuthProvider) {
    this.firebaseAuthProvider = firebaseAuthProvider;
  }

  @Override
  public FirebaseAuthService get() {
    return newInstance(firebaseAuthProvider.get());
  }

  public static FirebaseAuthService_Factory create(Provider<FirebaseAuth> firebaseAuthProvider) {
    return new FirebaseAuthService_Factory(firebaseAuthProvider);
  }

  public static FirebaseAuthService newInstance(FirebaseAuth firebaseAuth) {
    return new FirebaseAuthService(firebaseAuth);
  }
}
