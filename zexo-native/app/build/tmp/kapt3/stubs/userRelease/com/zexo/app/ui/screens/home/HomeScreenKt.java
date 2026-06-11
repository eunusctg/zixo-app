package com.zexo.app.ui.screens.home;

import androidx.compose.animation.*;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material.icons.outlined.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.graphics.vector.ImageVector;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.navigation.NavHostController;
import com.zexo.app.data.model.CallRecord;
import com.zexo.app.data.model.Chat;
import com.zexo.app.data.model.Status;
import com.zexo.app.data.model.User;
import com.zexo.app.ui.navigation.HomeTab;
import com.zexo.app.ui.navigation.Screen;
import com.zexo.app.ui.theme.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000t\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0014\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\t\n\u0000\u001a<\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u0006\u0010\t\u001a\u00020\nH\u0003\u001a\u001e\u0010\u000b\u001a\u00020\u00012\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u001e\u0010\u000f\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u0018\u0010\u0010\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\t\u001a\u00020\nH\u0003\u001a\u001e\u0010\u0013\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a4\u0010\u0014\u001a\u00020\u00012\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0011\u001a\u00020\u00122\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u0018\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\t\u001a\u00020\nH\u0003\u001a \u0010\u0019\u001a\u00020\u00012\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u001dH\u0003\u001aD\u0010\u001f\u001a\u00020\u00012\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010 \u001a\u00020\u001d2\b\b\u0002\u0010!\u001a\u00020\"2\b\b\u0002\u0010#\u001a\u00020\"2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b$\u0010%\u001a\u001a\u0010&\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u0011\u001a\u00020\u0012H\u0007\u001ah\u0010\'\u001a\u00020\u00012\b\u0010(\u001a\u0004\u0018\u00010)2\u0006\u0010*\u001a\u00020\u001d2\u0012\u0010+\u001a\u000e\u0012\u0004\u0012\u00020\u001d\u0012\u0004\u0012\u00020\u00010,2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u0006\u0010.\u001a\u00020\u00052\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u0006\u00100\u001a\u00020\u0005H\u0003\u001a\u001e\u00101\u001a\u00020\u00012\u0006\u00102\u001a\u00020\u00052\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a&\u00103\u001a\u00020\u00012\u0006\u00104\u001a\u00020)2\u0006\u00105\u001a\u00020\u00052\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\b\u00106\u001a\u00020\u0001H\u0003\u001a8\u00107\u001a\u00020\u00012\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010 \u001a\u00020\u001d2\u0006\u00108\u001a\u00020\"2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b9\u0010:\u001a.\u0010;\u001a\u00020\u00012\b\u00104\u001a\u0004\u0018\u00010)2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010<\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u0018\u0010=\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\t\u001a\u00020\nH\u0003\u001a\u0018\u0010>\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\t\u001a\u00020\nH\u0003\u001a:\u0010?\u001a\u00020\u00012\f\u0010@\u001a\b\u0012\u0004\u0012\u00020B0A2\u0006\u00105\u001a\u00020\u00052\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a&\u0010C\u001a\u00020\u00012\u0006\u0010D\u001a\u00020\u00032\u0006\u0010E\u001a\u00020\u00052\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a$\u0010F\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010G\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010,H\u0003\u001a\n\u0010H\u001a\u0004\u0018\u00010\u001dH\u0003\u001a\u0010\u0010I\u001a\u00020\u001d2\u0006\u0010\f\u001a\u00020\rH\u0002\u001a\u0010\u0010J\u001a\u00020\u001d2\u0006\u0010K\u001a\u00020LH\u0002\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006M"}, d2 = {"AnimatedFab", "", "selectedTab", "Lcom/zexo/app/ui/navigation/HomeTab;", "expanded", "", "onExpandToggle", "Lkotlin/Function0;", "onDismiss", "navController", "Landroidx/navigation/NavHostController;", "CallItem", "record", "Lcom/zexo/app/data/model/CallRecord;", "onClick", "CallsFabMenu", "CallsTabContent", "viewModel", "Lcom/zexo/app/ui/screens/home/HomeViewModel;", "ChatFabMenu", "ChatItem", "chat", "Lcom/zexo/app/data/model/Chat;", "onAvatarClick", "ChatsTabContent", "EmptyState", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "title", "", "subtitle", "FabMenuItem", "label", "backgroundColor", "Landroidx/compose/ui/graphics/Color;", "iconTint", "FabMenuItem-OoHUuok", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;JJLkotlin/jvm/functions/Function0;)V", "HomeScreen", "HomeTopBar", "currentUser", "Lcom/zexo/app/data/model/User;", "searchQuery", "onSearchQueryChange", "Lkotlin/Function1;", "onClearSearch", "isSearching", "onAdminClick", "isAdmin", "MyStatusItem", "hasStatus", "SearchUserItem", "user", "isOnline", "SettingsDivider", "SettingsItem", "tint", "SettingsItem-9LQNqLg", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;JLkotlin/jvm/functions/Function0;)V", "SettingsProfileCard", "onEditClick", "SettingsTabContent", "StatusTabContent", "UserStatusItem", "statuses", "", "Lcom/zexo/app/data/model/Status;", "ZexoTabItem", "tab", "selected", "ZexoTabRow", "onTabSelected", "currentUserUid", "formatCallInfo", "formatTimestamp", "ts", "", "app_userRelease"})
public final class HomeScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void HomeScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavHostController navController, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.home.HomeViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void HomeTopBar(com.zexo.app.data.model.User currentUser, java.lang.String searchQuery, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onSearchQueryChange, kotlin.jvm.functions.Function0<kotlin.Unit> onClearSearch, boolean isSearching, kotlin.jvm.functions.Function0<kotlin.Unit> onAvatarClick, kotlin.jvm.functions.Function0<kotlin.Unit> onAdminClick, boolean isAdmin) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ZexoTabRow(com.zexo.app.ui.navigation.HomeTab selectedTab, kotlin.jvm.functions.Function1<? super com.zexo.app.ui.navigation.HomeTab, kotlin.Unit> onTabSelected) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ZexoTabItem(com.zexo.app.ui.navigation.HomeTab tab, boolean selected, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AnimatedFab(com.zexo.app.ui.navigation.HomeTab selectedTab, boolean expanded, kotlin.jvm.functions.Function0<kotlin.Unit> onExpandToggle, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, androidx.navigation.NavHostController navController) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ChatFabMenu(androidx.navigation.NavHostController navController, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CallsFabMenu(androidx.navigation.NavHostController navController, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ChatsTabContent(com.zexo.app.ui.screens.home.HomeViewModel viewModel, androidx.navigation.NavHostController navController) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ChatItem(com.zexo.app.data.model.Chat chat, com.zexo.app.ui.screens.home.HomeViewModel viewModel, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, kotlin.jvm.functions.Function0<kotlin.Unit> onAvatarClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SearchUserItem(com.zexo.app.data.model.User user, boolean isOnline, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void StatusTabContent(com.zexo.app.ui.screens.home.HomeViewModel viewModel, androidx.navigation.NavHostController navController) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void MyStatusItem(boolean hasStatus, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void UserStatusItem(java.util.List<com.zexo.app.data.model.Status> statuses, boolean isOnline, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, kotlin.jvm.functions.Function0<kotlin.Unit> onAvatarClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CallsTabContent(com.zexo.app.ui.screens.home.HomeViewModel viewModel, androidx.navigation.NavHostController navController) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CallItem(com.zexo.app.data.model.CallRecord record, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SettingsTabContent(com.zexo.app.ui.screens.home.HomeViewModel viewModel, androidx.navigation.NavHostController navController) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SettingsProfileCard(com.zexo.app.data.model.User user, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, kotlin.jvm.functions.Function0<kotlin.Unit> onEditClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SettingsDivider() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void EmptyState(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title, java.lang.String subtitle) {
    }
    
    private static final java.lang.String formatTimestamp(long ts) {
        return null;
    }
    
    private static final java.lang.String formatCallInfo(com.zexo.app.data.model.CallRecord record) {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    private static final java.lang.String currentUserUid() {
        return null;
    }
}