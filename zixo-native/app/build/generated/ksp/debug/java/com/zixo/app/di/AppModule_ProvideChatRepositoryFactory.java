package com.zixo.app.di;

import com.zixo.app.data.repository.ChatRepositoryImpl;
import com.zixo.app.domain.repository.ChatRepository;
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
public final class AppModule_ProvideChatRepositoryFactory implements Factory<ChatRepository> {
  private final Provider<ChatRepositoryImpl> implProvider;

  public AppModule_ProvideChatRepositoryFactory(Provider<ChatRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public ChatRepository get() {
    return provideChatRepository(implProvider.get());
  }

  public static AppModule_ProvideChatRepositoryFactory create(
      Provider<ChatRepositoryImpl> implProvider) {
    return new AppModule_ProvideChatRepositoryFactory(implProvider);
  }

  public static ChatRepository provideChatRepository(ChatRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatRepository(impl));
  }
}
