package com.zixo.app.di;

import com.google.firebase.database.FirebaseDatabase;
import com.zixo.app.data.remote.webrtc.FirebaseSignalingClient;
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
public final class AppModule_ProvideFirebaseSignalingClientFactory implements Factory<FirebaseSignalingClient> {
  private final Provider<FirebaseDatabase> firebaseDatabaseProvider;

  public AppModule_ProvideFirebaseSignalingClientFactory(
      Provider<FirebaseDatabase> firebaseDatabaseProvider) {
    this.firebaseDatabaseProvider = firebaseDatabaseProvider;
  }

  @Override
  public FirebaseSignalingClient get() {
    return provideFirebaseSignalingClient(firebaseDatabaseProvider.get());
  }

  public static AppModule_ProvideFirebaseSignalingClientFactory create(
      Provider<FirebaseDatabase> firebaseDatabaseProvider) {
    return new AppModule_ProvideFirebaseSignalingClientFactory(firebaseDatabaseProvider);
  }

  public static FirebaseSignalingClient provideFirebaseSignalingClient(
      FirebaseDatabase firebaseDatabase) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFirebaseSignalingClient(firebaseDatabase));
  }
}
