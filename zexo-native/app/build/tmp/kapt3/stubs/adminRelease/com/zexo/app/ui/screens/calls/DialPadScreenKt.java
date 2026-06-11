package com.zexo.app.ui.screens.calls;

import android.content.Intent;
import androidx.compose.animation.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.graphics.vector.ImageVector;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.navigation.NavController;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.UserRepository;
import com.zexo.app.ui.navigation.Screen;
import com.zexo.app.ui.theme.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000@\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\u001aL\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\u0010\u0004\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0006\u001a\u00020\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\t2\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\t2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\tH\u0007\u001a\u001e\u0010\f\u001a\u00020\u00012\u0006\u0010\r\u001a\u00020\u000e2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\tH\u0007\u001a@\u0010\u0010\u001a\u00020\u00012\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\t2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\t2\u0006\u0010\u0006\u001a\u00020\u0007H\u0007\u001a\u001a\u0010\u0015\u001a\u00020\u00012\u0006\u0010\u0016\u001a\u00020\u00172\b\b\u0002\u0010\u0018\u001a\u00020\u0019H\u0007\u001a$\u0010\u001a\u001a\u00020\u00012\u0006\u0010\u001b\u001a\u00020\u00072\b\u0010\u001c\u001a\u0004\u0018\u00010\u00072\b\u0010\u001d\u001a\u0004\u0018\u00010\u0007H\u0007\u001a\u0010\u0010\u001e\u001a\u00020\u00072\u0006\u0010\u001b\u001a\u00020\u0007H\u0002\u00a8\u0006\u001f"}, d2 = {"CallButtons", "", "isSearching", "", "foundUser", "Lcom/zexo/app/data/model/User;", "enteredNumber", "", "onAudioCall", "Lkotlin/Function0;", "onVideoCall", "onSearch", "DialKeyButton", "key", "Lcom/zexo/app/ui/screens/calls/DialKey;", "onClick", "DialPadGrid", "onDigitPress", "Lkotlin/Function1;", "onDelete", "onLongDelete", "DialPadScreen", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/zexo/app/ui/screens/calls/DialPadViewModel;", "NumberDisplay", "number", "userName", "userAvatar", "formatZixoNumber", "app_adminRelease"})
public final class DialPadScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void DialPadScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.calls.DialPadViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void NumberDisplay(@org.jetbrains.annotations.NotNull()
    java.lang.String number, @org.jetbrains.annotations.Nullable()
    java.lang.String userName, @org.jetbrains.annotations.Nullable()
    java.lang.String userAvatar) {
    }
    
    private static final java.lang.String formatZixoNumber(java.lang.String number) {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    public static final void DialPadGrid(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onDigitPress, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDelete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLongDelete, @org.jetbrains.annotations.NotNull()
    java.lang.String enteredNumber) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void DialKeyButton(@org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.calls.DialKey key, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void CallButtons(boolean isSearching, @org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User foundUser, @org.jetbrains.annotations.NotNull()
    java.lang.String enteredNumber, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onAudioCall, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onVideoCall, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSearch) {
    }
}