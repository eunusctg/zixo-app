package com.zexo.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
import com.zexo.app.data.repository.ChatRepository;
import com.zexo.app.data.repository.StatusRepository;
import com.zexo.app.data.repository.UserRepository;
import com.zexo.app.di.AppModule_ProvideAuthRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideCallRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideChatRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideFirebaseAuthFactory;
import com.zexo.app.di.AppModule_ProvideFirestoreFactory;
import com.zexo.app.di.AppModule_ProvideRtdbFactory;
import com.zexo.app.di.AppModule_ProvideStatusRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideUserRepositoryFactory;
import com.zexo.app.ui.screens.admin.AdminViewModel;
import com.zexo.app.ui.screens.admin.AdminViewModel_HiltModules;
import com.zexo.app.ui.screens.admin.AdminViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.admin.AdminViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.auth.AuthViewModel;
import com.zexo.app.ui.screens.auth.AuthViewModel_HiltModules;
import com.zexo.app.ui.screens.auth.AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.auth.AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.calls.CallActivity;
import com.zexo.app.ui.screens.calls.CallViewModel;
import com.zexo.app.ui.screens.calls.CallViewModel_HiltModules;
import com.zexo.app.ui.screens.calls.CallViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.calls.CallViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.calls.DialPadViewModel;
import com.zexo.app.ui.screens.calls.DialPadViewModel_HiltModules;
import com.zexo.app.ui.screens.calls.DialPadViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.calls.DialPadViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.chat.ChatViewModel;
import com.zexo.app.ui.screens.chat.ChatViewModel_HiltModules;
import com.zexo.app.ui.screens.chat.ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.chat.ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.contacts.NewChatViewModel;
import com.zexo.app.ui.screens.contacts.NewChatViewModel_HiltModules;
import com.zexo.app.ui.screens.contacts.NewChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.contacts.NewChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.contacts.QRScannerViewModel;
import com.zexo.app.ui.screens.contacts.QRScannerViewModel_HiltModules;
import com.zexo.app.ui.screens.contacts.QRScannerViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.contacts.QRScannerViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.home.HomeViewModel;
import com.zexo.app.ui.screens.home.HomeViewModel_HiltModules;
import com.zexo.app.ui.screens.home.HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.home.HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.profile.ProfileEditViewModel;
import com.zexo.app.ui.screens.profile.ProfileEditViewModel_HiltModules;
import com.zexo.app.ui.screens.profile.ProfileEditViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.profile.ProfileEditViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.profile.ProfileViewModel;
import com.zexo.app.ui.screens.profile.ProfileViewModel_HiltModules;
import com.zexo.app.ui.screens.profile.ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.profile.ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.settings.SettingsViewModel;
import com.zexo.app.ui.screens.settings.SettingsViewModel_HiltModules;
import com.zexo.app.ui.screens.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.splash.SplashViewModel;
import com.zexo.app.ui.screens.splash.SplashViewModel_HiltModules;
import com.zexo.app.ui.screens.splash.SplashViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.splash.SplashViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.status.NewStatusViewModel;
import com.zexo.app.ui.screens.status.NewStatusViewModel_HiltModules;
import com.zexo.app.ui.screens.status.NewStatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.status.NewStatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.status.StatusViewViewModel;
import com.zexo.app.ui.screens.status.StatusViewViewModel_HiltModules;
import com.zexo.app.ui.screens.status.StatusViewViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.status.StatusViewViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerZexoApp_HiltComponents_SingletonC {
  private DaggerZexoApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static ZexoApp_HiltComponents.SingletonC create() {
    return new Builder().build();
  }

  public static final class Builder {
    private Builder() {
    }

    /**
     * @deprecated This module is declared, but an instance is not used in the component. This method is a no-op. For more, see https://dagger.dev/unused-modules.
     */
    @Deprecated
    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public ZexoApp_HiltComponents.SingletonC build() {
      return new SingletonCImpl();
    }
  }

  private static final class ActivityRetainedCBuilder implements ZexoApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public ZexoApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements ZexoApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public ZexoApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements ZexoApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public ZexoApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements ZexoApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public ZexoApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements ZexoApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public ZexoApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements ZexoApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public ZexoApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements ZexoApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public ZexoApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends ZexoApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends ZexoApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends ZexoApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends ZexoApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
    }

    @Override
    public void injectCallActivity(CallActivity arg0) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(14).put(AdminViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AdminViewModel_HiltModules.KeyModule.provide()).put(AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AuthViewModel_HiltModules.KeyModule.provide()).put(CallViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CallViewModel_HiltModules.KeyModule.provide()).put(ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChatViewModel_HiltModules.KeyModule.provide()).put(DialPadViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DialPadViewModel_HiltModules.KeyModule.provide()).put(HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HomeViewModel_HiltModules.KeyModule.provide()).put(NewChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, NewChatViewModel_HiltModules.KeyModule.provide()).put(NewStatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, NewStatusViewModel_HiltModules.KeyModule.provide()).put(ProfileEditViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ProfileEditViewModel_HiltModules.KeyModule.provide()).put(ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ProfileViewModel_HiltModules.KeyModule.provide()).put(QRScannerViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, QRScannerViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(SplashViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SplashViewModel_HiltModules.KeyModule.provide()).put(StatusViewViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, StatusViewViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends ZexoApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AdminViewModel> adminViewModelProvider;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<CallViewModel> callViewModelProvider;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<DialPadViewModel> dialPadViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<NewChatViewModel> newChatViewModelProvider;

    private Provider<NewStatusViewModel> newStatusViewModelProvider;

    private Provider<ProfileEditViewModel> profileEditViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<QRScannerViewModel> qRScannerViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private Provider<StatusViewViewModel> statusViewViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.adminViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.callViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.dialPadViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.newChatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.newStatusViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.profileEditViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.qRScannerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.statusViewViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(14).put(AdminViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) adminViewModelProvider)).put(AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) authViewModelProvider)).put(CallViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) callViewModelProvider)).put(ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) chatViewModelProvider)).put(DialPadViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) dialPadViewModelProvider)).put(HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) homeViewModelProvider)).put(NewChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) newChatViewModelProvider)).put(NewStatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) newStatusViewModelProvider)).put(ProfileEditViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) profileEditViewModelProvider)).put(ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) profileViewModelProvider)).put(QRScannerViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) qRScannerViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(SplashViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) splashViewModelProvider)).put(StatusViewViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) statusViewViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.zexo.app.ui.screens.admin.AdminViewModel 
          return (T) new AdminViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideFirestoreProvider.get());

          case 1: // com.zexo.app.ui.screens.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 2: // com.zexo.app.ui.screens.calls.CallViewModel 
          return (T) new CallViewModel(singletonCImpl.provideCallRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideRtdbProvider.get());

          case 3: // com.zexo.app.ui.screens.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get());

          case 4: // com.zexo.app.ui.screens.calls.DialPadViewModel 
          return (T) new DialPadViewModel(singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get());

          case 5: // com.zexo.app.ui.screens.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideCallRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideStatusRepositoryProvider.get());

          case 6: // com.zexo.app.ui.screens.contacts.NewChatViewModel 
          return (T) new NewChatViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          case 7: // com.zexo.app.ui.screens.status.NewStatusViewModel 
          return (T) new NewStatusViewModel(singletonCImpl.provideStatusRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get());

          case 8: // com.zexo.app.ui.screens.profile.ProfileEditViewModel 
          return (T) new ProfileEditViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 9: // com.zexo.app.ui.screens.profile.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get());

          case 10: // com.zexo.app.ui.screens.contacts.QRScannerViewModel 
          return (T) new QRScannerViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          case 11: // com.zexo.app.ui.screens.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideFirestoreProvider.get());

          case 12: // com.zexo.app.ui.screens.splash.SplashViewModel 
          return (T) new SplashViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 13: // com.zexo.app.ui.screens.status.StatusViewViewModel 
          return (T) new StatusViewViewModel(singletonCImpl.provideStatusRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends ZexoApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends ZexoApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends ZexoApp_HiltComponents.SingletonC {
    private final SingletonCImpl singletonCImpl = this;

    private Provider<FirebaseAuth> provideFirebaseAuthProvider;

    private Provider<FirebaseFirestore> provideFirestoreProvider;

    private Provider<FirebaseDatabase> provideRtdbProvider;

    private Provider<AuthRepository> provideAuthRepositoryProvider;

    private Provider<UserRepository> provideUserRepositoryProvider;

    private Provider<CallRepository> provideCallRepositoryProvider;

    private Provider<ChatRepository> provideChatRepositoryProvider;

    private Provider<StatusRepository> provideStatusRepositoryProvider;

    private SingletonCImpl() {

      initialize();

    }

    @SuppressWarnings("unchecked")
    private void initialize() {
      this.provideFirebaseAuthProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuth>(singletonCImpl, 1));
      this.provideFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseFirestore>(singletonCImpl, 2));
      this.provideRtdbProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseDatabase>(singletonCImpl, 3));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 0));
      this.provideUserRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserRepository>(singletonCImpl, 4));
      this.provideCallRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CallRepository>(singletonCImpl, 5));
      this.provideChatRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepository>(singletonCImpl, 6));
      this.provideStatusRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<StatusRepository>(singletonCImpl, 7));
    }

    @Override
    public void injectZexoApp(ZexoApp zexoApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.zexo.app.data.repository.AuthRepository 
          return (T) AppModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.provideFirestoreProvider.get(), singletonCImpl.provideRtdbProvider.get());

          case 1: // com.google.firebase.auth.FirebaseAuth 
          return (T) AppModule_ProvideFirebaseAuthFactory.provideFirebaseAuth();

          case 2: // com.google.firebase.firestore.FirebaseFirestore 
          return (T) AppModule_ProvideFirestoreFactory.provideFirestore();

          case 3: // com.google.firebase.database.FirebaseDatabase 
          return (T) AppModule_ProvideRtdbFactory.provideRtdb();

          case 4: // com.zexo.app.data.repository.UserRepository 
          return (T) AppModule_ProvideUserRepositoryFactory.provideUserRepository(singletonCImpl.provideFirestoreProvider.get());

          case 5: // com.zexo.app.data.repository.CallRepository 
          return (T) AppModule_ProvideCallRepositoryFactory.provideCallRepository(singletonCImpl.provideFirestoreProvider.get(), singletonCImpl.provideRtdbProvider.get());

          case 6: // com.zexo.app.data.repository.ChatRepository 
          return (T) AppModule_ProvideChatRepositoryFactory.provideChatRepository(singletonCImpl.provideFirestoreProvider.get(), singletonCImpl.provideRtdbProvider.get());

          case 7: // com.zexo.app.data.repository.StatusRepository 
          return (T) AppModule_ProvideStatusRepositoryFactory.provideStatusRepository(singletonCImpl.provideFirestoreProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
