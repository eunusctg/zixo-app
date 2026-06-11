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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0016\b\u0007\u0018\u00002\u00020\u0001B\'\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0006\u0010\u001d\u001a\u00020\u001eJ\b\u0010\u001f\u001a\u00020\u001eH\u0002J\u0006\u0010 \u001a\u00020\u001eJ\u0006\u0010!\u001a\u00020\u001eJ\u001e\u0010\"\u001a\u00020\u001e2\u0006\u0010#\u001a\u00020\u000f2\u0006\u0010$\u001a\u00020\u000f2\u0006\u0010%\u001a\u00020\u0013J\u0016\u0010&\u001a\u00020\u001e2\u0006\u0010\'\u001a\u00020\u000f2\u0006\u0010%\u001a\u00020\u0013J\u0010\u0010(\u001a\u00020\u001e2\u0006\u0010)\u001a\u00020\u000fH\u0002J\u0010\u0010*\u001a\u00020\u001e2\u0006\u0010)\u001a\u00020\u000fH\u0002J\b\u0010+\u001a\u00020\u001eH\u0002J\b\u0010,\u001a\u00020\u001eH\u0002J\b\u0010-\u001a\u00020\u001eH\u0002J\b\u0010.\u001a\u00020\u001eH\u0014J\b\u0010/\u001a\u00020\u001eH\u0002J\b\u00100\u001a\u00020\u001eH\u0002J\u0006\u00101\u001a\u00020\u001eJ\u0006\u00102\u001a\u00020\u001eJ\u0006\u00103\u001a\u00020\u001eR\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0017\u001a\u0004\u0018\u00010\u0018X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\r0\u001a\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00064"}, d2 = {"Lcom/zexo/app/ui/screens/calls/CallViewModel;", "Landroidx/lifecycle/ViewModel;", "callRepository", "Lcom/zexo/app/data/repository/CallRepository;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "userRepository", "Lcom/zexo/app/data/repository/UserRepository;", "rtdb", "Lcom/google/firebase/database/FirebaseDatabase;", "(Lcom/zexo/app/data/repository/CallRepository;Lcom/zexo/app/data/repository/AuthRepository;Lcom/zexo/app/data/repository/UserRepository;Lcom/google/firebase/database/FirebaseDatabase;)V", "_uiState", "Landroidx/compose/runtime/MutableState;", "Lcom/zexo/app/ui/screens/calls/CallUiState;", "callId", "", "callStartTime", "", "isCaller", "", "otherUserId", "signalListener", "Lcom/google/firebase/database/ValueEventListener;", "timerJob", "Lkotlinx/coroutines/Job;", "uiState", "Landroidx/compose/runtime/State;", "getUiState", "()Landroidx/compose/runtime/State;", "answerCall", "", "cleanup", "declineCall", "endCall", "initIncomingCall", "existingCallId", "callerId", "isVideo", "initOutgoingCall", "receiverId", "listenForCallCancel", "cid", "listenForCallResponse", "onCallConnected", "onCallDeclined", "onCallEnded", "onCleared", "saveCallRecord", "startTimer", "toggleMute", "toggleSpeaker", "toggleVideo", "app_userRelease"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class CallViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.CallRepository callRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.UserRepository userRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.database.FirebaseDatabase rtdb = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<com.zexo.app.ui.screens.calls.CallUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.State<com.zexo.app.ui.screens.calls.CallUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String callId = "";
    private boolean isCaller = false;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String otherUserId = "";
    private long callStartTime = 0L;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job timerJob;
    @org.jetbrains.annotations.Nullable()
    private com.google.firebase.database.ValueEventListener signalListener;
    
    @javax.inject.Inject()
    public CallViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.CallRepository callRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.UserRepository userRepository, @org.jetbrains.annotations.NotNull()
    com.google.firebase.database.FirebaseDatabase rtdb) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<com.zexo.app.ui.screens.calls.CallUiState> getUiState() {
        return null;
    }
    
    public final void initOutgoingCall(@org.jetbrains.annotations.NotNull()
    java.lang.String receiverId, boolean isVideo) {
    }
    
    public final void initIncomingCall(@org.jetbrains.annotations.NotNull()
    java.lang.String existingCallId, @org.jetbrains.annotations.NotNull()
    java.lang.String callerId, boolean isVideo) {
    }
    
    private final void listenForCallResponse(java.lang.String cid) {
    }
    
    private final void listenForCallCancel(java.lang.String cid) {
    }
    
    public final void answerCall() {
    }
    
    public final void declineCall() {
    }
    
    public final void endCall() {
    }
    
    public final void toggleMute() {
    }
    
    public final void toggleSpeaker() {
    }
    
    public final void toggleVideo() {
    }
    
    private final void onCallConnected() {
    }
    
    private final void onCallDeclined() {
    }
    
    private final void onCallEnded() {
    }
    
    private final void startTimer() {
    }
    
    private final void saveCallRecord() {
    }
    
    private final void cleanup() {
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
}