package com.zexo.app.ui.screens.admin;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.navigation.NavHostController;
import com.zexo.app.data.model.User;
import com.zexo.app.ui.theme.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000>\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0006\u001a\u001a\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u001a8\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\u000eH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u000f\u0010\u0010\u001aP\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u000e2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\u000eH\u0003\u001a&\u0010\u001a\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\u000eH\u0003\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u001b"}, d2 = {"AdminUsersScreen", "", "navController", "Landroidx/navigation/NavHostController;", "viewModel", "Lcom/zexo/app/ui/screens/admin/AdminViewModel;", "DialogActionButton", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "text", "", "color", "Landroidx/compose/ui/graphics/Color;", "onClick", "Lkotlin/Function0;", "DialogActionButton-9LQNqLg", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;JLkotlin/jvm/functions/Function0;)V", "UserDetailDialog", "user", "Lcom/zexo/app/data/model/User;", "isDisabled", "", "onDismiss", "onToggleAdmin", "onToggleDisabled", "onDelete", "UserListItem", "app_adminRelease"})
public final class AdminUsersScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void AdminUsersScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavHostController navController, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.admin.AdminViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void UserListItem(com.zexo.app.data.model.User user, boolean isDisabled, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void UserDetailDialog(com.zexo.app.data.model.User user, boolean isDisabled, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function0<kotlin.Unit> onToggleAdmin, kotlin.jvm.functions.Function0<kotlin.Unit> onToggleDisabled, kotlin.jvm.functions.Function0<kotlin.Unit> onDelete) {
    }
}