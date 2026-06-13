package com.zexo.app.data.repository;

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
public final class StorageRepository_Factory implements Factory<StorageRepository> {
  private final Provider<FirebaseStorage> storageProvider;

  public StorageRepository_Factory(Provider<FirebaseStorage> storageProvider) {
    this.storageProvider = storageProvider;
  }

  @Override
  public StorageRepository get() {
    return newInstance(storageProvider.get());
  }

  public static StorageRepository_Factory create(Provider<FirebaseStorage> storageProvider) {
    return new StorageRepository_Factory(storageProvider);
  }

  public static StorageRepository newInstance(FirebaseStorage storage) {
    return new StorageRepository(storage);
  }
}
