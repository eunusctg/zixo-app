package com.zixo.app.di;

import android.content.Context;
import androidx.work.WorkManager;
import com.zixo.app.data.remote.firebase.FirestoreSyncWorker;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideFirestoreSyncWorkerFactory implements Factory<FirestoreSyncWorker> {
  private final Provider<Context> contextProvider;

  private final Provider<WorkManager> workManagerProvider;

  public AppModule_ProvideFirestoreSyncWorkerFactory(Provider<Context> contextProvider,
      Provider<WorkManager> workManagerProvider) {
    this.contextProvider = contextProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public FirestoreSyncWorker get() {
    return provideFirestoreSyncWorker(contextProvider.get(), workManagerProvider.get());
  }

  public static AppModule_ProvideFirestoreSyncWorkerFactory create(
      Provider<Context> contextProvider, Provider<WorkManager> workManagerProvider) {
    return new AppModule_ProvideFirestoreSyncWorkerFactory(contextProvider, workManagerProvider);
  }

  public static FirestoreSyncWorker provideFirestoreSyncWorker(Context context,
      WorkManager workManager) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFirestoreSyncWorker(context, workManager));
  }
}
