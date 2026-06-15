package com.zixo.app.di;

import com.zixo.app.data.remote.cloudflare.CloudflareApi;
import com.zixo.app.data.remote.cloudflare.CloudflareApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideCloudflareApiServiceFactory implements Factory<CloudflareApiService> {
  private final Provider<CloudflareApi> apiProvider;

  public AppModule_ProvideCloudflareApiServiceFactory(Provider<CloudflareApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public CloudflareApiService get() {
    return provideCloudflareApiService(apiProvider.get());
  }

  public static AppModule_ProvideCloudflareApiServiceFactory create(
      Provider<CloudflareApi> apiProvider) {
    return new AppModule_ProvideCloudflareApiServiceFactory(apiProvider);
  }

  public static CloudflareApiService provideCloudflareApiService(CloudflareApi api) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCloudflareApiService(api));
  }
}
