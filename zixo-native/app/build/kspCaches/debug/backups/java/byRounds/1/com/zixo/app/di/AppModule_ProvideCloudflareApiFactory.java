package com.zixo.app.di;

import com.zixo.app.data.remote.cloudflare.CloudflareApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class AppModule_ProvideCloudflareApiFactory implements Factory<CloudflareApi> {
  private final Provider<Retrofit> retrofitProvider;

  public AppModule_ProvideCloudflareApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public CloudflareApi get() {
    return provideCloudflareApi(retrofitProvider.get());
  }

  public static AppModule_ProvideCloudflareApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new AppModule_ProvideCloudflareApiFactory(retrofitProvider);
  }

  public static CloudflareApi provideCloudflareApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCloudflareApi(retrofit));
  }
}
