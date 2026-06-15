package com.zixo.app.data.remote.webrtc;

import android.content.Context;
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
public final class ZixoAudioManager_Factory implements Factory<ZixoAudioManager> {
  private final Provider<Context> contextProvider;

  public ZixoAudioManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ZixoAudioManager get() {
    return newInstance(contextProvider.get());
  }

  public static ZixoAudioManager_Factory create(Provider<Context> contextProvider) {
    return new ZixoAudioManager_Factory(contextProvider);
  }

  public static ZixoAudioManager newInstance(Context context) {
    return new ZixoAudioManager(context);
  }
}
