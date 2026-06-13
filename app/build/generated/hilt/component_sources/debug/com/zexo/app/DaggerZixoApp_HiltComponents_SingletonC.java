package com.zexo.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.zexo.app.data.local.PreferencesManager;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
import com.zexo.app.data.repository.ChatRepository;
import com.zexo.app.data.repository.StatusRepository;
import com.zexo.app.data.repository.StorageRepository;
import com.zexo.app.data.repository.UserRepository;
import com.zexo.app.di.AppModule_ProvideAuthRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideCallRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideChatRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideFirebaseAuthFactory;
import com.zexo.app.di.AppModule_ProvideFirebaseStorageFactory;
import com.zexo.app.di.AppModule_ProvideFirestoreFactory;
import com.zexo.app.di.AppModule_ProvidePreferencesManagerFactory;
import com.zexo.app.di.AppModule_ProvideStatusRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideStorageRepositoryFactory;
import com.zexo.app.di.AppModule_ProvideUserRepositoryFactory;
import com.zexo.app.ui.screens.auth.AuthViewModel;
import com.zexo.app.ui.screens.auth.AuthViewModel_HiltModules;
import com.zexo.app.ui.screens.auth.AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.auth.AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.calls.CallsViewModel;
import com.zexo.app.ui.screens.calls.CallsViewModel_HiltModules;
import com.zexo.app.ui.screens.calls.CallsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.calls.CallsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.chat.ChatViewModel;
import com.zexo.app.ui.screens.chat.ChatViewModel_HiltModules;
import com.zexo.app.ui.screens.chat.ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.chat.ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.chat.ForwardViewModel;
import com.zexo.app.ui.screens.chat.ForwardViewModel_HiltModules;
import com.zexo.app.ui.screens.chat.ForwardViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.chat.ForwardViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.contacts.NewChatViewModel;
import com.zexo.app.ui.screens.contacts.NewChatViewModel_HiltModules;
import com.zexo.app.ui.screens.contacts.NewChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.contacts.NewChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.home.HomeViewModel;
import com.zexo.app.ui.screens.home.HomeViewModel_HiltModules;
import com.zexo.app.ui.screens.home.HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.home.HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.profile.EditProfileViewModel;
import com.zexo.app.ui.screens.profile.EditProfileViewModel_HiltModules;
import com.zexo.app.ui.screens.profile.EditProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.profile.EditProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.settings.SettingsViewModel;
import com.zexo.app.ui.screens.settings.SettingsViewModel_HiltModules;
import com.zexo.app.ui.screens.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zexo.app.ui.screens.status.StatusViewModel;
import com.zexo.app.ui.screens.status.StatusViewModel_HiltModules;
import com.zexo.app.ui.screens.status.StatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zexo.app.ui.screens.status.StatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
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
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
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
public final class DaggerZixoApp_HiltComponents_SingletonC {
  private DaggerZixoApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public ZixoApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements ZixoApp_HiltComponents.ActivityRetainedC.Builder {
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
    public ZixoApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements ZixoApp_HiltComponents.ActivityC.Builder {
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
    public ZixoApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements ZixoApp_HiltComponents.FragmentC.Builder {
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
    public ZixoApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements ZixoApp_HiltComponents.ViewWithFragmentC.Builder {
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
    public ZixoApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements ZixoApp_HiltComponents.ViewC.Builder {
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
    public ZixoApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements ZixoApp_HiltComponents.ViewModelC.Builder {
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
    public ZixoApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements ZixoApp_HiltComponents.ServiceC.Builder {
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
    public ZixoApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends ZixoApp_HiltComponents.ViewWithFragmentC {
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

  private static final class FragmentCImpl extends ZixoApp_HiltComponents.FragmentC {
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

  private static final class ViewCImpl extends ZixoApp_HiltComponents.ViewC {
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

  private static final class ActivityCImpl extends ZixoApp_HiltComponents.ActivityC {
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
      injectMainActivity2(arg0);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(9).put(AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AuthViewModel_HiltModules.KeyModule.provide()).put(CallsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CallsViewModel_HiltModules.KeyModule.provide()).put(ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChatViewModel_HiltModules.KeyModule.provide()).put(EditProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, EditProfileViewModel_HiltModules.KeyModule.provide()).put(ForwardViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ForwardViewModel_HiltModules.KeyModule.provide()).put(HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HomeViewModel_HiltModules.KeyModule.provide()).put(NewChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, NewChatViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(StatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, StatusViewModel_HiltModules.KeyModule.provide()).build());
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

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectPreferencesManager(instance, singletonCImpl.providePreferencesManagerProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends ZixoApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<CallsViewModel> callsViewModelProvider;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<EditProfileViewModel> editProfileViewModelProvider;

    private Provider<ForwardViewModel> forwardViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<NewChatViewModel> newChatViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<StatusViewModel> statusViewModelProvider;

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
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.callsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.editProfileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.forwardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.newChatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.statusViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(9).put(AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) authViewModelProvider)).put(CallsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) callsViewModelProvider)).put(ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) chatViewModelProvider)).put(EditProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) editProfileViewModelProvider)).put(ForwardViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) forwardViewModelProvider)).put(HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) homeViewModelProvider)).put(NewChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) newChatViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(StatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) statusViewModelProvider)).build());
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
          case 0: // com.zexo.app.ui.screens.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 1: // com.zexo.app.ui.screens.calls.CallsViewModel 
          return (T) new CallsViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideCallRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          case 2: // com.zexo.app.ui.screens.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          case 3: // com.zexo.app.ui.screens.profile.EditProfileViewModel 
          return (T) new EditProfileViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideStorageRepositoryProvider.get());

          case 4: // com.zexo.app.ui.screens.chat.ForwardViewModel 
          return (T) new ForwardViewModel(singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get());

          case 5: // com.zexo.app.ui.screens.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideCallRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          case 6: // com.zexo.app.ui.screens.contacts.NewChatViewModel 
          return (T) new NewChatViewModel(singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get());

          case 7: // com.zexo.app.ui.screens.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.providePreferencesManagerProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.zexo.app.ui.screens.status.StatusViewModel 
          return (T) new StatusViewModel(singletonCImpl.provideStatusRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends ZixoApp_HiltComponents.ActivityRetainedC {
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

  private static final class ServiceCImpl extends ZixoApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends ZixoApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<PreferencesManager> providePreferencesManagerProvider;

    private Provider<FirebaseAuth> provideFirebaseAuthProvider;

    private Provider<FirebaseFirestore> provideFirestoreProvider;

    private Provider<AuthRepository> provideAuthRepositoryProvider;

    private Provider<CallRepository> provideCallRepositoryProvider;

    private Provider<UserRepository> provideUserRepositoryProvider;

    private Provider<ChatRepository> provideChatRepositoryProvider;

    private Provider<FirebaseStorage> provideFirebaseStorageProvider;

    private Provider<StorageRepository> provideStorageRepositoryProvider;

    private Provider<StatusRepository> provideStatusRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.providePreferencesManagerProvider = DoubleCheck.provider(new SwitchingProvider<PreferencesManager>(singletonCImpl, 0));
      this.provideFirebaseAuthProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuth>(singletonCImpl, 2));
      this.provideFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseFirestore>(singletonCImpl, 3));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 1));
      this.provideCallRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CallRepository>(singletonCImpl, 4));
      this.provideUserRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserRepository>(singletonCImpl, 5));
      this.provideChatRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepository>(singletonCImpl, 6));
      this.provideFirebaseStorageProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseStorage>(singletonCImpl, 8));
      this.provideStorageRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<StorageRepository>(singletonCImpl, 7));
      this.provideStatusRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<StatusRepository>(singletonCImpl, 9));
    }

    @Override
    public void injectZixoApp(ZixoApp arg0) {
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
          case 0: // com.zexo.app.data.local.PreferencesManager 
          return (T) AppModule_ProvidePreferencesManagerFactory.providePreferencesManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.zexo.app.data.repository.AuthRepository 
          return (T) AppModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.provideFirestoreProvider.get());

          case 2: // com.google.firebase.auth.FirebaseAuth 
          return (T) AppModule_ProvideFirebaseAuthFactory.provideFirebaseAuth();

          case 3: // com.google.firebase.firestore.FirebaseFirestore 
          return (T) AppModule_ProvideFirestoreFactory.provideFirestore();

          case 4: // com.zexo.app.data.repository.CallRepository 
          return (T) AppModule_ProvideCallRepositoryFactory.provideCallRepository(singletonCImpl.provideFirestoreProvider.get());

          case 5: // com.zexo.app.data.repository.UserRepository 
          return (T) AppModule_ProvideUserRepositoryFactory.provideUserRepository(singletonCImpl.provideFirestoreProvider.get());

          case 6: // com.zexo.app.data.repository.ChatRepository 
          return (T) AppModule_ProvideChatRepositoryFactory.provideChatRepository(singletonCImpl.provideFirestoreProvider.get());

          case 7: // com.zexo.app.data.repository.StorageRepository 
          return (T) AppModule_ProvideStorageRepositoryFactory.provideStorageRepository(singletonCImpl.provideFirebaseStorageProvider.get());

          case 8: // com.google.firebase.storage.FirebaseStorage 
          return (T) AppModule_ProvideFirebaseStorageFactory.provideFirebaseStorage();

          case 9: // com.zexo.app.data.repository.StatusRepository 
          return (T) AppModule_ProvideStatusRepositoryFactory.provideStatusRepository(singletonCImpl.provideFirestoreProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
