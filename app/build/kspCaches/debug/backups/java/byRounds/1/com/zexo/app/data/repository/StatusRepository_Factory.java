package com.zexo.app.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
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
public final class StatusRepository_Factory implements Factory<StatusRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public StatusRepository_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public StatusRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static StatusRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider) {
    return new StatusRepository_Factory(firestoreProvider);
  }

  public static StatusRepository newInstance(FirebaseFirestore firestore) {
    return new StatusRepository(firestore);
  }
}
