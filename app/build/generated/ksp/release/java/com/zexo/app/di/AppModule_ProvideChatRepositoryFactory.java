package com.zexo.app.di;

import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.ChatRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideChatRepositoryFactory implements Factory<ChatRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public AppModule_ProvideChatRepositoryFactory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public ChatRepository get() {
    return provideChatRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideChatRepositoryFactory create(
      Provider<FirebaseFirestore> firestoreProvider) {
    return new AppModule_ProvideChatRepositoryFactory(firestoreProvider);
  }

  public static ChatRepository provideChatRepository(FirebaseFirestore firestore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatRepository(firestore));
  }
}
