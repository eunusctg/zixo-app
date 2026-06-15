package com.zixo.app.data.remote.webrtc;

import com.google.firebase.database.FirebaseDatabase;
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
public final class FirebaseSignalingClient_Factory implements Factory<FirebaseSignalingClient> {
  private final Provider<FirebaseDatabase> realtimeDbProvider;

  public FirebaseSignalingClient_Factory(Provider<FirebaseDatabase> realtimeDbProvider) {
    this.realtimeDbProvider = realtimeDbProvider;
  }

  @Override
  public FirebaseSignalingClient get() {
    return newInstance(realtimeDbProvider.get());
  }

  public static FirebaseSignalingClient_Factory create(
      Provider<FirebaseDatabase> realtimeDbProvider) {
    return new FirebaseSignalingClient_Factory(realtimeDbProvider);
  }

  public static FirebaseSignalingClient newInstance(FirebaseDatabase realtimeDb) {
    return new FirebaseSignalingClient(realtimeDb);
  }
}
