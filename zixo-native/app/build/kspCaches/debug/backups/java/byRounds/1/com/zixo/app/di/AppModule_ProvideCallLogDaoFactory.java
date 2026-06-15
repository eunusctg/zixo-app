package com.zixo.app.di;

import com.zixo.app.data.local.room.ZixoDatabase;
import com.zixo.app.data.local.room.dao.CallLogDao;
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
public final class AppModule_ProvideCallLogDaoFactory implements Factory<CallLogDao> {
  private final Provider<ZixoDatabase> dbProvider;

  public AppModule_ProvideCallLogDaoFactory(Provider<ZixoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CallLogDao get() {
    return provideCallLogDao(dbProvider.get());
  }

  public static AppModule_ProvideCallLogDaoFactory create(Provider<ZixoDatabase> dbProvider) {
    return new AppModule_ProvideCallLogDaoFactory(dbProvider);
  }

  public static CallLogDao provideCallLogDao(ZixoDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCallLogDao(db));
  }
}
