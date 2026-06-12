package com.zexo.app.data.repository;

import android.util.Log;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Direct Firebase Auth REST API client that bypasses the Firebase Android SDK's
 * dependency on Firebase Installations. This allows auth to work even when
 * the mobilesdk_app_id in google-services.json is invalid or the Android app
 * is not registered in the Firebase Console.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u0000 \u00172\u00020\u0001:\u0002\u0016\u0017B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0004H\u0002J\u0018\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\tH\u0002J\u000e\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0004J\u001e\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u0016\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u0004J\u0016\u0010\u0015\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u0004\u00a8\u0006\u0018"}, d2 = {"Lcom/zexo/app/data/repository/FirebaseAuthRestApi;", "", "()V", "mapErrorMessage", "", "firebaseMessage", "postRequest", "urlString", "jsonBody", "Lorg/json/JSONObject;", "sendPasswordReset", "Lcom/zexo/app/data/repository/FirebaseAuthRestApi$AuthResult;", "email", "signInToSdkWithRestResult", "", "authResult", "auth", "Lcom/google/firebase/auth/FirebaseAuth;", "(Lcom/zexo/app/data/repository/FirebaseAuthRestApi$AuthResult;Lcom/google/firebase/auth/FirebaseAuth;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "signInWithEmail", "password", "signUpWithEmail", "AuthResult", "Companion", "app_userRelease"})
public final class FirebaseAuthRestApi {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "FirebaseAuthRestApi";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String API_KEY = "AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_URL = "https://identitytoolkit.googleapis.com/v1/accounts";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SIGN_IN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String RESET_PASSWORD_URL = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SEND_VERIFICATION_URL = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXCHANGE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token?key=AIzaSyDHz9_Cw10zmF5qJvezSqUUBTTaxhq5epA";
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.data.repository.FirebaseAuthRestApi.Companion Companion = null;
    
    @javax.inject.Inject()
    public FirebaseAuthRestApi() {
        super();
    }
    
    /**
     * Sign up with email and password using Firebase Auth REST API
     */
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.data.repository.FirebaseAuthRestApi.AuthResult signUpWithEmail(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
        return null;
    }
    
    /**
     * Sign in with email and password using Firebase Auth REST API
     */
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.data.repository.FirebaseAuthRestApi.AuthResult signInWithEmail(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
        return null;
    }
    
    /**
     * Send password reset email using Firebase Auth REST API
     */
    @org.jetbrains.annotations.NotNull()
    public final com.zexo.app.data.repository.FirebaseAuthRestApi.AuthResult sendPasswordReset(@org.jetbrains.annotations.NotNull()
    java.lang.String email) {
        return null;
    }
    
    /**
     * Sign in to the Firebase Android SDK using a custom token or REST API auth result.
     * This bridges the REST API auth with the Firebase SDK so that Firestore, RTDB, etc. work.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object signInToSdkWithRestResult(@org.jetbrains.annotations.NotNull()
    com.zexo.app.data.repository.FirebaseAuthRestApi.AuthResult authResult, @org.jetbrains.annotations.NotNull()
    com.google.firebase.auth.FirebaseAuth auth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final java.lang.String postRequest(java.lang.String urlString, org.json.JSONObject jsonBody) {
        return null;
    }
    
    private final java.lang.String mapErrorMessage(java.lang.String firebaseMessage) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u001d\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001BS\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0005\u0012\b\b\u0002\u0010\b\u001a\u00020\u0005\u0012\b\b\u0002\u0010\t\u001a\u00020\u0005\u0012\b\b\u0002\u0010\n\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\fJ\t\u0010\u0017\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\u0005H\u00c6\u0003JY\u0010\u001f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\u00052\b\b\u0002\u0010\b\u001a\u00020\u00052\b\b\u0002\u0010\t\u001a\u00020\u00052\b\b\u0002\u0010\n\u001a\u00020\u00052\b\b\u0002\u0010\u000b\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010 \u001a\u00020\u00032\b\u0010!\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\"\u001a\u00020#H\u00d6\u0001J\t\u0010$\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000eR\u0011\u0010\u000b\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000eR\u0011\u0010\n\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000eR\u0011\u0010\b\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u000eR\u0011\u0010\t\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u000eR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u000e\u00a8\u0006%"}, d2 = {"Lcom/zexo/app/data/repository/FirebaseAuthRestApi$AuthResult;", "", "success", "", "uid", "", "email", "displayName", "idToken", "refreshToken", "expiresIn", "error", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getDisplayName", "()Ljava/lang/String;", "getEmail", "getError", "getExpiresIn", "getIdToken", "getRefreshToken", "getSuccess", "()Z", "getUid", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "other", "hashCode", "", "toString", "app_userRelease"})
    public static final class AuthResult {
        private final boolean success = false;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String uid = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String email = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String displayName = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String idToken = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String refreshToken = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String expiresIn = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String error = null;
        
        public final boolean component1() {
            return false;
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
        public final java.lang.String component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component6() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component7() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component8() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.zexo.app.data.repository.FirebaseAuthRestApi.AuthResult copy(boolean success, @org.jetbrains.annotations.NotNull()
        java.lang.String uid, @org.jetbrains.annotations.NotNull()
        java.lang.String email, @org.jetbrains.annotations.NotNull()
        java.lang.String displayName, @org.jetbrains.annotations.NotNull()
        java.lang.String idToken, @org.jetbrains.annotations.NotNull()
        java.lang.String refreshToken, @org.jetbrains.annotations.NotNull()
        java.lang.String expiresIn, @org.jetbrains.annotations.NotNull()
        java.lang.String error) {
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
        
        public AuthResult(boolean success, @org.jetbrains.annotations.NotNull()
        java.lang.String uid, @org.jetbrains.annotations.NotNull()
        java.lang.String email, @org.jetbrains.annotations.NotNull()
        java.lang.String displayName, @org.jetbrains.annotations.NotNull()
        java.lang.String idToken, @org.jetbrains.annotations.NotNull()
        java.lang.String refreshToken, @org.jetbrains.annotations.NotNull()
        java.lang.String expiresIn, @org.jetbrains.annotations.NotNull()
        java.lang.String error) {
            super();
        }
        
        public final boolean getSuccess() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getUid() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getEmail() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDisplayName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getIdToken() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getRefreshToken() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getExpiresIn() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getError() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/zexo/app/data/repository/FirebaseAuthRestApi$Companion;", "", "()V", "API_KEY", "", "BASE_URL", "EXCHANGE_TOKEN_URL", "RESET_PASSWORD_URL", "SEND_VERIFICATION_URL", "SIGN_IN_URL", "SIGN_UP_URL", "TAG", "app_userRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}