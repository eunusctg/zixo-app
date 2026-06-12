package com.zexo.app.ui.screens.contacts;

import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.ChatRepository;
import com.zexo.app.data.repository.UserRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.FlowPreview;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u001e\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B\u0093\u0001\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\b\b\u0002\u0010\n\u001a\u00020\t\u0012 \b\u0002\u0010\u000b\u001a\u001a\u0012\u0004\u0012\u00020\u0003\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u000e0\r0\f\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0011\u001a\u00020\t\u0012\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0013J\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010 \u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000f\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\t\u0010#\u001a\u00020\tH\u00c6\u0003J\t\u0010$\u001a\u00020\tH\u00c6\u0003J!\u0010%\u001a\u001a\u0012\u0004\u0012\u00020\u0003\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u000e0\r0\fH\u00c6\u0003J\u000b\u0010&\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\'\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010(\u001a\u00020\tH\u00c6\u0003J\u0097\u0001\u0010)\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\t2 \b\u0002\u0010\u000b\u001a\u001a\u0012\u0004\u0012\u00020\u0003\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u000e0\r0\f2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0011\u001a\u00020\t2\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001J\u0013\u0010*\u001a\u00020\t2\b\u0010+\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010,\u001a\u00020-H\u00d6\u0001J\t\u0010.\u001a\u00020\u0003H\u00d6\u0001R\u0017\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0017R\u0013\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0017R\u0011\u0010\u0011\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u001aR\u0011\u0010\n\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u001aR\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u001aR)\u0010\u000b\u001a\u001a\u0012\u0004\u0012\u00020\u0003\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u000e0\r0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0017R\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0015\u00a8\u0006/"}, d2 = {"Lcom/zexo/app/ui/screens/contacts/NewChatUiState;", "", "searchQuery", "", "searchResults", "", "Lcom/zexo/app/data/model/User;", "allUsers", "isSearching", "", "isLoadingAll", "presenceMap", "", "Lkotlin/Pair;", "", "createdChatId", "createdChatOtherUserId", "isCreatingChat", "error", "(Ljava/lang/String;Ljava/util/List;Ljava/util/List;ZZLjava/util/Map;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;)V", "getAllUsers", "()Ljava/util/List;", "getCreatedChatId", "()Ljava/lang/String;", "getCreatedChatOtherUserId", "getError", "()Z", "getPresenceMap", "()Ljava/util/Map;", "getSearchQuery", "getSearchResults", "component1", "component10", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "", "toString", "app_adminRelease"})
public final class NewChatUiState {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.zexo.app.data.model.User> searchResults = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.zexo.app.data.model.User> allUsers = null;
    private final boolean isSearching = false;
    private final boolean isLoadingAll = false;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>> presenceMap = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String createdChatId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String createdChatOtherUserId = null;
    private final boolean isCreatingChat = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String error = null;
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.zexo.app.data.model.User> component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.zexo.app.data.model.User> component3() {
        return null;
    }
    
    public final boolean component4() {
        return false;
    }
    
    public final boolean component5() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>> component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    public final boolean component9() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.ui.screens.contacts.NewChatUiState copy(@org.jetbrains.annotations.NotNull()
    java.lang.String searchQuery, @org.jetbrains.annotations.NotNull()
    java.util.List<com.zexo.app.data.model.User> searchResults, @org.jetbrains.annotations.NotNull()
    java.util.List<com.zexo.app.data.model.User> allUsers, boolean isSearching, boolean isLoadingAll, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>> presenceMap, @org.jetbrains.annotations.Nullable()
    java.lang.String createdChatId, @org.jetbrains.annotations.Nullable()
    java.lang.String createdChatOtherUserId, boolean isCreatingChat, @org.jetbrains.annotations.Nullable()
    java.lang.String error) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
    
    public NewChatUiState(@org.jetbrains.annotations.NotNull()
    java.lang.String searchQuery, @org.jetbrains.annotations.NotNull()
    java.util.List<com.zexo.app.data.model.User> searchResults, @org.jetbrains.annotations.NotNull()
    java.util.List<com.zexo.app.data.model.User> allUsers, boolean isSearching, boolean isLoadingAll, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>> presenceMap, @org.jetbrains.annotations.Nullable()
    java.lang.String createdChatId, @org.jetbrains.annotations.Nullable()
    java.lang.String createdChatOtherUserId, boolean isCreatingChat, @org.jetbrains.annotations.Nullable()
    java.lang.String error) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSearchQuery() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.zexo.app.data.model.User> getSearchResults() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.zexo.app.data.model.User> getAllUsers() {
        return null;
    }
    
    public final boolean isSearching() {
        return false;
    }
    
    public final boolean isLoadingAll() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, kotlin.Pair<java.lang.Boolean, java.lang.Long>> getPresenceMap() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCreatedChatId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCreatedChatOtherUserId() {
        return null;
    }
    
    public final boolean isCreatingChat() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getError() {
        return null;
    }
    
    public NewChatUiState() {
        super();
    }
}