package com.zexo.app.data.repository;

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
public final class ChatRepository_Factory implements Factory<ChatRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> rtdbProvider;

  public ChatRepository_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> rtdbProvider) {
    this.firestoreProvider = firestoreProvider;
    this.rtdbProvider = rtdbProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(firestoreProvider.get(), rtdbProvider.get());
  }

  public static ChatRepository_Factory create(
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<FirebaseDatabase> rtdbProvider) {
    return new ChatRepository_Factory(Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(rtdbProvider));
  }

  public static ChatRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> rtdbProvider) {
    return new ChatRepository_Factory(firestoreProvider, rtdbProvider);
  }

  public static ChatRepository newInstance(FirebaseFirestore firestore, FirebaseDatabase rtdb) {
    return new ChatRepository(firestore, rtdb);
  }
}
