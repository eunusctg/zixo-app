package com.zexo.app.ui.screens.chat;

import android.content.Intent;
import android.text.format.DateUtils;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.navigation.NavHostController;
import com.zexo.app.data.model.Message;
import com.zexo.app.ui.navigation.Screen;
import com.zexo.app.ui.screens.calls.CallActivity;
import com.zexo.app.ui.theme.*;
import com.zexo.app.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000J\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\n\u001a,\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\bH\u0007\u001ab\u0010\t\u001a\u00020\u00012\b\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u00102\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u0012H\u0003\u001a\b\u0010\u0016\u001a\u00020\u0001H\u0003\u001a\u0018\u0010\u0017\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\rH\u0003\u001aN\u0010\u001b\u001a\u00020\u00012\u0006\u0010\u001c\u001a\u00020\u00052\u0012\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00010\u001e2\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00010\u0012H\u0003\u001a\u0010\u0010\"\u001a\u00020\u00012\u0006\u0010#\u001a\u00020\u0005H\u0003\u001a\u0010\u0010$\u001a\u00020\u00052\u0006\u0010\u000f\u001a\u00020\u0010H\u0002\u001a\u0017\u0010%\u001a\u00020\u00052\b\u0010&\u001a\u0004\u0018\u00010\u0010H\u0002\u00a2\u0006\u0002\u0010\'\u00a8\u0006("}, d2 = {"ChatScreen", "", "navController", "Landroidx/navigation/NavHostController;", "chatId", "", "otherUserId", "viewModel", "Lcom/zexo/app/ui/screens/chat/ChatViewModel;", "ChatTopBar", "otherUser", "Lcom/zexo/app/data/model/User;", "isOnline", "", "isTyping", "lastSeen", "", "onBackClick", "Lkotlin/Function0;", "onAvatarClick", "onAudioCallClick", "onVideoCallClick", "EmptyChatState", "MessageBubble", "message", "Lcom/zexo/app/data/model/Message;", "isFromMe", "MessageInputBar", "text", "onTextChange", "Lkotlin/Function1;", "onSendClick", "onAttachmentClick", "onVoiceClick", "MessageStatusIcon", "status", "formatLastSeen", "formatMessageTime", "timestamp", "(Ljava/lang/Long;)Ljava/lang/String;", "app_adminRelease"})
public final class ChatScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ChatScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavHostController navController, @org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String otherUserId, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.chat.ChatViewModel viewModel) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    private static final void ChatTopBar(com.zexo.app.data.model.User otherUser, boolean isOnline, boolean isTyping, long lastSeen, kotlin.jvm.functions.Function0<kotlin.Unit> onBackClick, kotlin.jvm.functions.Function0<kotlin.Unit> onAvatarClick, kotlin.jvm.functions.Function0<kotlin.Unit> onAudioCallClick, kotlin.jvm.functions.Function0<kotlin.Unit> onVideoCallClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void MessageBubble(com.zexo.app.data.model.Message message, boolean isFromMe) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void MessageStatusIcon(java.lang.String status) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void MessageInputBar(java.lang.String text, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onTextChange, kotlin.jvm.functions.Function0<kotlin.Unit> onSendClick, kotlin.jvm.functions.Function0<kotlin.Unit> onAttachmentClick, kotlin.jvm.functions.Function0<kotlin.Unit> onVoiceClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void EmptyChatState() {
    }
    
    private static final java.lang.String formatMessageTime(java.lang.Long timestamp) {
        return null;
    }
    
    private static final java.lang.String formatLastSeen(long lastSeen) {
        return null;
    }
}