package com.zixo.app.di;

import com.zixo.app.data.local.room.ZixoDatabase;
import com.zixo.app.data.local.room.dao.ChatDao;
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
public final class AppModule_ProvideChatDaoFactory implements Factory<ChatDao> {
  private final Provider<ZixoDatabase> dbProvider;

  public AppModule_ProvideChatDaoFactory(Provider<ZixoDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ChatDao get() {
    return provideChatDao(dbProvider.get());
  }

  public static AppModule_ProvideChatDaoFactory create(Provider<ZixoDatabase> dbProvider) {
    return new AppModule_ProvideChatDaoFactory(dbProvider);
  }

  public static ChatDao provideChatDao(ZixoDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatDao(db));
  }
}
