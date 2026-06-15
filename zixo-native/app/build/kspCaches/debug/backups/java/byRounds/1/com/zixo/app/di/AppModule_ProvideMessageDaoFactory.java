package com.zixo.app.di;

import com.zixo.app.data.local.room.ZixoDatabase;
import com.zixo.app.data.local.room.dao.MessageDao;
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
public final class AppModule_ProvideMessageDaoFactory implements Factory<MessageDao> {
  private final Provider<ZixoDatabase> dbProvider;

  public AppModule_ProvideMessageDaoFactory(Provider<ZixoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public MessageDao get() {
    return provideMessageDao(dbProvider.get());
  }

  public static AppModule_ProvideMessageDaoFactory create(Provider<ZixoDatabase> dbProvider) {
    return new AppModule_ProvideMessageDaoFactory(dbProvider);
  }

  public static MessageDao provideMessageDao(ZixoDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageDao(db));
  }
}
