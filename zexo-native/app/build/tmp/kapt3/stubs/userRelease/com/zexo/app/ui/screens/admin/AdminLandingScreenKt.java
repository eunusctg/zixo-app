package com.zexo.app.ui.screens.admin;

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
import androidx.compose.ui.text.style.TextOverflow;
import androidx.navigation.NavHostController;
import com.zexo.app.ui.theme.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\\\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u001a\u001a\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u001aH\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\u000b2\u0006\u0010\f\u001a\u00020\b2\b\b\u0002\u0010\r\u001a\u00020\u000e2\b\b\u0002\u0010\u000f\u001a\u00020\u0010H\u0003\u001a&\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\b2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u0015H\u0003\u001a\u0018\u0010\u0016\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\bH\u0003\u001a@\u0010\u0017\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\b2\u0006\u0010\u0019\u001a\u00020\b2\u0006\u0010\u001a\u001a\u00020\b2\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\b0\u001c2\u0006\u0010\u001d\u001a\u00020\b2\b\b\u0002\u0010\u001e\u001a\u00020\u001fH\u0003\u001a*\u0010 \u001a\u00020\u00012\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020\b2\u0006\u0010$\u001a\u00020%H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b&\u0010\'\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006("}, d2 = {"AdminLandingScreen", "", "navController", "Landroidx/navigation/NavHostController;", "viewModel", "Lcom/zexo/app/ui/screens/admin/AdminViewModel;", "EditFieldCard", "label", "", "value", "onValueChange", "Lkotlin/Function1;", "hint", "singleLine", "", "maxLines", "", "FeatureItem", "index", "text", "onRemove", "Lkotlin/Function0;", "FeaturePreviewItem", "LandingPreview", "heroTitle", "heroSubtitle", "heroDescription", "features", "", "ctaText", "modifier", "Landroidx/compose/ui/Modifier;", "SectionHeader", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "title", "color", "Landroidx/compose/ui/graphics/Color;", "SectionHeader-mxwnekA", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;J)V", "app_userRelease"})
public final class AdminLandingScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void AdminLandingScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavHostController navController, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.admin.AdminViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void LandingPreview(java.lang.String heroTitle, java.lang.String heroSubtitle, java.lang.String heroDescription, java.util.List<java.lang.String> features, java.lang.String ctaText, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void FeaturePreviewItem(int index, java.lang.String text) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void EditFieldCard(java.lang.String label, java.lang.String value, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onValueChange, java.lang.String hint, boolean singleLine, int maxLines) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void FeatureItem(int index, java.lang.String text, kotlin.jvm.functions.Function0<kotlin.Unit> onRemove) {
    }
}