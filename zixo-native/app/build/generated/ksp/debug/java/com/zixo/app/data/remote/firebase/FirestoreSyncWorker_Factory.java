package com.zixo.app.data.remote.firebase;

import android.content.Context;
import androidx.work.WorkManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FirestoreSyncWorker_Factory implements Factory<FirestoreSyncWorker> {
  private final Provider<Context> contextProvider;

  private final Provider<WorkManager> workManagerProvider;

  public FirestoreSyncWorker_Factory(Provider<Context> contextProvider,
      Provider<WorkManager> workManagerProvider) {
    this.contextProvider = contextProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public FirestoreSyncWorker get() {
    return newInstance(contextProvider.get(), workManagerProvider.get());
  }

  public static FirestoreSyncWorker_Factory create(Provider<Context> contextProvider,
      Provider<WorkManager> workManagerProvider) {
    return new FirestoreSyncWorker_Factory(contextProvider, workManagerProvider);
  }

  public static FirestoreSyncWorker newInstance(Context context, WorkManager workManager) {
    return new FirestoreSyncWorker(context, workManager);
  }
}
