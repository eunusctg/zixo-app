package com.zixo.app.data.sync;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zixo.app.data.local.room.dao.ContactDao;
import com.zixo.app.data.local.room.dao.MessageDao;
import com.zixo.app.data.local.room.dao.StatusDao;
import com.zixo.app.data.local.room.dao.UserDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SyncWorker_Factory {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<ContactDao> contactDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<StatusDao> statusDaoProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<ConflictResolver> conflictResolverProvider;

  public SyncWorker_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseAuth> firebaseAuthProvider, Provider<ContactDao> contactDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<StatusDao> statusDaoProvider,
      Provider<UserDao> userDaoProvider, Provider<ConflictResolver> conflictResolverProvider) {
    this.firestoreProvider = firestoreProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.contactDaoProvider = contactDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.statusDaoProvider = statusDaoProvider;
    this.userDaoProvider = userDaoProvider;
    this.conflictResolverProvider = conflictResolverProvider;
  }

  public SyncWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, firestoreProvider.get(), firebaseAuthProvider.get(), contactDaoProvider.get(), messageDaoProvider.get(), statusDaoProvider.get(), userDaoProvider.get(), conflictResolverProvider.get());
  }

  public static SyncWorker_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseAuth> firebaseAuthProvider, Provider<ContactDao> contactDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<StatusDao> statusDaoProvider,
      Provider<UserDao> userDaoProvider, Provider<ConflictResolver> conflictResolverProvider) {
    return new SyncWorker_Factory(firestoreProvider, firebaseAuthProvider, contactDaoProvider, messageDaoProvider, statusDaoProvider, userDaoProvider, conflictResolverProvider);
  }

  public static SyncWorker newInstance(Context context, WorkerParameters params,
      FirebaseFirestore firestore, FirebaseAuth firebaseAuth, ContactDao contactDao,
      MessageDao messageDao, StatusDao statusDao, UserDao userDao,
      ConflictResolver conflictResolver) {
    return new SyncWorker(context, params, firestore, firebaseAuth, contactDao, messageDao, statusDao, userDao, conflictResolver);
  }
}
