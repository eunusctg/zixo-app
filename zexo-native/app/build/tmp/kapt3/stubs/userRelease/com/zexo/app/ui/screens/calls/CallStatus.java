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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lcom/zexo/app/ui/screens/calls/CallStatus;", "", "(Ljava/lang/String;I)V", "RINGING", "CONNECTING", "CONNECTED", "ENDED", "DECLINED", "MISSED", "app_userRelease"})
public enum CallStatus {
    /*public static final*/ RINGING /* = new RINGING() */,
    /*public static final*/ CONNECTING /* = new CONNECTING() */,
    /*public static final*/ CONNECTED /* = new CONNECTED() */,
    /*public static final*/ ENDED /* = new ENDED() */,
    /*public static final*/ DECLINED /* = new DECLINED() */,
    /*public static final*/ MISSED /* = new MISSED() */;
    
    CallStatus() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.zexo.app.ui.screens.calls.CallStatus> getEntries() {
        return null;
    }
}