package com.zixo.app.di;

import com.zixo.app.data.repository.ContactRepositoryImpl;
import com.zixo.app.domain.repository.ContactRepository;
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
public final class AppModule_ProvideContactRepositoryFactory implements Factory<ContactRepository> {
  private final Provider<ContactRepositoryImpl> implProvider;

  public AppModule_ProvideContactRepositoryFactory(Provider<ContactRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public ContactRepository get() {
    return provideContactRepository(implProvider.get());
  }

  public static AppModule_ProvideContactRepositoryFactory create(
      Provider<ContactRepositoryImpl> implProvider) {
    return new AppModule_ProvideContactRepositoryFactory(implProvider);
  }

  public static ContactRepository provideContactRepository(ContactRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideContactRepository(impl));
  }
}
