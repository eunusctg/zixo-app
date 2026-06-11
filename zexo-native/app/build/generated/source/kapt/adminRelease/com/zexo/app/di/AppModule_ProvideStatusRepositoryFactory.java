package com.zexo.app.di;

import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.StatusRepository;
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
public final class AppModule_ProvideStatusRepositoryFactory implements Factory<StatusRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public AppModule_ProvideStatusRepositoryFactory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public StatusRepository get() {
    return provideStatusRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideStatusRepositoryFactory create(
      javax.inject.Provider<FirebaseFirestore> firestoreProvider) {
    return new AppModule_ProvideStatusRepositoryFactory(Providers.asDaggerProvider(firestoreProvider));
  }

  public static AppModule_ProvideStatusRepositoryFactory create(
      Provider<FirebaseFirestore> firestoreProvider) {
    return new AppModule_ProvideStatusRepositoryFactory(firestoreProvider);
  }

  public static StatusRepository provideStatusRepository(FirebaseFirestore firestore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideStatusRepository(firestore));
  }
}
