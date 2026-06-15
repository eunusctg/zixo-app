package com.zixo.app.data.remote.cloudflare;

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
public final class CloudflareApiService_Factory implements Factory<CloudflareApiService> {
  private final Provider<CloudflareApi> apiProvider;

  public CloudflareApiService_Factory(Provider<CloudflareApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public CloudflareApiService get() {
    return newInstance(apiProvider.get());
  }

  public static CloudflareApiService_Factory create(Provider<CloudflareApi> apiProvider) {
    return new CloudflareApiService_Factory(apiProvider);
  }

  public static CloudflareApiService newInstance(CloudflareApi api) {
    return new CloudflareApiService(api);
  }
}
