package com.zexo.app.ui.screens.settings;

import android.content.Context;
import com.zexo.app.data.local.PreferencesManager;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.UserRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<Context> contextProvider;

  public SettingsViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider, Provider<Context> contextProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(authRepositoryProvider.get(), userRepositoryProvider.get(), preferencesManagerProvider.get(), contextProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider, Provider<Context> contextProvider) {
    return new SettingsViewModel_Factory(authRepositoryProvider, userRepositoryProvider, preferencesManagerProvider, contextProvider);
  }

  public static SettingsViewModel newInstance(AuthRepository authRepository,
      UserRepository userRepository, PreferencesManager preferencesManager, Context context) {
    return new SettingsViewModel(authRepository, userRepository, preferencesManager, context);
  }
}
