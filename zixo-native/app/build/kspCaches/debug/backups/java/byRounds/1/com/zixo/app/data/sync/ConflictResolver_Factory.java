package com.zixo.app.data.sync;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ConflictResolver_Factory implements Factory<ConflictResolver> {
  @Override
  public ConflictResolver get() {
    return newInstance();
  }

  public static ConflictResolver_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ConflictResolver newInstance() {
    return new ConflictResolver();
  }

  private static final class InstanceHolder {
    private static final ConflictResolver_Factory INSTANCE = new ConflictResolver_Factory();
  }
}
