package com.zixo.app.data.repository;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zixo.app.data.remote.webrtc.FirebaseSignalingClient;
import com.zixo.app.data.remote.webrtc.WebRtcClient;
import com.zixo.app.domain.repository.ContactRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class CallRepositoryImpl_Factory implements Factory<CallRepositoryImpl> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> realtimeDbProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<WebRtcClient> webRtcClientProvider;

  private final Provider<FirebaseSignalingClient> signalingClientProvider;

  private final Provider<Context> contextProvider;

  public CallRepositoryImpl_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider, Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<WebRtcClient> webRtcClientProvider,
      Provider<FirebaseSignalingClient> signalingClientProvider,
      Provider<Context> contextProvider) {
    this.firestoreProvider = firestoreProvider;
    this.realtimeDbProvider = realtimeDbProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.webRtcClientProvider = webRtcClientProvider;
    this.signalingClientProvider = signalingClientProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public CallRepositoryImpl get() {
    return newInstance(firestoreProvider.get(), realtimeDbProvider.get(), firebaseAuthProvider.get(), contactRepositoryProvider.get(), webRtcClientProvider.get(), signalingClientProvider.get(), contextProvider.get());
  }

  public static CallRepositoryImpl_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider, Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<WebRtcClient> webRtcClientProvider,
      Provider<FirebaseSignalingClient> signalingClientProvider,
      Provider<Context> contextProvider) {
    return new CallRepositoryImpl_Factory(firestoreProvider, realtimeDbProvider, firebaseAuthProvider, contactRepositoryProvider, webRtcClientProvider, signalingClientProvider, contextProvider);
  }

  public static CallRepositoryImpl newInstance(FirebaseFirestore firestore,
      FirebaseDatabase realtimeDb, FirebaseAuth firebaseAuth, ContactRepository contactRepository,
      WebRtcClient webRtcClient, FirebaseSignalingClient signalingClient, Context context) {
    return new CallRepositoryImpl(firestore, realtimeDb, firebaseAuth, contactRepository, webRtcClient, signalingClient, context);
  }
}
