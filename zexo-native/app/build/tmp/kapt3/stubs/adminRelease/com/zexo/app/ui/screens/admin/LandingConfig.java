package com.zexo.app.ui.screens.admin;

import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zexo.app.data.model.AdminConfig;
import com.zexo.app.data.model.User;
import com.zexo.app.data.repository.AuthRepository;
import com.zexo.app.data.repository.UserRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B=\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00030\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00030\u0007H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003JA\u0010\u0016\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00030\u00072\b\b\u0002\u0010\b\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001a\u001a\u00020\u001bH\u00d6\u0001J\t\u0010\u001c\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00030\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000b\u00a8\u0006\u001d"}, d2 = {"Lcom/zexo/app/ui/screens/admin/LandingConfig;", "", "heroTitle", "", "heroSubtitle", "heroDescription", "features", "", "ctaText", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;)V", "getCtaText", "()Ljava/lang/String;", "getFeatures", "()Ljava/util/List;", "getHeroDescription", "getHeroSubtitle", "getHeroTitle", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "", "toString", "app_adminRelease"})
public final class LandingConfig {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String heroTitle = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String heroSubtitle = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String heroDescription = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> features = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String ctaText = null;
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.ui.screens.admin.LandingConfig copy(@org.jetbrains.annotations.NotNull()
    java.lang.String heroTitle, @org.jetbrains.annotations.NotNull()
    java.lang.String heroSubtitle, @org.jetbrains.annotations.NotNull()
    java.lang.String heroDescription, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> features, @org.jetbrains.annotations.NotNull()
    java.lang.String ctaText) {
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
    
    public LandingConfig(@org.jetbrains.annotations.NotNull()
    java.lang.String heroTitle, @org.jetbrains.annotations.NotNull()
    java.lang.String heroSubtitle, @org.jetbrains.annotations.NotNull()
    java.lang.String heroDescription, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> features, @org.jetbrains.annotations.NotNull()
    java.lang.String ctaText) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHeroTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHeroSubtitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHeroDescription() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getFeatures() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCtaText() {
        return null;
    }
    
    public LandingConfig() {
        super();
    }
}