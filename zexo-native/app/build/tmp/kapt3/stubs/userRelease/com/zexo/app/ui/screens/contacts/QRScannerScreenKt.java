package com.zexo.app.ui.screens.contacts;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavHostController;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.zexo.app.data.model.User;
import com.zexo.app.ui.navigation.Screen;
import com.zexo.app.ui.theme.*;
import java.util.concurrent.Executors;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000P\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a4\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a*\u0010\t\u001a\u00020\u00012\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\u00010\u000b2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u001a\u0010\u000e\u001a\u00020\u00012\b\b\u0002\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0003\u001a\u001a\u0010\u0013\u001a\u00020\u00012\u0006\u0010\u0014\u001a\u00020\u00152\b\b\u0002\u0010\u0016\u001a\u00020\u0017H\u0007\u001a\b\u0010\u0018\u001a\u00020\u0001H\u0003\u001a:\u0010\u0019\u001a\u00020\u00012\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u001d2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\u00010\u000b2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u00a8\u0006\u001e"}, d2 = {"AddContactDialog", "", "user", "Lcom/zexo/app/data/model/User;", "isCreatingChat", "", "onDismiss", "Lkotlin/Function0;", "onStartChat", "CameraPreview", "onBarcodeDetected", "Lkotlin/Function1;", "Lcom/google/mlkit/vision/barcode/common/Barcode;", "onError", "CornerAccent", "modifier", "Landroidx/compose/ui/Modifier;", "rotation", "", "QRScannerScreen", "navController", "Landroidx/navigation/NavHostController;", "viewModel", "Lcom/zexo/app/ui/screens/contacts/QRScannerViewModel;", "ScanFrameOverlay", "processImageProxy", "imageProxy", "Landroidx/camera/core/ImageProxy;", "barcodeScanner", "Lcom/google/mlkit/vision/barcode/BarcodeScanner;", "app_userRelease"})
public final class QRScannerScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void QRScannerScreen(@org.jetbrains.annotations.NotNull()
    androidx.navigation.NavHostController navController, @org.jetbrains.annotations.NotNull()
    com.zexo.app.ui.screens.contacts.QRScannerViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CameraPreview(kotlin.jvm.functions.Function1<? super com.google.mlkit.vision.barcode.common.Barcode, kotlin.Unit> onBarcodeDetected, kotlin.jvm.functions.Function0<kotlin.Unit> onError) {
    }
    
    @androidx.annotation.OptIn(markerClass = {androidx.camera.core.ExperimentalGetImage.class})
    private static final void processImageProxy(androidx.camera.core.ImageProxy imageProxy, com.google.mlkit.vision.barcode.BarcodeScanner barcodeScanner, kotlin.jvm.functions.Function1<? super com.google.mlkit.vision.barcode.common.Barcode, kotlin.Unit> onBarcodeDetected, kotlin.jvm.functions.Function0<kotlin.Unit> onError) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ScanFrameOverlay() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CornerAccent(androidx.compose.ui.Modifier modifier, float rotation) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AddContactDialog(com.zexo.app.data.model.User user, boolean isCreatingChat, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function0<kotlin.Unit> onStartChat) {
    }
}