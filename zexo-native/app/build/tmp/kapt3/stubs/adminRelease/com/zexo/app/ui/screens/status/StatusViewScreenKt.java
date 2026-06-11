package com.zexo.app.ui.screens.status;

import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.navigation.NavController;
import com.zexo.app.data.model.Status;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.StatusRepository;
import com.zexo.app.data.repository.UserRepository;
import com.zexo.app.ui.theme.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000N\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\u001an\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00072\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\nH\u0007\u001a*\u0010\u000f\u001a\u00020\u00012\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00072\u0006\u0010\u0010\u001a\u00020\u00052\b\b\u0002\u0010\u0011\u001a\u00020\u0012H\u0007\u001a\"\u0010\u0013\u001a\u00020\u00012\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00172\b\b\u0002\u0010\u0018\u001a\u00020\u0019H\u0007\u001a\u0010\u0010\u001a\u001a\u00020\u00172\u0006\u0010\u001b\u001a\u00020\u001cH\u0002\u001a\"\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\u001eH\u0002\u00f8\u0001\u0000\u00a2\u0006\u0004\b!\u0010\"\u001a\u0014\u0010#\u001a\u00020\u0012*\u00020\u00122\u0006\u0010$\u001a\u00020\u0005H\u0003\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006%"}, d2 = {"StatusContent", "", "status", "Lcom/zexo/app/data/model/Status;", "progress", "", "totalStatuses", "", "currentIndex", "onNext", "Lkotlin/Function0;", "onPrev", "onClose", "onPause", "onResume", "StatusProgressBars", "currentProgress", "modifier", "Landroidx/compose/ui/Modifier;", "StatusViewScreen", "navController", "Landroidx/navigation/NavController;", "statusId", "", "viewModel", "Lcom/zexo/app/ui/screens/status/StatusViewViewModel;", "formatTimeAgo", "timestamp", "", "parseColor", "Landroidx/compose/ui/graphics/Color;", "hex", "default", "parseColor-4WTKRHQ", "(Ljava/lang/String;J)J", "fractionalWidth", "fraction", "app_adminRelease"})
public final class StatusViewScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void StatusViewScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    java.lang.String statusId, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.status.StatusViewViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void StatusContent(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.model.Status status, float progress, int totalStatuses, int currentIndex, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNext, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onPrev, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClose, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onPause, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onResume) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void StatusProgressBars(int totalStatuses, int currentIndex, float currentProgress, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final androidx.compose.ui.Modifier fractionalWidth(androidx.compose.ui.Modifier $this$fractionalWidth, float fraction) {
        return null;
    }
    
    private static final java.lang.String formatTimeAgo(long timestamp) {
        return null;
    }
}