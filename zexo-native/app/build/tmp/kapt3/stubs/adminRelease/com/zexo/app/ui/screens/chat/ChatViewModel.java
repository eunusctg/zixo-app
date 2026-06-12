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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0011\b\u0007\u0018\u0000 )2\u00020\u0001:\u0001)B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0018\u001a\u00020\u0019J\u0016\u0010\u001a\u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\r2\u0006\u0010\u001c\u001a\u00020\rJ\u0010\u0010\u001d\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\rH\u0002J\u0010\u0010\u001f\u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\rH\u0002J\u0010\u0010 \u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\rH\u0002J\u0010\u0010!\u001a\u00020\u00192\u0006\u0010\u001c\u001a\u00020\rH\u0002J\u0010\u0010\"\u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\rH\u0002J\b\u0010#\u001a\u00020\u0019H\u0014J\u0010\u0010$\u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\rH\u0002J\u0016\u0010%\u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\r2\u0006\u0010&\u001a\u00020\rJ\u0016\u0010\'\u001a\u00020\u00192\u0006\u0010\u001b\u001a\u00020\r2\u0006\u0010(\u001a\u00020\u0011R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\u0004\u0018\u00010\r8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006*"}, d2 = {"Lcom/zexo/app/ui/screens/chat/ChatViewModel;", "Landroidx/lifecycle/ViewModel;", "chatRepository", "Lcom/zexo/app/data/repository/ChatRepository;", "userRepository", "Lcom/zexo/app/data/repository/UserRepository;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "(Lcom/zexo/app/data/repository/ChatRepository;Lcom/zexo/app/data/repository/UserRepository;Lcom/zexo/app/data/repository/AuthRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/zexo/app/ui/screens/chat/ChatUiState;", "currentUid", "", "getCurrentUid", "()Ljava/lang/String;", "initialized", "", "typingJob", "Lkotlinx/coroutines/Job;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearError", "", "initChat", "chatId", "otherUserId", "loadOtherUserProfile", "uid", "markChatRead", "observeMessages", "observePresence", "observeTyping", "onCleared", "resolveOtherUserAndInit", "sendMessage", "text", "setTyping", "isTyping", "Companion", "app_adminRelease"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.ChatRepository chatRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.UserRepository userRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "ChatViewModel";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.ui.screens.chat.ChatUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.chat.ChatUiState> uiState = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job typingJob;
    private boolean initialized = false;
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.ui.screens.chat.ChatViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public ChatViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.ChatRepository chatRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.UserRepository userRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.chat.ChatUiState> getUiState() {
        return null;
    }
    
    private final java.lang.String getCurrentUid() {
        return null;
    }
    
    public final void initChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String otherUserId) {
    }
    
    private final void resolveOtherUserAndInit(java.lang.String chatId) {
    }
    
    private final void loadOtherUserProfile(java.lang.String uid) {
    }
    
    private final void observeMessages(java.lang.String chatId) {
    }
    
    private final void observeTyping(java.lang.String chatId) {
    }
    
    private final void observePresence(java.lang.String otherUserId) {
    }
    
    public final void sendMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void setTyping(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, boolean isTyping) {
    }
    
    private final void markChatRead(java.lang.String chatId) {
    }
    
    public final void clearError() {
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/zexo/app/ui/screens/chat/ChatViewModel$Companion;", "", "()V", "TAG", "", "app_adminRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}