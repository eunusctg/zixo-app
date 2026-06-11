package com.zexo.app.ui.screens.settings;

import android.content.Context;
import android.util.Log;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.model.User;
import com.zexo.app.data.model.UserSettings;
import com.zexo.app.data.repository.AuthRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000`\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 22\u00020\u0001:\u00012B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u0018\u001a\u00020\u0019J\u000e\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001c0\u001bH\u0002J\b\u0010\u001d\u001a\u00020\u0019H\u0002J\b\u0010\u001e\u001a\u00020\u0019H\u0002J\u0006\u0010\u001f\u001a\u00020\u0019J\u0010\u0010 \u001a\u00020\u00192\u0006\u0010!\u001a\u00020\"H\u0002J\u0016\u0010#\u001a\u00020\u00192\u0006\u0010!\u001a\u00020\"H\u0082@\u00a2\u0006\u0002\u0010$J\u000e\u0010%\u001a\u00020\u00192\u0006\u0010&\u001a\u00020\bJ\u000e\u0010\'\u001a\u00020\u00192\u0006\u0010(\u001a\u00020\u000bJ\u000e\u0010)\u001a\u00020\u00192\u0006\u0010(\u001a\u00020\u000bJ\u000e\u0010*\u001a\u00020\u00192\u0006\u0010(\u001a\u00020\u000bJ\u000e\u0010+\u001a\u00020\u00192\u0006\u0010(\u001a\u00020\u000bJ\u000e\u0010,\u001a\u00020\u00192\u0006\u0010(\u001a\u00020\u000bJ\u000e\u0010-\u001a\u00020\u00192\u0006\u0010(\u001a\u00020\u000bJ\u000e\u0010.\u001a\u00020\u00192\u0006\u0010(\u001a\u00020\u000bJ\u001c\u0010/\u001a\u00020\u00192\u0012\u00100\u001a\u000e\u0012\u0004\u0012\u00020\"\u0012\u0004\u0012\u00020\"01H\u0002R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\u0004\u0018\u00010\u000f8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\r0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0015\u00a8\u00063"}, d2 = {"Lcom/zexo/app/ui/screens/settings/SettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "firestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "(Lcom/zexo/app/data/repository/AuthRepository;Lcom/google/firebase/firestore/FirebaseFirestore;)V", "_appContext", "Landroid/content/Context;", "_darkTheme", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_uiState", "Lcom/zexo/app/ui/screens/settings/SettingsUiState;", "currentUid", "", "getCurrentUid", "()Ljava/lang/String;", "darkTheme", "Lkotlinx/coroutines/flow/StateFlow;", "getDarkTheme", "()Lkotlinx/coroutines/flow/StateFlow;", "uiState", "getUiState", "clearError", "", "getDataStore", "Landroidx/datastore/core/DataStore;", "Landroidx/datastore/preferences/core/Preferences;", "loadSettings", "loadUserProfile", "logout", "saveSettings", "settings", "Lcom/zexo/app/data/model/UserSettings;", "saveToDataStore", "(Lcom/zexo/app/data/model/UserSettings;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setAppContext", "context", "setBiometricLock", "enabled", "setDarkTheme", "setNotifications", "setReadReceipts", "setShowLastSeen", "setShowOnlineStatus", "setTypingIndicators", "updateSetting", "transform", "Lkotlin/Function1;", "Companion", "app_adminRelease"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.firestore.FirebaseFirestore firestore = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "SettingsViewModel";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SETTINGS_COLLECTION = "user_settings";
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> KEY_DARK_THEME = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> KEY_NOTIFICATIONS = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> KEY_BIOMETRIC_LOCK = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> KEY_SHOW_ONLINE_STATUS = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> KEY_SHOW_LAST_SEEN = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> KEY_READ_RECEIPTS = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> KEY_TYPING_INDICATORS = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.ui.screens.settings.SettingsUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.settings.SettingsUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _darkTheme = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> darkTheme = null;
    private android.content.Context _appContext;
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.ui.screens.settings.SettingsViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public SettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.google.firebase.firestore.FirebaseFirestore firestore) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.settings.SettingsUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getDarkTheme() {
        return null;
    }
    
    private final java.lang.String getCurrentUid() {
        return null;
    }
    
    private final void loadUserProfile() {
    }
    
    private final void loadSettings() {
    }
    
    public final void setDarkTheme(boolean enabled) {
    }
    
    public final void setNotifications(boolean enabled) {
    }
    
    public final void setBiometricLock(boolean enabled) {
    }
    
    public final void setShowOnlineStatus(boolean enabled) {
    }
    
    public final void setShowLastSeen(boolean enabled) {
    }
    
    public final void setReadReceipts(boolean enabled) {
    }
    
    public final void setTypingIndicators(boolean enabled) {
    }
    
    private final void updateSetting(kotlin.jvm.functions.Function1<? super com.zexo.app.data.model.UserSettings, com.zexo.app.data.model.UserSettings> transform) {
    }
    
    private final void saveSettings(com.zexo.app.data.model.UserSettings settings) {
    }
    
    private final java.lang.Object saveToDataStore(com.zexo.app.data.model.UserSettings settings, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void logout() {
    }
    
    public final void clearError() {
    }
    
    public final void setAppContext(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    private final androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> getDataStore() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u000f\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u0007R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\u0007R\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u0007R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0007R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0007R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0007R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0015X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/zexo/app/ui/screens/settings/SettingsViewModel$Companion;", "", "()V", "KEY_BIOMETRIC_LOCK", "Landroidx/datastore/preferences/core/Preferences$Key;", "", "getKEY_BIOMETRIC_LOCK", "()Landroidx/datastore/preferences/core/Preferences$Key;", "KEY_DARK_THEME", "getKEY_DARK_THEME", "KEY_NOTIFICATIONS", "getKEY_NOTIFICATIONS", "KEY_READ_RECEIPTS", "getKEY_READ_RECEIPTS", "KEY_SHOW_LAST_SEEN", "getKEY_SHOW_LAST_SEEN", "KEY_SHOW_ONLINE_STATUS", "getKEY_SHOW_ONLINE_STATUS", "KEY_TYPING_INDICATORS", "getKEY_TYPING_INDICATORS", "SETTINGS_COLLECTION", "", "TAG", "app_adminRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> getKEY_DARK_THEME() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> getKEY_NOTIFICATIONS() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> getKEY_BIOMETRIC_LOCK() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> getKEY_SHOW_ONLINE_STATUS() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> getKEY_SHOW_LAST_SEEN() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> getKEY_READ_RECEIPTS() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> getKEY_TYPING_INDICATORS() {
            return null;
        }
    }
}