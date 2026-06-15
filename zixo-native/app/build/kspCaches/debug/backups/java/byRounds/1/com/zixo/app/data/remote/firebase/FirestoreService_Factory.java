package com.zixo.app.data.remote.firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
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
public final class FirestoreService_Factory implements Factory<FirestoreService> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseStorage> storageProvider;

  public FirestoreService_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseStorage> storageProvider) {
    this.firestoreProvider = firestoreProvider;
    this.storageProvider = storageProvider;
  }

  @Override
  public FirestoreService get() {
    return newInstance(firestoreProvider.get(), storageProvider.get());
  }

  public static FirestoreService_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseStorage> storageProvider) {
    return new FirestoreService_Factory(firestoreProvider, storageProvider);
  }

  public static FirestoreService newInstance(FirebaseFirestore firestore, FirebaseStorage storage) {
    return new FirestoreService(firestore, storage);
  }
}
