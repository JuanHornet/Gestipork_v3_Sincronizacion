package com.example.gestipork_v3_sincronizacion.sync;

import android.content.Context;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.gestipork_v3_sincronizacion.sync.workers.SyncWorker;

public class SincronizadorManual {

    public static void sincronizarAhora(Context ctx) {
        OneTimeWorkRequest req =
                new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .addTag("sync-manual")
                        .build();

        WorkManager.getInstance(ctx).enqueue(req);
    }
}
