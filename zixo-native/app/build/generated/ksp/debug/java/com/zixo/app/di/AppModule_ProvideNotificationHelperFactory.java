package com.zixo.app.di;

import android.content.Context;
import com.zixo.app.ui.components.NotificationHelper;
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
public final class AppModule_ProvideNotificationHelperFactory implements Factory<NotificationHelper> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideNotificationHelperFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationHelper get() {
    return provideNotificationHelper(contextProvider.get());
  }

  public static AppModule_ProvideNotificationHelperFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideNotificationHelperFactory(contextProvider);
  }

  public static NotificationHelper provideNotificationHelper(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNotificationHelper(context));
  }
}
