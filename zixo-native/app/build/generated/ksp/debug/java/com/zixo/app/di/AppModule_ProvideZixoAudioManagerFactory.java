package com.zixo.app.di;

import android.content.Context;
import com.zixo.app.data.remote.webrtc.ZixoAudioManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideZixoAudioManagerFactory implements Factory<ZixoAudioManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideZixoAudioManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ZixoAudioManager get() {
    return provideZixoAudioManager(contextProvider.get());
  }

  public static AppModule_ProvideZixoAudioManagerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideZixoAudioManagerFactory(contextProvider);
  }

  public static ZixoAudioManager provideZixoAudioManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideZixoAudioManager(context));
  }
}
