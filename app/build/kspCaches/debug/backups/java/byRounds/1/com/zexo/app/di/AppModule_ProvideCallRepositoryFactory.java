package com.zexo.app.di;

import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.CallRepository;
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
public final class AppModule_ProvideCallRepositoryFactory implements Factory<CallRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public AppModule_ProvideCallRepositoryFactory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public CallRepository get() {
    return provideCallRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideCallRepositoryFactory create(
      Provider<FirebaseFirestore> firestoreProvider) {
    return new AppModule_ProvideCallRepositoryFactory(firestoreProvider);
  }

  public static CallRepository provideCallRepository(FirebaseFirestore firestore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCallRepository(firestore));
  }
}
