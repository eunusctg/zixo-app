package com.zixo.app;

import android.app.Activity;
import android.app.Service;
import android.content.SharedPreferences;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.zixo.app.data.local.PreferencesDataStore;
import com.zixo.app.data.local.room.ZixoDatabase;
import com.zixo.app.data.remote.cloudflare.CloudflareApi;
import com.zixo.app.data.remote.cloudflare.CloudflareApiService;
import com.zixo.app.data.remote.firebase.FirebaseAuthService;
import com.zixo.app.data.remote.firebase.FirestoreService;
import com.zixo.app.data.remote.firebase.ZixoMessagingService;
import com.zixo.app.data.remote.firebase.ZixoMessagingService_MembersInjector;
import com.zixo.app.data.remote.webrtc.FirebaseSignalingClient;
import com.zixo.app.data.remote.webrtc.WebRtcClient;
import com.zixo.app.data.repository.AuthRepositoryImpl;
import com.zixo.app.data.repository.CallRepositoryImpl;
import com.zixo.app.data.repository.ChatRepositoryImpl;
import com.zixo.app.data.repository.ContactRepositoryImpl;
import com.zixo.app.data.repository.SettingsRepositoryImpl;
import com.zixo.app.data.repository.StatusRepositoryImpl;
import com.zixo.app.di.AppModule_ProvideAuthRepositoryFactory;
import com.zixo.app.di.AppModule_ProvideCallRepositoryFactory;
import com.zixo.app.di.AppModule_ProvideChatRepositoryFactory;
import com.zixo.app.di.AppModule_ProvideCloudflareApiFactory;
import com.zixo.app.di.AppModule_ProvideCloudflareApiServiceFactory;
import com.zixo.app.di.AppModule_ProvideContactRepositoryFactory;
import com.zixo.app.di.AppModule_ProvideEncryptMessageUseCaseFactory;
import com.zixo.app.di.AppModule_ProvideFirebaseSignalingClientFactory;
import com.zixo.app.di.AppModule_ProvideGetContactsUseCaseFactory;
import com.zixo.app.di.AppModule_ProvideInitiateCallUseCaseFactory;
import com.zixo.app.di.AppModule_ProvideJsonFactory;
import com.zixo.app.di.AppModule_ProvideOkHttpClientFactory;
import com.zixo.app.di.AppModule_ProvidePreferencesDataStoreFactory;
import com.zixo.app.di.AppModule_ProvideRetrofitFactory;
import com.zixo.app.di.AppModule_ProvideSendMessageUseCaseFactory;
import com.zixo.app.di.AppModule_ProvideSettingsRepositoryFactory;
import com.zixo.app.di.AppModule_ProvideSharedPreferencesFactory;
import com.zixo.app.di.AppModule_ProvideStatusRepositoryFactory;
import com.zixo.app.di.AppModule_ProvideWebRtcClientFactory;
import com.zixo.app.di.AppModule_ProvideZixoDatabaseFactory;
import com.zixo.app.di.FirebaseModule_ProvideFirebaseAuthFactory;
import com.zixo.app.di.FirebaseModule_ProvideFirebaseDatabaseFactory;
import com.zixo.app.di.FirebaseModule_ProvideFirebaseFirestoreFactory;
import com.zixo.app.di.FirebaseModule_ProvideFirebaseStorageFactory;
import com.zixo.app.domain.repository.AuthRepository;
import com.zixo.app.domain.repository.CallRepository;
import com.zixo.app.domain.repository.ChatRepository;
import com.zixo.app.domain.repository.ContactRepository;
import com.zixo.app.domain.repository.SettingsRepository;
import com.zixo.app.domain.repository.StatusRepository;
import com.zixo.app.domain.usecase.EncryptMessageUseCase;
import com.zixo.app.domain.usecase.GetContactsUseCase;
import com.zixo.app.domain.usecase.InitiateCallUseCase;
import com.zixo.app.domain.usecase.SendMessageUseCase;
import com.zixo.app.ui.chat.ChatViewModel;
import com.zixo.app.ui.chat.ChatViewModel_HiltModules;
import com.zixo.app.ui.chat.ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.chat.ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.chat.GroupChatViewModel;
import com.zixo.app.ui.chat.GroupChatViewModel_HiltModules;
import com.zixo.app.ui.chat.GroupChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.chat.GroupChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.contacts.ContactListViewModel;
import com.zixo.app.ui.contacts.ContactListViewModel_HiltModules;
import com.zixo.app.ui.contacts.ContactListViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.contacts.ContactListViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.main.HomeViewModel;
import com.zixo.app.ui.main.HomeViewModel_HiltModules;
import com.zixo.app.ui.main.HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.main.HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.screens.auth.AuthViewModel;
import com.zixo.app.ui.screens.auth.AuthViewModel_HiltModules;
import com.zixo.app.ui.screens.auth.AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.screens.auth.AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.screens.calls.CallsViewModel;
import com.zixo.app.ui.screens.calls.CallsViewModel_HiltModules;
import com.zixo.app.ui.screens.calls.CallsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.screens.calls.CallsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.screens.chats.ChatsViewModel;
import com.zixo.app.ui.screens.chats.ChatsViewModel_HiltModules;
import com.zixo.app.ui.screens.chats.ChatsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.screens.chats.ChatsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.settings.SettingsViewModel;
import com.zixo.app.ui.settings.SettingsViewModel_HiltModules;
import com.zixo.app.ui.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.zixo.app.ui.status.StatusViewModel;
import com.zixo.app.ui.status.StatusViewModel_HiltModules;
import com.zixo.app.ui.status.StatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.zixo.app.ui.status.StatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
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
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerZixoApplication_HiltComponents_SingletonC {
  private DaggerZixoApplication_HiltComponents_SingletonC() {
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

    public ZixoApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements ZixoApplication_HiltComponents.ActivityRetainedC.Builder {
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
    public ZixoApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements ZixoApplication_HiltComponents.ActivityC.Builder {
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
    public ZixoApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements ZixoApplication_HiltComponents.FragmentC.Builder {
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
    public ZixoApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements ZixoApplication_HiltComponents.ViewWithFragmentC.Builder {
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
    public ZixoApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements ZixoApplication_HiltComponents.ViewC.Builder {
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
    public ZixoApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements ZixoApplication_HiltComponents.ViewModelC.Builder {
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
    public ZixoApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements ZixoApplication_HiltComponents.ServiceC.Builder {
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
    public ZixoApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends ZixoApplication_HiltComponents.ViewWithFragmentC {
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

  private static final class FragmentCImpl extends ZixoApplication_HiltComponents.FragmentC {
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

  private static final class ViewCImpl extends ZixoApplication_HiltComponents.ViewC {
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

  private static final class ActivityCImpl extends ZixoApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(9).put(AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AuthViewModel_HiltModules.KeyModule.provide()).put(CallsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CallsViewModel_HiltModules.KeyModule.provide()).put(ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChatViewModel_HiltModules.KeyModule.provide()).put(ChatsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChatsViewModel_HiltModules.KeyModule.provide()).put(ContactListViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ContactListViewModel_HiltModules.KeyModule.provide()).put(GroupChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, GroupChatViewModel_HiltModules.KeyModule.provide()).put(HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HomeViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(StatusViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, StatusViewModel_HiltModules.KeyModule.provide()).build());
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
      MainActivity_MembersInjector.injectPreferencesDataStore(instance, singletonCImpl.preferencesDataStoreProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends ZixoApplication_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<CallsViewModel> callsViewModelProvider;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<ChatsViewModel> chatsViewModelProvider;

    private Provider<ContactListViewModel> contactListViewModelProvider;

    private Provider<GroupChatViewModel> groupChatViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<StatusViewModel> statusViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.callsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.chatsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.contactListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.groupChatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.statusViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(9).put(AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) authViewModelProvider)).put(CallsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) callsViewModelProvider)).put(ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) chatViewModelProvider)).put(ChatsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) chatsViewModelProvider)).put(ContactListViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) contactListViewModelProvider)).put(GroupChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) groupChatViewModelProvider)).put(HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) homeViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(StatusViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) statusViewModelProvider)).build());
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
          case 0: // com.zixo.app.ui.screens.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 1: // com.zixo.app.ui.screens.calls.CallsViewModel 
          return (T) new CallsViewModel(singletonCImpl.provideCallRepositoryProvider.get(), singletonCImpl.provideInitiateCallUseCaseProvider.get(), singletonCImpl.provideFirebaseAuthProvider.get());

          case 2: // com.zixo.app.ui.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideCallRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 3: // com.zixo.app.ui.screens.chats.ChatsViewModel 
          return (T) new ChatsViewModel(singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideGetContactsUseCaseProvider.get());

          case 4: // com.zixo.app.ui.contacts.ContactListViewModel 
          return (T) new ContactListViewModel(singletonCImpl.provideContactRepositoryProvider.get());

          case 5: // com.zixo.app.ui.chat.GroupChatViewModel 
          return (T) new GroupChatViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideSendMessageUseCaseProvider.get());

          case 6: // com.zixo.app.ui.main.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideSettingsRepositoryProvider.get());

          case 7: // com.zixo.app.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideSettingsRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get());

          case 8: // com.zixo.app.ui.status.StatusViewModel 
          return (T) new StatusViewModel(singletonCImpl.provideStatusRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideSettingsRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends ZixoApplication_HiltComponents.ActivityRetainedC {
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

  private static final class ServiceCImpl extends ZixoApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectZixoMessagingService(ZixoMessagingService zixoMessagingService) {
      injectZixoMessagingService2(zixoMessagingService);
    }

    @CanIgnoreReturnValue
    private ZixoMessagingService injectZixoMessagingService2(ZixoMessagingService instance) {
      ZixoMessagingService_MembersInjector.injectFirestore(instance, singletonCImpl.provideFirebaseFirestoreProvider.get());
      ZixoMessagingService_MembersInjector.injectSharedPrefs(instance, singletonCImpl.provideSharedPreferencesProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends ZixoApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<DataStore<Preferences>> providePreferencesDataStoreProvider;

    private Provider<PreferencesDataStore> preferencesDataStoreProvider;

    private Provider<FirebaseAuth> provideFirebaseAuthProvider;

    private Provider<FirebaseAuthService> firebaseAuthServiceProvider;

    private Provider<FirebaseFirestore> provideFirebaseFirestoreProvider;

    private Provider<FirebaseStorage> provideFirebaseStorageProvider;

    private Provider<FirestoreService> firestoreServiceProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<CloudflareApi> provideCloudflareApiProvider;

    private Provider<CloudflareApiService> provideCloudflareApiServiceProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<AuthRepository> provideAuthRepositoryProvider;

    private Provider<FirebaseDatabase> provideFirebaseDatabaseProvider;

    private Provider<ContactRepositoryImpl> contactRepositoryImplProvider;

    private Provider<ContactRepository> provideContactRepositoryProvider;

    private Provider<WebRtcClient> provideWebRtcClientProvider;

    private Provider<FirebaseSignalingClient> provideFirebaseSignalingClientProvider;

    private Provider<CallRepositoryImpl> callRepositoryImplProvider;

    private Provider<CallRepository> provideCallRepositoryProvider;

    private Provider<InitiateCallUseCase> provideInitiateCallUseCaseProvider;

    private Provider<ChatRepositoryImpl> chatRepositoryImplProvider;

    private Provider<ChatRepository> provideChatRepositoryProvider;

    private Provider<GetContactsUseCase> provideGetContactsUseCaseProvider;

    private Provider<EncryptMessageUseCase> provideEncryptMessageUseCaseProvider;

    private Provider<SendMessageUseCase> provideSendMessageUseCaseProvider;

    private Provider<ZixoDatabase> provideZixoDatabaseProvider;

    private Provider<SettingsRepositoryImpl> settingsRepositoryImplProvider;

    private Provider<SettingsRepository> provideSettingsRepositoryProvider;

    private Provider<StatusRepositoryImpl> statusRepositoryImplProvider;

    private Provider<StatusRepository> provideStatusRepositoryProvider;

    private Provider<SharedPreferences> provideSharedPreferencesProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);
      initialize2(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.providePreferencesDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 1));
      this.preferencesDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<PreferencesDataStore>(singletonCImpl, 0));
      this.provideFirebaseAuthProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuth>(singletonCImpl, 5));
      this.firebaseAuthServiceProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuthService>(singletonCImpl, 4));
      this.provideFirebaseFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseFirestore>(singletonCImpl, 7));
      this.provideFirebaseStorageProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseStorage>(singletonCImpl, 8));
      this.firestoreServiceProvider = DoubleCheck.provider(new SwitchingProvider<FirestoreService>(singletonCImpl, 6));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 12));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 13));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 11));
      this.provideCloudflareApiProvider = DoubleCheck.provider(new SwitchingProvider<CloudflareApi>(singletonCImpl, 10));
      this.provideCloudflareApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<CloudflareApiService>(singletonCImpl, 9));
      this.authRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepositoryImpl>(singletonCImpl, 3));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 2));
      this.provideFirebaseDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseDatabase>(singletonCImpl, 16));
      this.contactRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ContactRepositoryImpl>(singletonCImpl, 18));
      this.provideContactRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ContactRepository>(singletonCImpl, 17));
      this.provideWebRtcClientProvider = DoubleCheck.provider(new SwitchingProvider<WebRtcClient>(singletonCImpl, 19));
      this.provideFirebaseSignalingClientProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseSignalingClient>(singletonCImpl, 20));
      this.callRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<CallRepositoryImpl>(singletonCImpl, 15));
      this.provideCallRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CallRepository>(singletonCImpl, 14));
      this.provideInitiateCallUseCaseProvider = DoubleCheck.provider(new SwitchingProvider<InitiateCallUseCase>(singletonCImpl, 21));
      this.chatRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepositoryImpl>(singletonCImpl, 23));
      this.provideChatRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepository>(singletonCImpl, 22));
      this.provideGetContactsUseCaseProvider = DoubleCheck.provider(new SwitchingProvider<GetContactsUseCase>(singletonCImpl, 24));
    }

    @SuppressWarnings("unchecked")
    private void initialize2(final ApplicationContextModule applicationContextModuleParam) {
      this.provideEncryptMessageUseCaseProvider = DoubleCheck.provider(new SwitchingProvider<EncryptMessageUseCase>(singletonCImpl, 26));
      this.provideSendMessageUseCaseProvider = DoubleCheck.provider(new SwitchingProvider<SendMessageUseCase>(singletonCImpl, 25));
      this.provideZixoDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<ZixoDatabase>(singletonCImpl, 29));
      this.settingsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepositoryImpl>(singletonCImpl, 28));
      this.provideSettingsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepository>(singletonCImpl, 27));
      this.statusRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<StatusRepositoryImpl>(singletonCImpl, 31));
      this.provideStatusRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<StatusRepository>(singletonCImpl, 30));
      this.provideSharedPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<SharedPreferences>(singletonCImpl, 32));
    }

    @Override
    public void injectZixoApplication(ZixoApplication zixoApplication) {
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
          case 0: // com.zixo.app.data.local.PreferencesDataStore 
          return (T) new PreferencesDataStore(singletonCImpl.providePreferencesDataStoreProvider.get());

          case 1: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvidePreferencesDataStoreFactory.providePreferencesDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.zixo.app.domain.repository.AuthRepository 
          return (T) AppModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.authRepositoryImplProvider.get());

          case 3: // com.zixo.app.data.repository.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.firebaseAuthServiceProvider.get(), singletonCImpl.firestoreServiceProvider.get(), singletonCImpl.provideCloudflareApiServiceProvider.get());

          case 4: // com.zixo.app.data.remote.firebase.FirebaseAuthService 
          return (T) new FirebaseAuthService(singletonCImpl.provideFirebaseAuthProvider.get());

          case 5: // com.google.firebase.auth.FirebaseAuth 
          return (T) FirebaseModule_ProvideFirebaseAuthFactory.provideFirebaseAuth();

          case 6: // com.zixo.app.data.remote.firebase.FirestoreService 
          return (T) new FirestoreService(singletonCImpl.provideFirebaseFirestoreProvider.get(), singletonCImpl.provideFirebaseStorageProvider.get());

          case 7: // com.google.firebase.firestore.FirebaseFirestore 
          return (T) FirebaseModule_ProvideFirebaseFirestoreFactory.provideFirebaseFirestore();

          case 8: // com.google.firebase.storage.FirebaseStorage 
          return (T) FirebaseModule_ProvideFirebaseStorageFactory.provideFirebaseStorage();

          case 9: // com.zixo.app.data.remote.cloudflare.CloudflareApiService 
          return (T) AppModule_ProvideCloudflareApiServiceFactory.provideCloudflareApiService(singletonCImpl.provideCloudflareApiProvider.get());

          case 10: // com.zixo.app.data.remote.cloudflare.CloudflareApi 
          return (T) AppModule_ProvideCloudflareApiFactory.provideCloudflareApi(singletonCImpl.provideRetrofitProvider.get());

          case 11: // retrofit2.Retrofit 
          return (T) AppModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 12: // okhttp3.OkHttpClient 
          return (T) AppModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 13: // kotlinx.serialization.json.Json 
          return (T) AppModule_ProvideJsonFactory.provideJson();

          case 14: // com.zixo.app.domain.repository.CallRepository 
          return (T) AppModule_ProvideCallRepositoryFactory.provideCallRepository(singletonCImpl.callRepositoryImplProvider.get());

          case 15: // com.zixo.app.data.repository.CallRepositoryImpl 
          return (T) new CallRepositoryImpl(singletonCImpl.provideFirebaseFirestoreProvider.get(), singletonCImpl.provideFirebaseDatabaseProvider.get(), singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideWebRtcClientProvider.get(), singletonCImpl.provideFirebaseSignalingClientProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 16: // com.google.firebase.database.FirebaseDatabase 
          return (T) FirebaseModule_ProvideFirebaseDatabaseFactory.provideFirebaseDatabase();

          case 17: // com.zixo.app.domain.repository.ContactRepository 
          return (T) AppModule_ProvideContactRepositoryFactory.provideContactRepository(singletonCImpl.contactRepositoryImplProvider.get());

          case 18: // com.zixo.app.data.repository.ContactRepositoryImpl 
          return (T) new ContactRepositoryImpl(singletonCImpl.provideFirebaseFirestoreProvider.get(), singletonCImpl.provideFirebaseAuthProvider.get());

          case 19: // com.zixo.app.data.remote.webrtc.WebRtcClient 
          return (T) AppModule_ProvideWebRtcClientFactory.provideWebRtcClient(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 20: // com.zixo.app.data.remote.webrtc.FirebaseSignalingClient 
          return (T) AppModule_ProvideFirebaseSignalingClientFactory.provideFirebaseSignalingClient(singletonCImpl.provideFirebaseDatabaseProvider.get());

          case 21: // com.zixo.app.domain.usecase.InitiateCallUseCase 
          return (T) AppModule_ProvideInitiateCallUseCaseFactory.provideInitiateCallUseCase(singletonCImpl.provideCallRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get());

          case 22: // com.zixo.app.domain.repository.ChatRepository 
          return (T) AppModule_ProvideChatRepositoryFactory.provideChatRepository(singletonCImpl.chatRepositoryImplProvider.get());

          case 23: // com.zixo.app.data.repository.ChatRepositoryImpl 
          return (T) new ChatRepositoryImpl(singletonCImpl.provideFirebaseFirestoreProvider.get(), singletonCImpl.provideFirebaseDatabaseProvider.get(), singletonCImpl.provideFirebaseStorageProvider.get(), singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.provideContactRepositoryProvider.get());

          case 24: // com.zixo.app.domain.usecase.GetContactsUseCase 
          return (T) AppModule_ProvideGetContactsUseCaseFactory.provideGetContactsUseCase(singletonCImpl.provideContactRepositoryProvider.get());

          case 25: // com.zixo.app.domain.usecase.SendMessageUseCase 
          return (T) AppModule_ProvideSendMessageUseCaseFactory.provideSendMessageUseCase(singletonCImpl.provideChatRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideEncryptMessageUseCaseProvider.get());

          case 26: // com.zixo.app.domain.usecase.EncryptMessageUseCase 
          return (T) AppModule_ProvideEncryptMessageUseCaseFactory.provideEncryptMessageUseCase(singletonCImpl.provideContactRepositoryProvider.get());

          case 27: // com.zixo.app.domain.repository.SettingsRepository 
          return (T) AppModule_ProvideSettingsRepositoryFactory.provideSettingsRepository(singletonCImpl.settingsRepositoryImplProvider.get());

          case 28: // com.zixo.app.data.repository.SettingsRepositoryImpl 
          return (T) new SettingsRepositoryImpl(singletonCImpl.preferencesDataStoreProvider.get(), singletonCImpl.provideZixoDatabaseProvider.get(), singletonCImpl.firestoreServiceProvider.get(), singletonCImpl.firebaseAuthServiceProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 29: // com.zixo.app.data.local.room.ZixoDatabase 
          return (T) AppModule_ProvideZixoDatabaseFactory.provideZixoDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 30: // com.zixo.app.domain.repository.StatusRepository 
          return (T) AppModule_ProvideStatusRepositoryFactory.provideStatusRepository(singletonCImpl.statusRepositoryImplProvider.get());

          case 31: // com.zixo.app.data.repository.StatusRepositoryImpl 
          return (T) new StatusRepositoryImpl(singletonCImpl.provideFirebaseFirestoreProvider.get(), singletonCImpl.provideFirebaseStorageProvider.get(), singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.provideContactRepositoryProvider.get());

          case 32: // android.content.SharedPreferences 
          return (T) AppModule_ProvideSharedPreferencesFactory.provideSharedPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
