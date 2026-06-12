package com.zexo.app.ui.screens.chat;

import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.zexo.app.data.model.Message;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.ChatRepository;
import com.zexo.app.data.repository.UserRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u001b\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001Bi\u0012\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\b\u0012\b\b\u0002\u0010\f\u001a\u00020\b\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u000e\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u000e\u0012\b\b\u0002\u0010\u0010\u001a\u00020\u000e\u00a2\u0006\u0002\u0010\u0011J\u000f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000b\u0010\u001e\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\t\u0010\u001f\u001a\u00020\bH\u00c6\u0003J\t\u0010 \u001a\u00020\nH\u00c6\u0003J\t\u0010!\u001a\u00020\bH\u00c6\u0003J\t\u0010\"\u001a\u00020\bH\u00c6\u0003J\u000b\u0010#\u001a\u0004\u0018\u00010\u000eH\u00c6\u0003J\t\u0010$\u001a\u00020\u000eH\u00c6\u0003J\t\u0010%\u001a\u00020\u000eH\u00c6\u0003Jm\u0010&\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\b2\b\b\u0002\u0010\f\u001a\u00020\b2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u000e2\b\b\u0002\u0010\u000f\u001a\u00020\u000e2\b\b\u0002\u0010\u0010\u001a\u00020\u000eH\u00c6\u0001J\u0013\u0010\'\u001a\u00020\b2\b\u0010(\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010)\u001a\u00020*H\u00d6\u0001J\t\u0010+\u001a\u00020\u000eH\u00d6\u0001R\u0011\u0010\u000f\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0013\u0010\r\u001a\u0004\u0018\u00010\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0013R\u0011\u0010\f\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u0015R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\u0015R\u0011\u0010\u000b\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\u0015R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0011\u0010\u0010\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0013R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001c\u00a8\u0006,"}, d2 = {"Lcom/zexo/app/ui/screens/chat/ChatUiState;", "", "messages", "", "Lcom/zexo/app/data/model/Message;", "otherUser", "Lcom/zexo/app/data/model/User;", "isOtherUserOnline", "", "otherUserLastSeen", "", "isOtherUserTyping", "isLoading", "error", "", "chatId", "otherUserId", "(Ljava/util/List;Lcom/zexo/app/data/model/User;ZJZZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getChatId", "()Ljava/lang/String;", "getError", "()Z", "getMessages", "()Ljava/util/List;", "getOtherUser", "()Lcom/zexo/app/data/model/User;", "getOtherUserId", "getOtherUserLastSeen", "()J", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "", "toString", "app_userRelease"})
public final class ChatUiState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.zexo.app.data.model.Message> messages = null;
    @org.jetbrains.annotations.Nullable()
    private final com.zexo.app.data.model.User otherUser = null;
    private final boolean isOtherUserOnline = false;
    private final long otherUserLastSeen = 0L;
    private final boolean isOtherUserTyping = false;
    private final boolean isLoading = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String error = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chatId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String otherUserId = null;
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.zexo.app.data.model.Message> component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.zexo.app.data.model.User component2() {
        return null;
    }
    
    public final boolean component3() {
        return false;
    }
    
    public final long component4() {
        return 0L;
    }
    
    public final boolean component5() {
        return false;
    }
    
    public final boolean component6() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.ui.screens.chat.ChatUiState copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.zexo.app.data.model.Message> messages, @org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User otherUser, boolean isOtherUserOnline, long otherUserLastSeen, boolean isOtherUserTyping, boolean isLoading, @org.jetbrains.annotations.Nullable()
    java.lang.String error, @org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String otherUserId) {
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
    
    public ChatUiState(@org.jetbrains.annotations.NotNull()
    java.util.List<com.zexo.app.data.model.Message> messages, @org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User otherUser, boolean isOtherUserOnline, long otherUserLastSeen, boolean isOtherUserTyping, boolean isLoading, @org.jetbrains.annotations.Nullable()
    java.lang.String error, @org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String otherUserId) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.zexo.app.data.model.Message> getMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.zexo.app.data.model.User getOtherUser() {
        return null;
    }
    
    public final boolean isOtherUserOnline() {
        return false;
    }
    
    public final long getOtherUserLastSeen() {
        return 0L;
    }
    
    public final boolean isOtherUserTyping() {
        return false;
    }
    
    public final boolean isLoading() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getError() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getChatId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOtherUserId() {
        return null;
    }
    
    public ChatUiState() {
        super();
    }
}