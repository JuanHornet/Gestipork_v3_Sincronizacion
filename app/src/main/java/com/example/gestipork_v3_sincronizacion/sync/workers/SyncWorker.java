// sync/SyncWorker.java
package com.example.gestipork_v3_sincronizacion.sync.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.repo.*;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final String PREFS = "sync";

    private static final String KEY_LAST_SYNC_LOTES = "last_sync_lotes";
    private static final String KEY_LAST_SYNC_ACCIONES = "last_sync_acciones";
    private static final String KEY_LAST_SYNC_SALIDAS  = "last_sync_salidas";
    private static final String KEY_LAST_SYNC_PARIDERAS  = "last_sync_parideras";
    private static final String KEY_LAST_SYNC_CUBRICIONES= "last_sync_cubriciones";
    private static final String KEY_LAST_SYNC_ITACA      = "last_sync_itaca";
    private static final String KEY_LAST_SYNC_NOTAS = "last_sync_notas";
    private static final String KEY_LAST_SYNC_PESOS = "last_sync_pesos";
    private static final String KEY_LAST_SYNC_CONTAR = "last_sync_contar";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        DBHelper dbh = new DBHelper(ctx);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String lastSyncAcciones = prefs.getString(KEY_LAST_SYNC_ACCIONES, "1970-01-01T00:00:00");
        String lastSyncSalidas  = prefs.getString(KEY_LAST_SYNC_SALIDAS,  "1970-01-01T00:00:00");
        String lastSyncLotes = prefs.getString(KEY_LAST_SYNC_LOTES, "1970-01-01T00:00:00");
        String lastParideras   = prefs.getString(KEY_LAST_SYNC_PARIDERAS,   "1970-01-01T00:00:00");
        String lastCubriciones = prefs.getString(KEY_LAST_SYNC_CUBRICIONES, "1970-01-01T00:00:00");
        String lastItaca       = prefs.getString(KEY_LAST_SYNC_ITACA,       "1970-01-01T00:00:00");
        String lastSyncNotas = prefs.getString(KEY_LAST_SYNC_NOTAS, "1970-01-01T00:00:00");
        String lastSyncContar = prefs.getString(KEY_LAST_SYNC_CONTAR, "1970-01-01T00:00:00");
        String lastSyncPesos = prefs.getString(KEY_LAST_SYNC_PESOS, "1970-01-01T00:00:00");
        String nowIso = nowIso();

        boolean ok = true;

        // === LOTES ===


        try {
            LoteRepository lotes = new LoteRepository(dbh);

            int subidas = lotes.pushPendientes();
            Log.i(TAG, "Lotes subidos: " + subidas);

            int bajadas = lotes.pullDesde(lastSyncLotes);
            Log.i(TAG, "Lotes bajados: " + bajadas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando LOTES", e);
        }

        // si todo OK:
        prefs.edit()
                .putString(KEY_LAST_SYNC_ACCIONES, nowIso)
                .putString(KEY_LAST_SYNC_SALIDAS,  nowIso)
                .putString(KEY_LAST_SYNC_LOTES,   nowIso)
                .apply();

        // sync parideras
        try {
            ParideraRepository par = new ParideraRepository(dbh);
            Log.i(TAG, "Parideras subidas: " + par.pushPendientes());
            Log.i(TAG, "Parideras bajadas: " + par.pullDesde(lastParideras));
        } catch (Exception e) { ok=false; Log.e(TAG, "Error PARIDERAS", e); }

// sync cubriciones
        try {
            CubricionRepository cu = new CubricionRepository(dbh);
            Log.i(TAG, "Cubriciones subidas: " + cu.pushPendientes());
            Log.i(TAG, "Cubriciones bajadas: " + cu.pullDesde(lastCubriciones));
        } catch (Exception e) { ok=false; Log.e(TAG, "Error CUBRICIONES", e); }

// sync itaca
        try {
            ItacaRepository it = new ItacaRepository(dbh);
            Log.i(TAG, "Itaca subidas: " + it.pushPendientes());
            Log.i(TAG, "Itaca bajadas: " + it.pullDesde(lastItaca));
        } catch (Exception e) { ok=false; Log.e(TAG, "Error ITACA", e); }

// si todo OK, actualizar cursores:
        if (ok) {
            prefs.edit()
                    .putString(KEY_LAST_SYNC_PARIDERAS,   nowIso)
                    .putString(KEY_LAST_SYNC_CUBRICIONES, nowIso)
                    .putString(KEY_LAST_SYNC_ITACA,       nowIso)
                    .apply();
        }

        //=== CONTAR ===

        try {
            ContarRepository contar = new ContarRepository(dbh);

            int subidas = contar.pushPendientes();
            Log.i(TAG, "Contar subidos: " + subidas);

            int bajadas = contar.pullDesde(lastSyncContar);
            Log.i(TAG, "Contar bajados: " + bajadas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando CONTAR", e);
        }

// si todo OK, al guardar cursores añade:
        prefs.edit()
                // ...otros...
                .putString(KEY_LAST_SYNC_CONTAR, nowIso)
                .apply();


        //=== PESOS ===
        try {
            PesoRepository pesos = new PesoRepository(dbh);

            int subidasP = pesos.pushPendientes();
            Log.i(TAG, "Pesos subidos: " + subidasP);

            int bajadasP = pesos.pullDesde(lastSyncPesos);
            Log.i(TAG, "Pesos bajados: " + bajadasP);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando PESOS", e);
        }

// si todo OK, al guardar cursores añade:
        prefs.edit()
                // ... otros ...
                .putString(KEY_LAST_SYNC_PESOS, nowIso)
                .apply();

        // === NOTAS ===

        try {
            NotaRepository notas = new NotaRepository(dbh);
            int subNotas = notas.pushPendientes();
            Log.i(TAG, "Notas subidas: " + subNotas);

            int bajNotas = notas.pullDesde(lastSyncNotas);
            Log.i(TAG, "Notas bajadas: " + bajNotas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando NOTAS", e);
        }

// si todo OK, al guardar cursores añade:
        prefs.edit()
                // ... las otras entidades ...
                .putString(KEY_LAST_SYNC_NOTAS, nowIso)
                .apply();


        // === ACCIONES ===
        try {
            AccionRepository acciones = new AccionRepository(dbh);

            int subidas = acciones.pushPendientes();
            Log.i(TAG, "Acciones subidas: " + subidas);

            int bajadas = acciones.pullDesde(lastSyncAcciones);
            Log.i(TAG, "Acciones bajadas: " + bajadas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando ACCIONES", e);
        }

        // === SALIDAS ===
        try {
            SalidaRepository salidas = new SalidaRepository(dbh);

            int subidas = salidas.pushPendientes();
            Log.i(TAG, "Salidas subidas: " + subidas);

            int bajadas = salidas.pullDesde(lastSyncSalidas);
            Log.i(TAG, "Salidas bajadas: " + bajadas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando SALIDAS", e);
        }

        if (ok) {
            // Solo avanzamos el cursor si todo fue bien
            prefs.edit()
                    .putString(KEY_LAST_SYNC_ACCIONES, nowIso)
                    .putString(KEY_LAST_SYNC_SALIDAS,  nowIso)
                    .apply();
            Log.i(TAG, "last_sync_acciones = " + nowIso);
            Log.i(TAG, "last_sync_salidas  = " + nowIso);
            return Result.success();
        } else {
            // Si alguna parte falló, reintentamos y NO movemos la marca de tiempo
            return Result.retry();
        }
    }

    private String nowIso() {
        // ISO simple sin zona: 2025-08-21T10:15:30
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
