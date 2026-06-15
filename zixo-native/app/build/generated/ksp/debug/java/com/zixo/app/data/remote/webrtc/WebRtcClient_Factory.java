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
public final class WebRtcClient_Factory implements Factory<WebRtcClient> {
  private final Provider<Context> contextProvider;

  public WebRtcClient_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WebRtcClient get() {
    return newInstance(contextProvider.get());
  }

  public static WebRtcClient_Factory create(Provider<Context> contextProvider) {
    return new WebRtcClient_Factory(contextProvider);
  }

  public static WebRtcClient newInstance(Context context) {
    return new WebRtcClient(context);
  }
}
