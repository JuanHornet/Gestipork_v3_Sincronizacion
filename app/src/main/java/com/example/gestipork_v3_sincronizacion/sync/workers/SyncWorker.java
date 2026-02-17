package com.example.gestipork_v3_sincronizacion.sync.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.repo.*;
import com.example.gestipork_v3_sincronizacion.sync.SincronizadorMembresias;
import com.example.gestipork_v3_sincronizacion.auth.SessionManager;

import java.util.Set;

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
        String lastSyncLotes    = prefs.getString(KEY_LAST_SYNC_LOTES,    "1970-01-01T00:00:00");
        String lastParideras    = prefs.getString(KEY_LAST_SYNC_PARIDERAS,"1970-01-01T00:00:00");
        String lastCubriciones  = prefs.getString(KEY_LAST_SYNC_CUBRICIONES,"1970-01-01T00:00:00");
        String lastItaca        = prefs.getString(KEY_LAST_SYNC_ITACA,    "1970-01-01T00:00:00");
        String lastSyncNotas    = prefs.getString(KEY_LAST_SYNC_NOTAS,    "1970-01-01T00:00:00");
        String lastSyncContar   = prefs.getString(KEY_LAST_SYNC_CONTAR,   "1970-01-01T00:00:00");
        String lastSyncPesos    = prefs.getString(KEY_LAST_SYNC_PESOS,    "1970-01-01T00:00:00");
        String now = FechaUtils.ahoraIso();

        boolean ok = true
                ;

        // ============ PASO 0: Membresías (obtener explotaciones autorizadas) ============
        Set<String> explotacionesAutorizadas;
        try {
            SessionManager sm = new SessionManager(ctx);
            String userId = sm.getUserId();   // Usa SIEMPRE SessionManager

            if (TextUtils.isEmpty(userId)) {
                Log.w(TAG, "No hay userId en sesión (SessionManager); se aborta sincronización.");
                return Result.retry();
            }

            SincronizadorMembresias syncMem = new SincronizadorMembresias(ctx, dbh);
            explotacionesAutorizadas = syncMem.sincronizarYObtenerExplotaciones(userId);

            if (explotacionesAutorizadas == null || explotacionesAutorizadas.isEmpty()) {
                Log.i(TAG, "Usuario sin explotaciones autorizadas; nada que sincronizar.");
                return Result.success();
            }

            Log.i(TAG, "Explotaciones autorizadas: " + explotacionesAutorizadas.size());
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error en PASO 0 (membresías)", e);
            return Result.retry();
        }
        // ================================================================================

        // === LOTES ===
        try {
            LoteRepository lotes = new LoteRepository(dbh);
            // Si has añadido sobrecargas que aceptan el set, úsalo:
            // int subidas = lotes.pushPendientes(explotacionesAutorizadas);
            // int bajadas = lotes.pullDesde(lastSyncLotes, explotacionesAutorizadas);

            int subidas = lotes.pushPendientes();
            Log.i(TAG, "Lotes subidos: " + subidas);

            int bajadas = lotes.pullDesde(lastSyncLotes);
            Log.i(TAG, "Lotes bajados: " + bajadas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando LOTES", e);
        }

        // si todo OK:
        if (ok) {
            prefs.edit()
                    .putString(KEY_LAST_SYNC_ACCIONES, now)
                    .putString(KEY_LAST_SYNC_SALIDAS,  now)
                    .putString(KEY_LAST_SYNC_LOTES,   now)
                    .apply();
        }

        // === PARIDERAS ===
        try {
            ParideraRepository par = new ParideraRepository(dbh);
            // Log.i(TAG, "Parideras subidas: " + par.pushPendientes(explotacionesAutorizadas));
            // Log.i(TAG, "Parideras bajadas: " + par.pullDesde(lastParideras, explotacionesAutorizadas));

            Log.i(TAG, "Parideras subidas: " + par.pushPendientes());
            Log.i(TAG, "Parideras bajadas: " + par.pullDesde(lastParideras));
        } catch (Exception e) { ok=false; Log.e(TAG, "Error PARIDERAS", e); }

        // === CUBRICIONES ===
        try {
            CubricionRepository cu = new CubricionRepository(dbh);
            // Log.i(TAG, "Cubriciones subidas: " + cu.pushPendientes(explotacionesAutorizadas));
            // Log.i(TAG, "Cubriciones bajadas: " + cu.pullDesde(lastCubriciones, explotacionesAutorizadas));

            Log.i(TAG, "Cubriciones subidas: " + cu.pushPendientes());
            Log.i(TAG, "Cubriciones bajadas: " + cu.pullDesde(lastCubriciones));
        } catch (Exception e) { ok=false; Log.e(TAG, "Error CUBRICIONES", e); }

        // === ITACA ===
        try {
            ItacaRepository it = new ItacaRepository(dbh);
            // Log.i(TAG, "Itaca subidas: " + it.pushPendientes(explotacionesAutorizadas));
            // Log.i(TAG, "Itaca bajadas: " + it.pullDesde(lastItaca, explotacionesAutorizadas));

            Log.i(TAG, "Itaca subidas: " + it.pushPendientes());
            Log.i(TAG, "Itaca bajadas: " + it.pullDesde(lastItaca));
        } catch (Exception e) { ok=false; Log.e(TAG, "Error ITACA", e); }

        if (ok) {
            prefs.edit()
                    .putString(KEY_LAST_SYNC_PARIDERAS,   now)
                    .putString(KEY_LAST_SYNC_CUBRICIONES, now)
                    .putString(KEY_LAST_SYNC_ITACA,       now)
                    .apply();
        }

        // === CONTAR ===
        try {
            ContarRepository contar = new ContarRepository(dbh);
            // int subidas = contar.pushPendientes(explotacionesAutorizadas);
            // int bajadas = contar.pullDesde(lastSyncContar, explotacionesAutorizadas);

            int subidas = contar.pushPendientes();
            Log.i(TAG, "Contar subidos: " + subidas);

            int bajadas = contar.pullDesde(lastSyncContar);
            Log.i(TAG, "Contar bajados: " + bajadas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando CONTAR", e);
        }

        if (ok) {
            prefs.edit()
                    .putString(KEY_LAST_SYNC_CONTAR, now)
                    .apply();
        }

        // === PESOS ===
        try {
            PesoRepository pesos = new PesoRepository(dbh);
            // int subidasP = pesos.pushPendientes(explotacionesAutorizadas);
            // int bajadasP = pesos.pullDesde(lastSyncPesos, explotacionesAutorizadas);

            int subidasP = pesos.pushPendientes();
            Log.i(TAG, "Pesos subidos: " + subidasP);

            int bajadasP = pesos.pullDesde(lastSyncPesos);
            Log.i(TAG, "Pesos bajados: " + bajadasP);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando PESOS", e);
        }

        if (ok) {
            prefs.edit()
                    .putString(KEY_LAST_SYNC_PESOS, now)
                    .apply();
        }

        // === NOTAS ===
        try {
            NotaRepository notas = new NotaRepository(dbh);
            // int subNotas = notas.pushPendientes(explotacionesAutorizadas);
            // int bajNotas = notas.pullDesde(lastSyncNotas, explotacionesAutorizadas);

            int subNotas = notas.pushPendientes();
            Log.i(TAG, "Notas subidas: " + subNotas);

            int bajNotas = notas.pullDesde(lastSyncNotas);
            Log.i(TAG, "Notas bajadas: " + bajNotas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando NOTAS", e);
        }

        if (ok) {
            prefs.edit()
                    .putString(KEY_LAST_SYNC_NOTAS, now)
                    .apply();
        }

        // === ACCIONES ===
        try {
            AccionRepository acciones = new AccionRepository(dbh);
            // int subidas = acciones.pushPendientes(explotacionesAutorizadas);
            // int bajadas = acciones.pullDesde(lastSyncAcciones, explotacionesAutorizadas);

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
            // int subidas = salidas.pushPendientes(explotacionesAutorizadas);
            // int bajadas = salidas.pullDesde(lastSyncSalidas, explotacionesAutorizadas);

            int subidas = salidas.pushPendientes();
            Log.i(TAG, "Salidas subidas: " + subidas);

            int bajadas = salidas.pullDesde(lastSyncSalidas);
            Log.i(TAG, "Salidas bajadas: " + bajadas);
        } catch (Exception e) {
            ok = false;
            Log.e(TAG, "Error sincronizando SALIDAS", e);
        }

        if (ok) {
            // Solo avanzamos cursores si todo fue bien
            prefs.edit()
                    .putString(KEY_LAST_SYNC_ACCIONES, now)
                    .putString(KEY_LAST_SYNC_SALIDAS,  now)
                    .apply();
            Log.i(TAG, "last_sync_acciones = " + now);
            Log.i(TAG, "last_sync_salidas  = " + now);
            return Result.success();
        } else {
            // Si alguna parte falló, reintentamos y NO movemos la marca de tiempo
            return Result.retry();
        }
    }
}
