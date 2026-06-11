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
public final class CallRepository_Factory implements Factory<CallRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> rtdbProvider;

  public CallRepository_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> rtdbProvider) {
    this.firestoreProvider = firestoreProvider;
    this.rtdbProvider = rtdbProvider;
  }

  @Override
  public CallRepository get() {
    return newInstance(firestoreProvider.get(), rtdbProvider.get());
  }

  public static CallRepository_Factory create(
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<FirebaseDatabase> rtdbProvider) {
    return new CallRepository_Factory(Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(rtdbProvider));
  }

  public static CallRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> rtdbProvider) {
    return new CallRepository_Factory(firestoreProvider, rtdbProvider);
  }

  public static CallRepository newInstance(FirebaseFirestore firestore, FirebaseDatabase rtdb) {
    return new CallRepository(firestore, rtdb);
  }
}
