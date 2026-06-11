package com.zexo.app.ui.screens.admin;

import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.model.AdminConfig;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.UserRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u001d\n\u0002\u0010\u0002\n\u0002\b\u0018\b\u0007\u0018\u0000 V2\u00020\u0001:\u0001VB\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010>\u001a\u00020?J\u000e\u0010@\u001a\u00020?2\u0006\u0010A\u001a\u00020\u0010J\u0006\u0010B\u001a\u00020?J\b\u0010C\u001a\u00020?H\u0002J\u000e\u0010D\u001a\u00020\u00122\u0006\u0010A\u001a\u00020\u0010J\u0006\u0010E\u001a\u00020?J\u0006\u0010F\u001a\u00020?J\u0006\u0010G\u001a\u00020?J\u0006\u0010H\u001a\u00020?J\b\u0010I\u001a\u00020?H\u0014J\u0010\u0010J\u001a\u00020?2\b\u0010A\u001a\u0004\u0018\u00010\u0010J\u0006\u0010K\u001a\u00020?J\u0006\u0010L\u001a\u00020?J\u000e\u0010M\u001a\u00020?2\u0006\u0010A\u001a\u00020\u0010J\u000e\u0010N\u001a\u00020?2\u0006\u0010A\u001a\u00020\u0010J\u000e\u0010O\u001a\u00020?2\u0006\u0010P\u001a\u00020\rJ\u000e\u0010Q\u001a\u00020?2\u0006\u0010P\u001a\u00020\u0015J\u000e\u0010R\u001a\u00020?2\u0006\u0010S\u001a\u00020\u0017J\u000e\u0010T\u001a\u00020?2\u0006\u0010U\u001a\u00020\u0017R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00120\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0016\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00170\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00170\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0019\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00100\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00120\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00170\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u001f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010 \u001a\b\u0012\u0004\u0012\u00020\u000b0!\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0017\u0010$\u001a\b\u0012\u0004\u0012\u00020\r0!\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010#R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010&\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0!\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010#R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00120!\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010#R\u0017\u0010)\u001a\b\u0012\u0004\u0012\u00020\u00120!\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010#R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020\u00150!\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010#R\u0019\u0010,\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00170!\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010#R\u0017\u0010.\u001a\b\u0012\u0004\u0012\u00020\u00170!\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010#R\u0019\u00100\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00100!\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010#R\u0017\u00102\u001a\b\u0012\u0004\u0012\u00020\u00120!\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010#R\u0017\u00104\u001a\b\u0012\u0004\u0012\u00020\u000b0!\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010#R\u0017\u00106\u001a\b\u0012\u0004\u0012\u00020\u000b0!\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u0010#R\u0017\u00108\u001a\b\u0012\u0004\u0012\u00020\u000b0!\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010#R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010:\u001a\b\u0012\u0004\u0012\u00020\u00170!\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u0010#R\u001d\u0010<\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0!\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u0010#\u00a8\u0006W"}, d2 = {"Lcom/zexo/app/ui/screens/admin/AdminViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "userRepository", "Lcom/zexo/app/data/repository/UserRepository;", "firestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "(Lcom/zexo/app/data/repository/AuthRepository;Lcom/zexo/app/data/repository/UserRepository;Lcom/google/firebase/firestore/FirebaseFirestore;)V", "_activeUsers", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_adminConfig", "Lcom/zexo/app/data/model/AdminConfig;", "_filteredUsers", "", "Lcom/zexo/app/data/model/User;", "_isLoading", "", "_isSaving", "_landingConfig", "Lcom/zexo/app/ui/screens/admin/LandingConfig;", "_message", "", "_notificationText", "_selectedUser", "_showDeleteConfirm", "_totalCalls", "_totalChats", "_totalUsers", "_userSearchQuery", "_users", "activeUsers", "Lkotlinx/coroutines/flow/StateFlow;", "getActiveUsers", "()Lkotlinx/coroutines/flow/StateFlow;", "adminConfig", "getAdminConfig", "filteredUsers", "getFilteredUsers", "isLoading", "isSaving", "landingConfig", "getLandingConfig", "message", "getMessage", "notificationText", "getNotificationText", "selectedUser", "getSelectedUser", "showDeleteConfirm", "getShowDeleteConfirm", "totalCalls", "getTotalCalls", "totalChats", "getTotalChats", "totalUsers", "getTotalUsers", "userSearchQuery", "getUserSearchQuery", "users", "getUsers", "clearMessage", "", "deleteUser", "user", "dismissDeleteConfirmation", "filterUsers", "isUserDisabled", "loadAdminConfig", "loadDashboardStats", "loadLandingConfig", "loadUsers", "onCleared", "selectUser", "sendNotification", "showDeleteConfirmation", "toggleAdminRole", "toggleUserDisabled", "updateAdminConfig", "config", "updateLandingConfig", "updateNotificationText", "text", "updateUserSearchQuery", "query", "Companion", "app_adminRelease"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class AdminViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.UserRepository userRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.firestore.FirebaseFirestore firestore = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AdminViewModel";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String ADMIN_CONFIG_DOC = "admin_config";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String LANDING_CONFIG_DOC = "landing_config";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.zexo.app.data.model.User>> _users = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.User>> users = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.zexo.app.data.model.User>> _filteredUsers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.User>> filteredUsers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _userSearchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> userSearchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _totalUsers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> totalUsers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _activeUsers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> activeUsers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _totalChats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> totalChats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _totalCalls = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> totalCalls = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.data.model.AdminConfig> _adminConfig = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.data.model.AdminConfig> adminConfig = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.ui.screens.admin.LandingConfig> _landingConfig = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.admin.LandingConfig> landingConfig = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isSaving = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isSaving = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _message = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> message = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _notificationText = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> notificationText = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.data.model.User> _selectedUser = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.data.model.User> selectedUser = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _showDeleteConfirm = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> showDeleteConfirm = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.ui.screens.admin.AdminViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public AdminViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.UserRepository userRepository, @org.jetbrains.annotations.NotNull()
    com.google.firebase.firestore.FirebaseFirestore firestore) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.User>> getUsers() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.User>> getFilteredUsers() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getUserSearchQuery() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getTotalUsers() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getActiveUsers() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getTotalChats() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getTotalCalls() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.data.model.AdminConfig> getAdminConfig() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.admin.LandingConfig> getLandingConfig() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isSaving() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getNotificationText() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.data.model.User> getSelectedUser() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getShowDeleteConfirm() {
        return null;
    }
    
    public final void loadDashboardStats() {
    }
    
    public final void loadUsers() {
    }
    
    public final void updateUserSearchQuery(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
    }
    
    private final void filterUsers() {
    }
    
    public final void selectUser(@org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User user) {
    }
    
    public final void showDeleteConfirmation() {
    }
    
    public final void dismissDeleteConfirmation() {
    }
    
    public final void toggleAdminRole(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.User user) {
    }
    
    public final void toggleUserDisabled(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.User user) {
    }
    
    public final void deleteUser(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.User user) {
    }
    
    public final void loadAdminConfig() {
    }
    
    public final void updateAdminConfig(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.AdminConfig config) {
    }
    
    public final void loadLandingConfig() {
    }
    
    public final void updateLandingConfig(@org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.admin.LandingConfig config) {
    }
    
    public final void updateNotificationText(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void sendNotification() {
    }
    
    public final void clearMessage() {
    }
    
    public final boolean isUserDisabled(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.User user) {
        return false;
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/zexo/app/ui/screens/admin/AdminViewModel$Companion;", "", "()V", "ADMIN_CONFIG_DOC", "", "LANDING_CONFIG_DOC", "TAG", "app_adminRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}