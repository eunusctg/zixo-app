package com.zexo.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> rtdbProvider;

  public AuthRepository_Factory(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseDatabase> rtdbProvider) {
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
    this.rtdbProvider = rtdbProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(authProvider.get(), firestoreProvider.get(), rtdbProvider.get());
  }

  public static AuthRepository_Factory create(javax.inject.Provider<FirebaseAuth> authProvider,
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<FirebaseDatabase> rtdbProvider) {
    return new AuthRepository_Factory(Providers.asDaggerProvider(authProvider), Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(rtdbProvider));
  }

  public static AuthRepository_Factory create(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseDatabase> rtdbProvider) {
    return new AuthRepository_Factory(authProvider, firestoreProvider, rtdbProvider);
  }

  public static AuthRepository newInstance(FirebaseAuth auth, FirebaseFirestore firestore,
      FirebaseDatabase rtdb) {
    return new AuthRepository(auth, firestore, rtdb);
  }
}
