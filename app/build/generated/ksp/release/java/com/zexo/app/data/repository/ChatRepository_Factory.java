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
public final class ChatRepository_Factory implements Factory<ChatRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public ChatRepository_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static ChatRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider) {
    return new ChatRepository_Factory(firestoreProvider);
  }

  public static ChatRepository newInstance(FirebaseFirestore firestore) {
    return new ChatRepository(firestore);
  }
}
