package com.zexo.app.ui.screens.home;

import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.zexo.app.BuildConfig;
import com.zexo.app.data.model.CallRecord;
import com.zexo.app.data.model.Chat;
import com.zexo.app.data.model.Status;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
import com.zexo.app.data.repository.ChatRepository;
import com.zexo.app.data.repository.StatusRepository;
import com.zexo.app.data.repository.UserRepository;
import com.zexo.app.ui.navigation.HomeTab;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.flow.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0086\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u001b\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u000f\b\u0007\u0018\u0000 U2\u00020\u0001:\u0001UB/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u0006\u0010@\u001a\u00020AJ\u000e\u0010B\u001a\u00020\u001a2\u0006\u0010C\u001a\u00020\u001aJ\u0010\u0010D\u001a\u0004\u0018\u00010\u00142\u0006\u0010E\u001a\u00020\u0010J\u000e\u0010F\u001a\u00020G2\u0006\u0010E\u001a\u00020\u0010J\u000e\u0010H\u001a\u00020\u00162\u0006\u0010C\u001a\u00020\u001aJ\b\u0010I\u001a\u00020AH\u0002J\u0010\u0010J\u001a\u00020A2\u0006\u0010C\u001a\u00020\u001aH\u0002J\u0010\u0010K\u001a\u00020A2\u0006\u0010C\u001a\u00020\u001aH\u0002J\u0010\u0010L\u001a\u00020A2\u0006\u0010C\u001a\u00020\u001aH\u0002J\b\u0010M\u001a\u00020AH\u0014J\u0016\u0010N\u001a\u00020A2\f\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fH\u0002J\u0010\u0010O\u001a\u00020A2\u0006\u0010P\u001a\u00020\u001aH\u0002J\u000e\u0010Q\u001a\u00020A2\u0006\u0010R\u001a\u00020 J\u0016\u0010S\u001a\u00020A2\f\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fH\u0002J\u000e\u0010T\u001a\u00020A2\u0006\u0010P\u001a\u00020\u001aR\u001a\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0011\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0013\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00140\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00160\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R,\u0010\u0018\u001a \u0012\u001c\u0012\u001a\u0012\u0004\u0012\u00020\u001a\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0016\u0012\u0004\u0012\u00020\u001c0\u001b0\u00190\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u001e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020 0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010!\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\"0\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010#\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u00140\u00190\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010$\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u000f0%\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010(\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0%\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010\'R\u0019\u0010*\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00140%\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\'R\u001d\u0010,\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0%\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\'R)\u0010.\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u001a\u0012\n\u0012\b\u0012\u0004\u0012\u00020\"0\u000f0\u00190%\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010\'R\u0011\u00100\u001a\u00020\u00168F\u00a2\u0006\u0006\u001a\u0004\b0\u00101R\u0017\u00102\u001a\b\u0012\u0004\u0012\u00020\u00160%\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u0010\'R\u0017\u00103\u001a\b\u0012\u0004\u0012\u00020\u00160%\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010\'R/\u00104\u001a \u0012\u001c\u0012\u001a\u0012\u0004\u0012\u00020\u001a\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0016\u0012\u0004\u0012\u00020\u001c0\u001b0\u00190%\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010\'R\u0017\u00106\u001a\b\u0012\u0004\u0012\u00020\u001a0%\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u0010\'R\u001d\u00108\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u000f0%\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010\'R\u0017\u0010:\u001a\b\u0012\u0004\u0012\u00020 0%\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u0010\'R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010<\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\"0\u000f0%\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u0010\'R#\u0010>\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u00140\u00190%\u00a2\u0006\b\n\u0000\u001a\u0004\b?\u0010\'R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006V"}, d2 = {"Lcom/zexo/app/ui/screens/home/HomeViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "chatRepository", "Lcom/zexo/app/data/repository/ChatRepository;", "callRepository", "Lcom/zexo/app/data/repository/CallRepository;", "userRepository", "Lcom/zexo/app/data/repository/UserRepository;", "statusRepository", "Lcom/zexo/app/data/repository/StatusRepository;", "(Lcom/zexo/app/data/repository/AuthRepository;Lcom/zexo/app/data/repository/ChatRepository;Lcom/zexo/app/data/repository/CallRepository;Lcom/zexo/app/data/repository/UserRepository;Lcom/zexo/app/data/repository/StatusRepository;)V", "_allChats", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/zexo/app/data/model/Chat;", "_callHistory", "Lcom/zexo/app/data/model/CallRecord;", "_currentUser", "Lcom/zexo/app/data/model/User;", "_isLoading", "", "_isSearching", "_presenceMap", "", "", "Lkotlin/Pair;", "", "_searchQuery", "_searchResults", "_selectedTab", "Lcom/zexo/app/ui/navigation/HomeTab;", "_statuses", "Lcom/zexo/app/data/model/Status;", "_userProfiles", "callHistory", "Lkotlinx/coroutines/flow/StateFlow;", "getCallHistory", "()Lkotlinx/coroutines/flow/StateFlow;", "chats", "getChats", "currentUser", "getCurrentUser", "filteredChats", "getFilteredChats", "groupedStatuses", "getGroupedStatuses", "isAdmin", "()Z", "isLoading", "isSearching", "presenceMap", "getPresenceMap", "searchQuery", "getSearchQuery", "searchResults", "getSearchResults", "selectedTab", "getSelectedTab", "statuses", "getStatuses", "userProfiles", "getUserProfiles", "clearSearch", "", "getLastSeenText", "uid", "getOtherUser", "chat", "getUnreadCount", "", "isUserOnline", "loadCurrentUser", "observeCallHistory", "observeChats", "observeStatuses", "onCleared", "resolveUserProfiles", "searchUsers", "query", "selectTab", "tab", "trackPresence", "updateSearchQuery", "Companion", "app_userRelease"})
@kotlin.OptIn(markerClass = {kotlinx.coroutines.ExperimentalCoroutinesApi.class})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class HomeViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.ChatRepository chatRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.CallRepository callRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.UserRepository userRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.StatusRepository statusRepository = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "HomeViewModel";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.data.model.User> _currentUser = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.data.model.User> currentUser = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.ui.navigation.HomeTab> _selectedTab = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.navigation.HomeTab> selectedTab = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.zexo.app.data.model.Chat>> _allChats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.Chat>> chats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.zexo.app.data.model.CallRecord>> _callHistory = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.CallRecord>> callHistory = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.zexo.app.data.model.Status>> _statuses = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.Status>> statuses = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>>> _presenceMap = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>>> presenceMap = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.Map<java.lang.String, com.zexo.app.data.model.User>> _userProfiles = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, com.zexo.app.data.model.User>> userProfiles = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.zexo.app.data.model.User>> _searchResults = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.User>> searchResults = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isSearching = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isSearching = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.Chat>> filteredChats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, java.util.List<com.zexo.app.data.model.Status>>> groupedStatuses = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.ui.screens.home.HomeViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public HomeViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.ChatRepository chatRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.CallRepository callRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.UserRepository userRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.StatusRepository statusRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.data.model.User> getCurrentUser() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.navigation.HomeTab> getSelectedTab() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.Chat>> getChats() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.CallRecord>> getCallHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.Status>> getStatuses() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>>> getPresenceMap() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, com.zexo.app.data.model.User>> getUserProfiles() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getSearchQuery() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.User>> getSearchResults() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isSearching() {
        return null;
    }
    
    public final boolean isAdmin() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.zexo.app.data.model.Chat>> getFilteredChats() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, java.util.List<com.zexo.app.data.model.Status>>> getGroupedStatuses() {
        return null;
    }
    
    public final void selectTab(@org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.navigation.HomeTab tab) {
    }
    
    public final void updateSearchQuery(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
    }
    
    public final void clearSearch() {
    }
    
    private final void loadCurrentUser() {
    }
    
    private final void observeChats(java.lang.String uid) {
    }
    
    private final void observeCallHistory(java.lang.String uid) {
    }
    
    private final void observeStatuses(java.lang.String uid) {
    }
    
    private final void resolveUserProfiles(java.util.List<com.zexo.app.data.model.Chat> chats) {
    }
    
    private final void trackPresence(java.util.List<com.zexo.app.data.model.Chat> chats) {
    }
    
    private final void searchUsers(java.lang.String query) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.zexo.app.data.model.User getOtherUser(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.Chat chat) {
        return null;
    }
    
    public final int getUnreadCount(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.Chat chat) {
        return 0;
    }
    
    public final boolean isUserOnline(@org.jetbrains.annotations.NotNull()
    java.lang.String uid) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLastSeenText(@org.jetbrains.annotations.NotNull()
    java.lang.String uid) {
        return null;
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/zexo/app/ui/screens/home/HomeViewModel$Companion;", "", "()V", "TAG", "", "app_userRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}