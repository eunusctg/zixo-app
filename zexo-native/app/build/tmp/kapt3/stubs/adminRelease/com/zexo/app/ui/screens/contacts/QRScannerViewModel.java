package com.zexo.app.ui.screens.contacts;

import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.ChatRepository;
import com.zexo.app.data.repository.UserRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u0000  2\u00020\u0001:\u0001 B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0014\u001a\u00020\u0015J\u0006\u0010\u0016\u001a\u00020\u0015J\u0006\u0010\u0017\u001a\u00020\u0015J\u0012\u0010\u0018\u001a\u0004\u0018\u00010\r2\u0006\u0010\u0019\u001a\u00020\rH\u0002J\u000e\u0010\u001a\u001a\u00020\u00152\u0006\u0010\u001b\u001a\u00020\rJ\u000e\u0010\u001c\u001a\u00020\u00152\u0006\u0010\u001d\u001a\u00020\u001eJ\u0006\u0010\u001f\u001a\u00020\u0015R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\u0004\u0018\u00010\r8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\u000fR\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/zexo/app/ui/screens/contacts/QRScannerViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/zexo/app/data/repository/AuthRepository;", "chatRepository", "Lcom/zexo/app/data/repository/ChatRepository;", "userRepository", "Lcom/zexo/app/data/repository/UserRepository;", "(Lcom/zexo/app/data/repository/AuthRepository;Lcom/zexo/app/data/repository/ChatRepository;Lcom/zexo/app/data/repository/UserRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/zexo/app/ui/screens/contacts/QRScannerUiState;", "currentUid", "", "getCurrentUid", "()Ljava/lang/String;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearCreatedChatId", "", "clearError", "dismissDialog", "extractUidFromQr", "content", "onQrCodeDetected", "qrContent", "setCameraReady", "ready", "", "startChatWithScannedUser", "Companion", "app_adminRelease"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class QRScannerViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.ChatRepository chatRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.zexo.app.data.repository.UserRepository userRepository = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "QRScannerViewModel";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String ZIXO_QR_PREFIX = "zixo://user/";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.zexo.app.ui.screens.contacts.QRScannerUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.contacts.QRScannerUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.ui.screens.contacts.QRScannerViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public QRScannerViewModel(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.AuthRepository authRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.ChatRepository chatRepository, @org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.UserRepository userRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.zexo.app.ui.screens.contacts.QRScannerUiState> getUiState() {
        return null;
    }
    
    private final java.lang.String getCurrentUid() {
        return null;
    }
    
    /**
     * Called when a QR code is detected. Extracts the Zixo user UID
     * from the QR content and fetches the user profile.
     */
    public final void onQrCodeDetected(@org.jetbrains.annotations.NotNull()
    java.lang.String qrContent) {
    }
    
    /**
     * Start a chat with the scanned user.
     */
    public final void startChatWithScannedUser() {
    }
    
    public final void dismissDialog() {
    }
    
    public final void clearCreatedChatId() {
    }
    
    public final void clearError() {
    }
    
    public final void setCameraReady(boolean ready) {
    }
    
    /**
     * Extract UID from QR content.
     * Expected formats:
     *  - "zixo://user/{uid}"
     *  - Just the raw UID string
     */
    private final java.lang.String extractUidFromQr(java.lang.String content) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/zexo/app/ui/screens/contacts/QRScannerViewModel$Companion;", "", "()V", "TAG", "", "ZIXO_QR_PREFIX", "app_adminRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}