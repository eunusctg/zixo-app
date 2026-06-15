package com.zixo.app;

import com.zixo.app.data.local.PreferencesDataStore;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<PreferencesDataStore> preferencesDataStoreProvider;

  public MainActivity_MembersInjector(Provider<PreferencesDataStore> preferencesDataStoreProvider) {
    this.preferencesDataStoreProvider = preferencesDataStoreProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<PreferencesDataStore> preferencesDataStoreProvider) {
    return new MainActivity_MembersInjector(preferencesDataStoreProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPreferencesDataStore(instance, preferencesDataStoreProvider.get());
  }

  @InjectedFieldSignature("com.zixo.app.MainActivity.preferencesDataStore")
  public static void injectPreferencesDataStore(MainActivity instance,
      PreferencesDataStore preferencesDataStore) {
    instance.preferencesDataStore = preferencesDataStore;
  }
}
