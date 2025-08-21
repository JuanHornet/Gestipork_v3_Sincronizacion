package com.example.gestipork_v3_sincronizacion.sync.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        // TODO: orquestar subida/bajada por entidad (usuarios, explotaciones, etc.)
        return Result.success();
    }

    public static void planificar(Context ctx) {
        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "sync_general", ExistingPeriodicWorkPolicy.UPDATE, req);
    }
}

