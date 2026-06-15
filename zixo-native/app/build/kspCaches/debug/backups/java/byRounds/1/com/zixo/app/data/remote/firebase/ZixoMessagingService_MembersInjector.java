package com.zixo.app.data.remote.firebase;

import android.content.SharedPreferences;
import com.google.firebase.firestore.FirebaseFirestore;
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
public final class ZixoMessagingService_MembersInjector implements MembersInjector<ZixoMessagingService> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<SharedPreferences> sharedPrefsProvider;

  public ZixoMessagingService_MembersInjector(Provider<FirebaseFirestore> firestoreProvider,
      Provider<SharedPreferences> sharedPrefsProvider) {
    this.firestoreProvider = firestoreProvider;
    this.sharedPrefsProvider = sharedPrefsProvider;
  }

  public static MembersInjector<ZixoMessagingService> create(
      Provider<FirebaseFirestore> firestoreProvider,
      Provider<SharedPreferences> sharedPrefsProvider) {
    return new ZixoMessagingService_MembersInjector(firestoreProvider, sharedPrefsProvider);
  }

  @Override
  public void injectMembers(ZixoMessagingService instance) {
    injectFirestore(instance, firestoreProvider.get());
    injectSharedPrefs(instance, sharedPrefsProvider.get());
  }

  @InjectedFieldSignature("com.zixo.app.data.remote.firebase.ZixoMessagingService.firestore")
  public static void injectFirestore(ZixoMessagingService instance, FirebaseFirestore firestore) {
    instance.firestore = firestore;
  }

  @InjectedFieldSignature("com.zixo.app.data.remote.firebase.ZixoMessagingService.sharedPrefs")
  public static void injectSharedPrefs(ZixoMessagingService instance,
      SharedPreferences sharedPrefs) {
    instance.sharedPrefs = sharedPrefs;
  }
}
