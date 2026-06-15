package com.zixo.app.di;

import android.content.Context;
import com.zixo.app.data.local.room.ZixoDatabase;
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
public final class AppModule_ProvideZixoDatabaseFactory implements Factory<ZixoDatabase> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideZixoDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ZixoDatabase get() {
    return provideZixoDatabase(contextProvider.get());
  }

  public static AppModule_ProvideZixoDatabaseFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideZixoDatabaseFactory(contextProvider);
  }

  public static ZixoDatabase provideZixoDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideZixoDatabase(context));
  }
}
