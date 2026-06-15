package com.zixo.app.di;

import com.zixo.app.data.local.room.ZixoDatabase;
import com.zixo.app.data.local.room.dao.UserDao;
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
public final class AppModule_ProvideUserDaoFactory implements Factory<UserDao> {
  private final Provider<ZixoDatabase> dbProvider;

  public AppModule_ProvideUserDaoFactory(Provider<ZixoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public UserDao get() {
    return provideUserDao(dbProvider.get());
  }

  public static AppModule_ProvideUserDaoFactory create(Provider<ZixoDatabase> dbProvider) {
    return new AppModule_ProvideUserDaoFactory(dbProvider);
  }

  public static UserDao provideUserDao(ZixoDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUserDao(db));
  }
}
