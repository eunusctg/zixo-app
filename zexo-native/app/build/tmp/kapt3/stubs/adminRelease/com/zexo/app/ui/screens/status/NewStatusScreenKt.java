package com.zexo.app.ui.screens.status;

import androidx.compose.animation.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.navigation.NavController;
import com.zexo.app.data.model.Status;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.StatusRepository;
import com.zexo.app.ui.theme.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000F\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\u001a$\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00060\nH\u0007\u001a\u001a\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\u000fH\u0007\u001a \u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\b2\u0006\u0010\u0014\u001a\u00020\u0015H\u0007\u001a,\u0010\u0016\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u00122\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00060\n2\u0006\u0010\u0018\u001a\u00020\u0015H\u0007\u001a\u001e\u0010\u0019\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\u00152\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00060\u001bH\u0007\"\u0017\u0010\u0000\u001a\b\u0012\u0004\u0012\u00020\u00020\u0001\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0003\u0010\u0004\u00a8\u0006\u001c"}, d2 = {"gradientPresets", "", "Lcom/zexo/app/ui/screens/status/GradientPreset;", "getGradientPresets", "()Ljava/util/List;", "BackgroundColorPicker", "", "selectedIndex", "", "onSelect", "Lkotlin/Function1;", "NewStatusScreen", "navController", "Landroidx/navigation/NavController;", "viewModel", "Lcom/zexo/app/ui/screens/status/NewStatusViewModel;", "StatusPreview", "text", "", "gradientIndex", "isWhiteText", "", "StatusTextInput", "onTextChange", "enabled", "TextColorToggle", "onToggle", "Lkotlin/Function0;", "app_adminRelease"})
public final class NewStatusScreenKt {
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<com.zexo.app.ui.screens.status.GradientPreset> gradientPresets = null;
    
    @org.jetbrains.annotations.NotNull()
    public static final java.util.List<com.zexo.app.ui.screens.status.GradientPreset> getGradientPresets() {
        return null;
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void NewStatusScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavController navController, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.status.NewStatusViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void StatusPreview(@org.jetbrains.annotations.NotNull()
    java.lang.String text, int gradientIndex, boolean isWhiteText) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void StatusTextInput(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onTextChange, boolean enabled) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void TextColorToggle(boolean isWhiteText, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToggle) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void BackgroundColorPicker(int selectedIndex, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> onSelect) {
    }
}