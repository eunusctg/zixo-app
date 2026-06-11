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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\n\b\u0007\u0018\u0000 -2\u00020\u0001:\u0001-B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u001f\u001a\u00020 J\u0006\u0010!\u001a\u00020 J\u000e\u0010\"\u001a\u00020 2\u0006\u0010#\u001a\u00020$J\u000e\u0010%\u001a\u00020 2\u0006\u0010&\u001a\u00020$J\b\u0010\'\u001a\u00020 H\u0002J\b\u0010(\u001a\u00020 H\u0014J\u0006\u0010)\u001a\u00020 J\b\u0010*\u001a\u00020 H\u0002J\u0006\u0010+\u001a\u00020 J\b\u0010,\u001a\u00020 H\u0002R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\r0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0016R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0016R\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u001bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u001c\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0016R\u0010\u0010\u001e\u001a\u0004\u0018\u00010\u001bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006."}, d2 = {"Lcom/zexo/app/ui/screens/status/StatusViewViewModel;", "Landroidx/lifecycle/ViewModel;", "statusRepository", "Lcom/zexo/app/data/repository/StatusRepository;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "userRepository", "Lcom/zexo/app/data/repository/UserRepository;", "(Lcom/zexo/app/data/repository/StatusRepository;Lcom/zexo/app/data/repository/AuthRepository;Lcom/zexo/app/data/repository/UserRepository;)V", "_currentIndex", "Landroidx/compose/runtime/MutableState;", "", "_isLoading", "", "_progress", "", "_statuses", "", "Lcom/zexo/app/data/model/Status;", "currentIndex", "Landroidx/compose/runtime/State;", "getCurrentIndex", "()Landroidx/compose/runtime/State;", "isLoading", "progress", "getProgress", "progressJob", "Lkotlinx/coroutines/Job;", "statuses", "getStatuses", "timerJob", "goToNextStatus", "", "goToPrevStatus", "loadStatuses", "initialStatusId", "", "loadStatusesByUser", "userId", "markCurrentAsSeen", "onCleared", "pauseProgress", "resetAndStartProgress", "resumeProgress", "startProgress", "Companion", "app_adminRelease"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class StatusViewViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.StatusRepository statusRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.UserRepository userRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<java.util.List<com.zexo.app.data.model.Status>> _statuses = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.State<java.util.List<com.zexo.app.data.model.Status>> statuses = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<java.lang.Integer> _currentIndex = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.State<java.lang.Integer> currentIndex = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<java.lang.Float> _progress = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.State<java.lang.Float> progress = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState<java.lang.Boolean> _isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.State<java.lang.Boolean> isLoading = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job timerJob;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job progressJob;
    public static final long STATUS_DURATION_MS = 6000L;
    public static final long PROGRESS_STEP_MS = 30L;
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.ui.screens.status.StatusViewViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public StatusViewViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.StatusRepository statusRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.UserRepository userRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<java.util.List<com.zexo.app.data.model.Status>> getStatuses() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<java.lang.Integer> getCurrentIndex() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<java.lang.Float> getProgress() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<java.lang.Boolean> isLoading() {
        return null;
    }
    
    public final void loadStatuses(@org.jetbrains.annotations.NotNull()
    java.lang.String initialStatusId) {
    }
    
    public final void loadStatusesByUser(@org.jetbrains.annotations.NotNull()
    java.lang.String userId) {
    }
    
    public final void goToNextStatus() {
    }
    
    public final void goToPrevStatus() {
    }
    
    private final void startProgress() {
    }
    
    private final void resetAndStartProgress() {
    }
    
    private final void markCurrentAsSeen() {
    }
    
    public final void pauseProgress() {
    }
    
    public final void resumeProgress() {
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/zexo/app/ui/screens/status/StatusViewViewModel$Companion;", "", "()V", "PROGRESS_STEP_MS", "", "STATUS_DURATION_MS", "app_adminRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}