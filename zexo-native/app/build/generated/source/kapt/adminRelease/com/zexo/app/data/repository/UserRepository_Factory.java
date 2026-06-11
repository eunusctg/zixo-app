package com.zexo.app.data.repository;

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
public final class UserRepository_Factory implements Factory<UserRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public UserRepository_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public UserRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static UserRepository_Factory create(
      javax.inject.Provider<FirebaseFirestore> firestoreProvider) {
    return new UserRepository_Factory(Providers.asDaggerProvider(firestoreProvider));
  }

  public static UserRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider) {
    return new UserRepository_Factory(firestoreProvider);
  }

  public static UserRepository newInstance(FirebaseFirestore firestore) {
    return new UserRepository(firestore);
  }
}
