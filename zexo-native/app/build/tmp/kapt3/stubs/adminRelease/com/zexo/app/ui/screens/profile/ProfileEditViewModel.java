package com.zexo.app.ui.screens.profile;

import androidx.lifecycle.ViewModel;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.model.User;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\f\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0010\u001a\u00020\u0011J\u0006\u0010\u0012\u001a\u00020\u0011J\u0006\u0010\u0013\u001a\u00020\u0011J\u0006\u0010\u0014\u001a\u00020\u0011J\u000e\u0010\u0015\u001a\u00020\u00112\u0006\u0010\u0016\u001a\u00020\tJ\u000e\u0010\u0017\u001a\u00020\u00112\u0006\u0010\u0018\u001a\u00020\tJ\u000e\u0010\u0019\u001a\u00020\u00112\u0006\u0010\u001a\u001a\u00020\tJ\u000e\u0010\u001b\u001a\u00020\u00112\u0006\u0010\u001c\u001a\u00020\tR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0013\u0010\b\u001a\u0004\u0018\u00010\t8F\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000bR\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00070\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001d"}, d2 = {"Lcom/zexo/app/ui/screens/profile/ProfileEditViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "(Lcom/zexo/app/data/repository/AuthRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/zexo/app/ui/screens/profile/ProfileEditUiState;", "currentUid", "", "getCurrentUid", "()Ljava/lang/String;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearError", "", "clearSaveSuccess", "loadProfile", "saveProfile", "updateBio", "bio", "updateDisplayName", "name", "updatePhone", "phone", "updateUsername", "username", "app_adminRelease"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ProfileEditViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.ui.screens.profile.ProfileEditUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.profile.ProfileEditUiState> uiState = null;
    
    @javax.inject.Inject()
    public ProfileEditViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.profile.ProfileEditUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCurrentUid() {
        return null;
    }
    
    public final void loadProfile() {
    }
    
    public final void updateDisplayName(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void updateUsername(@org.jetbrains.annotations.NotNull()
    java.lang.String username) {
    }
    
    public final void updateBio(@org.jetbrains.annotations.NotNull()
    java.lang.String bio) {
    }
    
    public final void updatePhone(@org.jetbrains.annotations.NotNull()
    java.lang.String phone) {
    }
    
    public final void saveProfile() {
    }
    
    public final void clearError() {
    }
    
    public final void clearSaveSuccess() {
    }
}