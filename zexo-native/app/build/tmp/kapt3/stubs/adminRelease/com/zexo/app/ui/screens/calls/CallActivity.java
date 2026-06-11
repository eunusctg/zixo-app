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

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \b2\u00020\u0001:\u0001\bB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0002J\u0012\u0010\u0005\u001a\u00020\u00042\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007H\u0014\u00a8\u0006\t"}, d2 = {"Lcom/zexo/app/ui/screens/calls/CallActivity;", "Landroidx/activity/ComponentActivity;", "()V", "installSplashScreen", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "Companion", "app_adminRelease"})
public final class CallActivity extends androidx.activity.ComponentActivity {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_RECEIVER_ID = "receiver_id";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_CALL_ID = "call_id";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_CALLER_ID = "caller_id";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_IS_VIDEO = "is_video";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_IS_INCOMING = "is_incoming";
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.ui.screens.calls.CallActivity.Companion Companion = null;
    
    public CallActivity() {
        super(0);
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @kotlin.Suppress(names = {"DEPRECATION"})
    private final void installSplashScreen() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J&\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0010J\u001e\u0010\u0011\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u0012\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0010R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/zexo/app/ui/screens/calls/CallActivity$Companion;", "", "()V", "EXTRA_CALLER_ID", "", "EXTRA_CALL_ID", "EXTRA_IS_INCOMING", "EXTRA_IS_VIDEO", "EXTRA_RECEIVER_ID", "createIncomingIntent", "Landroid/content/Intent;", "context", "Landroid/content/Context;", "callId", "callerId", "isVideo", "", "createOutgoingIntent", "receiverId", "app_adminRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.content.Intent createOutgoingIntent(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        java.lang.String receiverId, boolean isVideo) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.content.Intent createIncomingIntent(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        java.lang.String callId, @org.jetbrains.annotations.NotNull()
        java.lang.String callerId, boolean isVideo) {
            return null;
        }
    }
}