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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u001a\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B]\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\u0007\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\n\u001a\u00020\u0007\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\f\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\rJ\u000b\u0010\u0016\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u0017\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0007H\u00c6\u0003J\u000b\u0010\u001a\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0007H\u00c6\u0003J\u000b\u0010\u001c\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0007H\u00c6\u0003Ja\u0010\u001e\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\n\u001a\u00020\u00072\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\f\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u001f\u001a\u00020\u00072\b\u0010 \u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010!\u001a\u00020\"H\u00d6\u0001J\t\u0010#\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\t\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0013\u0010\u000b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000fR\u0011\u0010\f\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u0011R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0011R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0011R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u000fR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\n\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0011\u00a8\u0006$"}, d2 = {"Lcom/zexo/app/ui/screens/contacts/QRScannerUiState;", "", "scannedUid", "", "scannedUser", "Lcom/zexo/app/data/model/User;", "isFetchingUser", "", "isCreatingChat", "createdChatId", "showAddContactDialog", "error", "isCameraReady", "(Ljava/lang/String;Lcom/zexo/app/data/model/User;ZZLjava/lang/String;ZLjava/lang/String;Z)V", "getCreatedChatId", "()Ljava/lang/String;", "getError", "()Z", "getScannedUid", "getScannedUser", "()Lcom/zexo/app/data/model/User;", "getShowAddContactDialog", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "other", "hashCode", "", "toString", "app_adminRelease"})
public final class QRScannerUiState {
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String scannedUid = null;
    @org.jetbrains.annotations.Nullable()
    private final com.zexo.app.data.model.User scannedUser = null;
    private final boolean isFetchingUser = false;
    private final boolean isCreatingChat = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String createdChatId = null;
    private final boolean showAddContactDialog = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String error = null;
    private final boolean isCameraReady = false;
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.zexo.app.data.model.User component2() {
        return null;
    }
    
    public final boolean component3() {
        return false;
    }
    
    public final boolean component4() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    public final boolean component6() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    public final boolean component8() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.ui.screens.contacts.QRScannerUiState copy(@org.jetbrains.annotations.Nullable()
    java.lang.String scannedUid, @org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User scannedUser, boolean isFetchingUser, boolean isCreatingChat, @org.jetbrains.annotations.Nullable()
    java.lang.String createdChatId, boolean showAddContactDialog, @org.jetbrains.annotations.Nullable()
    java.lang.String error, boolean isCameraReady) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
    
    public QRScannerUiState(@org.jetbrains.annotations.Nullable()
    java.lang.String scannedUid, @org.jetbrains.annotations.Nullable()
    com.zexo.app.data.model.User scannedUser, boolean isFetchingUser, boolean isCreatingChat, @org.jetbrains.annotations.Nullable()
    java.lang.String createdChatId, boolean showAddContactDialog, @org.jetbrains.annotations.Nullable()
    java.lang.String error, boolean isCameraReady) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getScannedUid() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.zexo.app.data.model.User getScannedUser() {
        return null;
    }
    
    public final boolean isFetchingUser() {
        return false;
    }
    
    public final boolean isCreatingChat() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCreatedChatId() {
        return null;
    }
    
    public final boolean getShowAddContactDialog() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getError() {
        return null;
    }
    
    public final boolean isCameraReady() {
        return false;
    }
    
    public QRScannerUiState() {
        super();
    }
}