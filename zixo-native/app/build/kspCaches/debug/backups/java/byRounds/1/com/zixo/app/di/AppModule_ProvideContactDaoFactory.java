package com.zixo.app.di;

import com.zixo.app.data.local.room.ZixoDatabase;
import com.zixo.app.data.local.room.dao.ContactDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AppModule_ProvideContactDaoFactory implements Factory<ContactDao> {
  private final Provider<ZixoDatabase> dbProvider;

  public AppModule_ProvideContactDaoFactory(Provider<ZixoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ContactDao get() {
    return provideContactDao(dbProvider.get());
  }

  public static AppModule_ProvideContactDaoFactory create(Provider<ZixoDatabase> dbProvider) {
    return new AppModule_ProvideContactDaoFactory(dbProvider);
  }

  public static ContactDao provideContactDao(ZixoDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideContactDao(db));
  }
}
