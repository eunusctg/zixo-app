package com.zexo.app.di;

import com.google.firebase.storage.FirebaseStorage;
import com.zexo.app.data.repository.StorageRepository;
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
public final class AppModule_ProvideStorageRepositoryFactory implements Factory<StorageRepository> {
  private final Provider<FirebaseStorage> storageProvider;

  public AppModule_ProvideStorageRepositoryFactory(Provider<FirebaseStorage> storageProvider) {
    this.storageProvider = storageProvider;
  }

  @Override
  public StorageRepository get() {
    return provideStorageRepository(storageProvider.get());
  }

  public static AppModule_ProvideStorageRepositoryFactory create(
      Provider<FirebaseStorage> storageProvider) {
    return new AppModule_ProvideStorageRepositoryFactory(storageProvider);
  }

  public static StorageRepository provideStorageRepository(FirebaseStorage storage) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideStorageRepository(storage));
  }
}
