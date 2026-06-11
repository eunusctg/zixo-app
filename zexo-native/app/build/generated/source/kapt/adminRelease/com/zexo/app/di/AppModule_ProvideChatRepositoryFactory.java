package com.zexo.app.di;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.ChatRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideChatRepositoryFactory implements Factory<ChatRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> rtdbProvider;

  public AppModule_ProvideChatRepositoryFactory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> rtdbProvider) {
    this.firestoreProvider = firestoreProvider;
    this.rtdbProvider = rtdbProvider;
  }

  @Override
  public ChatRepository get() {
    return provideChatRepository(firestoreProvider.get(), rtdbProvider.get());
  }

  public static AppModule_ProvideChatRepositoryFactory create(
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<FirebaseDatabase> rtdbProvider) {
    return new AppModule_ProvideChatRepositoryFactory(Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(rtdbProvider));
  }

  public static AppModule_ProvideChatRepositoryFactory create(
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseDatabase> rtdbProvider) {
    return new AppModule_ProvideChatRepositoryFactory(firestoreProvider, rtdbProvider);
  }

  public static ChatRepository provideChatRepository(FirebaseFirestore firestore,
      FirebaseDatabase rtdb) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatRepository(firestore, rtdb));
  }
}
