package com.zixo.app.di;

import com.zixo.app.data.remote.webrtc.PeerConnectionObserver;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvidePeerConnectionObserverFactory implements Factory<PeerConnectionObserver> {
  @Override
  public PeerConnectionObserver get() {
    return providePeerConnectionObserver();
  }

  public static AppModule_ProvidePeerConnectionObserverFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PeerConnectionObserver providePeerConnectionObserver() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePeerConnectionObserver());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvidePeerConnectionObserverFactory INSTANCE = new AppModule_ProvidePeerConnectionObserverFactory();
  }
}
