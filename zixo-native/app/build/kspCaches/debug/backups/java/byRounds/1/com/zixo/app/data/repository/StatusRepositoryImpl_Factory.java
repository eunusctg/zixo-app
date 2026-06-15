package com.zixo.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.zixo.app.domain.repository.ContactRepository;
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
public final class StatusRepositoryImpl_Factory implements Factory<StatusRepositoryImpl> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseStorage> storageProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public StatusRepositoryImpl_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseStorage> storageProvider, Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.firestoreProvider = firestoreProvider;
    this.storageProvider = storageProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public StatusRepositoryImpl get() {
    return newInstance(firestoreProvider.get(), storageProvider.get(), firebaseAuthProvider.get(), contactRepositoryProvider.get());
  }

  public static StatusRepositoryImpl_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseStorage> storageProvider, Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new StatusRepositoryImpl_Factory(firestoreProvider, storageProvider, firebaseAuthProvider, contactRepositoryProvider);
  }

  public static StatusRepositoryImpl newInstance(FirebaseFirestore firestore,
      FirebaseStorage storage, FirebaseAuth firebaseAuth, ContactRepository contactRepository) {
    return new StatusRepositoryImpl(firestore, storage, firebaseAuth, contactRepository);
  }
}
