package com.zixo.app.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
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
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> realtimeDbProvider;

  private final Provider<FirebaseStorage> storageProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public ChatRepositoryImpl_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider, Provider<FirebaseStorage> storageProvider,
      Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.firestoreProvider = firestoreProvider;
    this.realtimeDbProvider = realtimeDbProvider;
    this.storageProvider = storageProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(firestoreProvider.get(), realtimeDbProvider.get(), storageProvider.get(), firebaseAuthProvider.get(), contactRepositoryProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider, Provider<FirebaseStorage> storageProvider,
      Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new ChatRepositoryImpl_Factory(firestoreProvider, realtimeDbProvider, storageProvider, firebaseAuthProvider, contactRepositoryProvider);
  }

  public static ChatRepositoryImpl newInstance(FirebaseFirestore firestore,
      FirebaseDatabase realtimeDb, FirebaseStorage storage, FirebaseAuth firebaseAuth,
      ContactRepository contactRepository) {
    return new ChatRepositoryImpl(firestore, realtimeDb, storage, firebaseAuth, contactRepository);
  }
}
