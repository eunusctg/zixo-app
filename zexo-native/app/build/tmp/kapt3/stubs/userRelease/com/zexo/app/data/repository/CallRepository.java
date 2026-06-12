package com.zexo.app.data.repository;

import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.zexo.app.data.model.CallRecord;
import com.zexo.app.data.model.CallSignal;
import kotlinx.coroutines.flow.Flow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u0000 \"2\u00020\u0001:\u0001\"B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J$\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000bH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\f\u0010\rJ\u0016\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\tH\u0086@\u00a2\u0006\u0002\u0010\u0011J*\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u00130\b2\u0006\u0010\u0015\u001a\u00020\tH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0016\u0010\u0011J\u001a\u0010\u0017\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u00130\u00182\u0006\u0010\u0015\u001a\u00020\tJ&\u0010\u0019\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u000b0\u001a0\u00130\u00182\u0006\u0010\u0015\u001a\u00020\tJ\u0012\u0010\u001b\u001a\u0004\u0018\u00010\u00142\u0006\u0010\u001c\u001a\u00020\u001dH\u0002J$\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\u001f\u001a\u00020\u0014H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b \u0010!R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006#"}, d2 = {"Lcom/zexo/app/data/repository/CallRepository;", "", "firestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "rtdb", "Lcom/google/firebase/database/FirebaseDatabase;", "(Lcom/google/firebase/firestore/FirebaseFirestore;Lcom/google/firebase/database/FirebaseDatabase;)V", "createCallSignal", "Lkotlin/Result;", "", "signal", "Lcom/zexo/app/data/model/CallSignal;", "createCallSignal-gIAlu-s", "(Lcom/zexo/app/data/model/CallSignal;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "endCallSignal", "", "callId", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCallHistory", "", "Lcom/zexo/app/data/model/CallRecord;", "uid", "getCallHistory-gIAlu-s", "observeCallHistory", "Lkotlinx/coroutines/flow/Flow;", "observeIncomingCalls", "Lkotlin/Pair;", "parseCallRecord", "doc", "Lcom/google/firebase/firestore/DocumentSnapshot;", "saveCallRecord", "record", "saveCallRecord-gIAlu-s", "(Lcom/zexo/app/data/model/CallRecord;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_userRelease"})
public final class CallRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.firestore.FirebaseFirestore firestore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.database.FirebaseDatabase rtdb = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "CallRepository";
    @org.jetbrains.annotations.NotNull()
    public static final com.zexo.app.data.repository.CallRepository.Companion Companion = null;
    
    @javax.inject.Inject()
    public CallRepository(@org.jetbrains.annotations.NotNull()
    com.google.firebase.firestore.FirebaseFirestore firestore, @org.jetbrains.annotations.NotNull()
    com.google.firebase.database.FirebaseDatabase rtdb) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<kotlin.Pair<java.lang.String, com.zexo.app.data.model.CallSignal>>> observeIncomingCalls(@org.jetbrains.annotations.NotNull()
    java.lang.String uid) {
        return null;
    }
    
    /**
     * Real-time Firestore listener for call history.
     * Observes both calls where user is caller and where user is receiver.
     */
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.zexo.app.data.model.CallRecord>> observeCallHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String uid) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object endCallSignal(@org.jetbrains.annotations.NotNull()
    java.lang.String callId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final com.zexo.app.data.model.CallRecord parseCallRecord(com.google.firebase.firestore.DocumentSnapshot doc) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/zexo/app/data/repository/CallRepository$Companion;", "", "()V", "TAG", "", "app_userRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}