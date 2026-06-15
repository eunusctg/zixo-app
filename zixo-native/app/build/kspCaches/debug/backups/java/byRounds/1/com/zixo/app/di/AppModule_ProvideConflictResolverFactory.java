package com.zixo.app.di;

import com.zixo.app.data.sync.ConflictResolver;
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
public final class AppModule_ProvideConflictResolverFactory implements Factory<ConflictResolver> {
  @Override
  public ConflictResolver get() {
    return provideConflictResolver();
  }

  public static AppModule_ProvideConflictResolverFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ConflictResolver provideConflictResolver() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideConflictResolver());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideConflictResolverFactory INSTANCE = new AppModule_ProvideConflictResolverFactory();
  }
}
