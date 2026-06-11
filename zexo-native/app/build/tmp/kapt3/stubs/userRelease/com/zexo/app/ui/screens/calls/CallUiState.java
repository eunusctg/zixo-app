package com.zexo.app.ui.screens.calls;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.activity.ComponentActivity;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.core.view.WindowCompat;
import com.google.firebase.database.FirebaseDatabase;
import com.zexo.app.data.model.CallRecord;
import com.zexo.app.data.model.CallSignal;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.CallRepository;
import com.zexo.app.data.repository.UserRepository;
import com.zexo.app.ui.theme.*;
import com.zexo.app.services.CallService;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\t\n\u0002\b\u0017\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001Ba\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0005\u0012\b\b\u0002\u0010\f\u001a\u00020\u0005\u0012\b\b\u0002\u0010\r\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\u0002\u0010\u0010J\t\u0010\u001a\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\bH\u00c6\u0003J\u000b\u0010\u001e\u001a\u0004\u0018\u00010\nH\u00c6\u0003J\t\u0010\u001f\u001a\u00020\u0005H\u00c6\u0003J\t\u0010 \u001a\u00020\u0005H\u00c6\u0003J\t\u0010!\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\"\u001a\u00020\u000fH\u00c6\u0003Je\u0010#\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n2\b\b\u0002\u0010\u000b\u001a\u00020\u00052\b\b\u0002\u0010\f\u001a\u00020\u00052\b\b\u0002\u0010\r\u001a\u00020\u00052\b\b\u0002\u0010\u000e\u001a\u00020\u000fH\u00c6\u0001J\u0013\u0010$\u001a\u00020\u00052\b\u0010%\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010&\u001a\u00020\'H\u00d6\u0001J\t\u0010(\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010\u0015R\u0011\u0010\u000b\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\u0015R\u0011\u0010\f\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u0015R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0015R\u0011\u0010\r\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u0015R\u0013\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019\u00a8\u0006)"}, d2 = {"Lcom/zexo/app/ui/screens/calls/CallUiState;", "", "callId", "", "isCaller", "", "isVideoCall", "status", "Lcom/zexo/app/ui/screens/calls/CallStatus;", "otherUser", "Lcom/zexo/app/data/model/User;", "isMuted", "isSpeakerOn", "isVideoEnabled", "durationSeconds", "", "(Ljava/lang/String;ZZLcom/zexo/app/ui/screens/calls/CallStatus;Lcom/zexo/app/data/model/User;ZZZJ)V", "getCallId", "()Ljava/lang/String;", "getDurationSeconds", "()J", "()Z", "getOtherUser", "()Lcom/zexo/app/data/model/User;", "getStatus", "()Lcom/zexo/app/ui/screens/calls/CallStatus;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "", "toString", "app_userRelease"})
public final class CallUiState {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String callId = null;
    private final boolean isCaller = false;
    private final boolean isVideoCall = false;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.ui.screens.calls.CallStatus status = null;
    @org.jetbrains.annotations.Nullable()
    private final com.zexo.app.data.model.User otherUser = null;
    private final boolean isMuted = false;
    private final boolean isSpeakerOn = false;
    private final boolean isVideoEnabled = false;
    private final long durationSeconds = 0L;
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    public final boolean component2() {
        return false;
    }
    
    public final boolean component3() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.ui.screens.calls.CallStatus component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.zexo.app.data.model.User component5() {
        return null;
    }
    
    public final boolean component6() {
        return false;
    }
    
    public final boolean component7() {
        return false;
    }
    
    public final boolean component8() {
        return false;
    }
    
    public final long component9() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.ui.screens.calls.CallUiState copy(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, boolean isCaller, boolean isVideoCall, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.calls.CallStatus status, @org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User otherUser, boolean isMuted, boolean isSpeakerOn, boolean isVideoEnabled, long durationSeconds) {
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
    
    public CallUiState(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, boolean isCaller, boolean isVideoCall, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.calls.CallStatus status, @org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User otherUser, boolean isMuted, boolean isSpeakerOn, boolean isVideoEnabled, long durationSeconds) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCallId() {
        return null;
    }
    
    public final boolean isCaller() {
        return false;
    }
    
    public final boolean isVideoCall() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.ui.screens.calls.CallStatus getStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.zexo.app.data.model.User getOtherUser() {
        return null;
    }
    
    public final boolean isMuted() {
        return false;
    }
    
    public final boolean isSpeakerOn() {
        return false;
    }
    
    public final boolean isVideoEnabled() {
        return false;
    }
    
    public final long getDurationSeconds() {
        return 0L;
    }
    
    public CallUiState() {
        super();
    }
}