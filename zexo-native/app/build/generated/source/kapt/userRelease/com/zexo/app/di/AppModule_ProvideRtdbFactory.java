package com.zexo.app.di;

import com.google.firebase.database.FirebaseDatabase;
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
public final class AppModule_ProvideRtdbFactory implements Factory<FirebaseDatabase> {
  @Override
  public FirebaseDatabase get() {
    return provideRtdb();
  }

  public static AppModule_ProvideRtdbFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FirebaseDatabase provideRtdb() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRtdb());
  }

  private static final class InstanceHolder {
    static final AppModule_ProvideRtdbFactory INSTANCE = new AppModule_ProvideRtdbFactory();
  }
}
