package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Lote;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;
import com.example.gestipork_v3_sincronizacion.base.FechaUtils;



public class LoteRepository {

    private static final String TABLA = "lotes";
    private static final String TAG = "LoteRepository";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public LoteRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }
    // ===== CRUD simple local: actualizar solo nombre y raza =====
    public boolean actualizarNombreYRaza(Lote lote) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("nombre_lote", lote.getNombre_lote());
        v.put("raza", lote.getRaza());
        v.put("fecha_actualizacion", FechaUtils.ahoraIso());
        v.put("sincronizado", 0); // marcar para subir a Supabase

        int updated = db.update(
                TABLA,
                v,
                "id = ?",
                new String[]{ lote.getId() }
        );

        return updated > 0;
    }


    // ========= SYNC: PUSH (local -> Supabase) =========
    public int pushPendientes() {
        List<Lote> pendientes = obtenerNoSincronizados();
        if (pendientes.isEmpty()) return 0;

        JsonArray body = new JsonArray();
        for (Lote l : pendientes) {
            body.add(toJson(l)); // upsert en lote
        }

        try {
            Response<JsonArray> resp = api.insertMany(TABLA, body).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                // Marcar sincronizados por id
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i = 0; i < resp.body().size(); i++) {
                        String id = resp.body().get(i).getAsJsonObject().get("id").getAsString();
                        marcarSincronizado(db, id);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return resp.body().size();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // ========= SYNC: PULL (Supabase -> local) =========
    public int pullDesde(String isoUltimaSync) {
        Map<String, String> q = new HashMap<>();
        q.put("fecha_actualizacion", "gt." + isoUltimaSync);
        q.put("order", "fecha_actualizacion.asc");

        try {
            Response<JsonArray> resp = api.fetch(TABLA, q).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                int count = 0;
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i = 0; i < resp.body().size(); i++) {
                        JsonObject o = resp.body().get(i).getAsJsonObject();
                        Lote l = fromJson(o);
                        upsertLocal(db, l);
                        count++;
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return count;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // ========= SYNC: Delete lógico remoto =========
    public boolean marcarEliminadoEnSupabase(String id, String fechaIso) {
        Map<String, String> match = new HashMap<>();
        match.put("id", "eq." + id);

        JsonObject patch = new JsonObject();
        patch.addProperty("eliminado", 1);
        patch.addProperty("fecha_eliminado", fechaIso);
        patch.addProperty("fecha_actualizacion", fechaIso);

        try {
            Response<JsonArray> resp = api.update(TABLA, match, patch).execute();
            return resp.isSuccessful();
        } catch (Exception ignored) {}
        return false;
    }

    // ========= Helpers =========
    private List<Lote> obtenerNoSincronizados() {
        List<Lote> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_explotacion, nDisponibles, nIniciales, nombre_lote, raza, estado, color, fecha_actualizacion, eliminado, fecha_eliminado " +
                        "FROM " + TABLA + " WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                Lote l = new Lote();
                l.setId(c.getString(0));
                l.setId_explotacion(c.getString(1));
                l.setnDisponibles(c.getInt(2));
                l.setnIniciales(c.getInt(3));
                l.setNombre_lote(c.getString(4));
                l.setRaza(c.getString(5));
                l.setEstado(c.getInt(6));
                l.setColor(c.getString(7));
                l.setFecha_actualizacion(c.getString(8)); // BaseEntity
                l.setEliminado(c.getInt(9));
                l.setFecha_eliminado(c.getString(10));
                l.setSincronizado(0); // sigue siendo pendiente
                out.add(l);
            }
        }
        return out;
    }

    private void marcarSincronizado(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues();
        v.put("sincronizado", 1);
        db.update(TABLA, v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, Lote l) {
        ContentValues v = new ContentValues();
        v.put("id", l.getId());
        v.put("id_explotacion", l.getId_explotacion());
        v.put("nDisponibles", l.getnDisponibles());
        v.put("nIniciales", l.getnIniciales());
        v.put("nombre_lote", l.getNombre_lote());
        v.put("raza", l.getRaza());
        v.put("estado", l.getEstado());
        v.put("color", l.getColor());
        v.put("fecha_actualizacion", l.getFecha_actualizacion());
        v.put("sincronizado", 1); // viene de remoto
        v.put("eliminado", l.getEliminado());
        v.put("fecha_eliminado", l.getFecha_eliminado());

        int updated = db.update(TABLA, v, "id=?", new String[]{l.getId()});
        if (updated == 0) db.insert(TABLA, null, v);
    }

    private JsonObject toJson(Lote l) {
        JsonObject o = new JsonObject();
        o.addProperty("id", l.getId());
        o.addProperty("id_explotacion", l.getId_explotacion());
        o.addProperty("nDisponibles", l.getnDisponibles());
        o.addProperty("nIniciales", l.getnIniciales());
        o.addProperty("nombre_lote", l.getNombre_lote());
        o.addProperty("raza", l.getRaza());
        o.addProperty("estado", l.getEstado());
        o.addProperty("color", l.getColor());
        o.addProperty("fecha_actualizacion", l.getFecha_actualizacion());
        o.addProperty("eliminado", l.getEliminado());
        o.addProperty("fecha_eliminado", l.getFecha_eliminado());

        // Si en tu Supabase existen estas columnas, las enviamos (pueden ser null).
        // Si no existen, puedes comentar estas tres líneas:
        // o.addProperty("id_paridera",  l.getId_paridera());
        // o.addProperty("id_cubricion", l.getId_cubricion());
        // o.addProperty("id_itaca",     l.getId_itaca());

        return o;
    }

    private Lote fromJson(JsonObject o) {
        Lote l = new Lote();
        l.setId(getString(o, "id"));
        l.setId_explotacion(getString(o, "id_explotacion"));
        l.setnDisponibles(getInt(o, "nDisponibles"));
        l.setnIniciales(getInt(o, "nIniciales"));
        l.setNombre_lote(getString(o, "nombre_lote"));
        l.setRaza(getString(o, "raza"));
        l.setEstado(getInt(o, "estado"));
        l.setColor(getString(o, "color"));
        l.setFecha_actualizacion(getString(o, "fecha_actualizacion"));
        l.setEliminado(getInt(o, "eliminado"));
        l.setFecha_eliminado(getString(o, "fecha_eliminado"));
        l.setSincronizado(1);
        return l;
    }

    private String getString(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }
    private int getInt(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
    }

    // ========= QUERIES LOCALES PARA LA UI =========
    public Lote findById(String idLote) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Lote lote = null;

        // Usamos nombres de columnas coherentes con obtenerNoSincronizados/upsertLocal
        String sql = "SELECT id, id_explotacion, nDisponibles, nIniciales, " +
                "nombre_lote, raza, estado, color, fecha_actualizacion, " +
                "eliminado, fecha_eliminado, sincronizado " +
                "FROM " + TABLA + " WHERE id = ? LIMIT 1";

        try (Cursor c = db.rawQuery(sql, new String[]{idLote})) {
            if (c.moveToFirst()) {
                lote = new Lote();
                lote.setId(c.getString(c.getColumnIndexOrThrow("id")));
                lote.setId_explotacion(c.getString(c.getColumnIndexOrThrow("id_explotacion")));
                lote.setnDisponibles(c.getInt(c.getColumnIndexOrThrow("nDisponibles")));
                lote.setnIniciales(c.getInt(c.getColumnIndexOrThrow("nIniciales")));
                lote.setNombre_lote(c.getString(c.getColumnIndexOrThrow("nombre_lote")));
                lote.setRaza(c.getString(c.getColumnIndexOrThrow("raza")));
                lote.setEstado(c.getInt(c.getColumnIndexOrThrow("estado")));
                lote.setColor(c.getString(c.getColumnIndexOrThrow("color")));
                lote.setFecha_actualizacion(c.getString(c.getColumnIndexOrThrow("fecha_actualizacion")));
                lote.setEliminado(c.getInt(c.getColumnIndexOrThrow("eliminado")));
                lote.setFecha_eliminado(c.getString(c.getColumnIndexOrThrow("fecha_eliminado")));
                lote.setSincronizado(c.getInt(c.getColumnIndexOrThrow("sincronizado")));
            }
        }

        return lote;
    }

    /**
     * Eliminación lógica de un lote y todas sus tablas hijas.
     * Marca eliminado=1, fecha_eliminado=now, sincronizado=0 en:
     *  - lotes
     *  - parideras, cubriciones, itaca
     *  - acciones, salidas, alimentacion
     *  - contar, pesar, notas
     */
    public boolean eliminarLoteYHijosLogicamente(String idLote, String idExplotacion) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        String fechaEliminado = FechaUtils.ahoraIso();

        try {
            db.beginTransaction();

            // 1) Lote
            ContentValues vLote = new ContentValues();
            vLote.put("eliminado", 1);
            vLote.put("fecha_eliminado", fechaEliminado);
            vLote.put("sincronizado", 0);

            int filasLote = db.update(
                    "lotes",
                    vLote,
                    "id = ?",
                    new String[]{idLote}
            );
            android.util.Log.d("LoteRepository", "eliminarLote: filasLote=" + filasLote);

            // 2) Plantilla para tablas hijas
            ContentValues vHijo = new ContentValues();
            vHijo.put("eliminado", 1);
            vHijo.put("fecha_eliminado", fechaEliminado);
            vHijo.put("sincronizado", 0);

            String whereHijo = "id_lote = ? AND id_explotacion = ?";
            String[] argsHijo = new String[]{idLote, idExplotacion};

            int fParideras   = db.update("parideras",    vHijo, whereHijo, argsHijo);
            int fCubriciones = db.update("cubriciones",  vHijo, whereHijo, argsHijo);
            int fItaca       = db.update("itaca",        vHijo, whereHijo, argsHijo);
            int fAcciones    = db.update("acciones",     vHijo, whereHijo, argsHijo);
            int fSalidas     = db.update("salidas",      vHijo, whereHijo, argsHijo);
            int fAlim        = db.update("alimentacion", vHijo, whereHijo, argsHijo);
            int fContar      = db.update("contar",       vHijo, whereHijo, argsHijo);
            int fPesar       = db.update("pesos",        vHijo, whereHijo, argsHijo);
            int fNotas       = db.update("notas",        vHijo, whereHijo, argsHijo);

            android.util.Log.d("LoteRepository", "eliminarLote hijos: parideras=" + fParideras +
                    ", cubriciones=" + fCubriciones +
                    ", itaca=" + fItaca +
                    ", acciones=" + fAcciones +
                    ", salidas=" + fSalidas +
                    ", alim=" + fAlim +
                    ", contar=" + fContar +
                    ", pesar=" + fPesar +
                    ", notas=" + fNotas);

            db.setTransactionSuccessful();

            // Consideramos éxito si se ha podido ejecutar todo sin excepciones
            // y al menos el lote se ha marcado (o ya lo estaba).
            return filasLote >= 0;

        } catch (Exception e) {
            android.util.Log.e("LoteRepository", "Error eliminando lógicamente lote y sus hijos", e);
            return false;
        } finally {
            try {
                db.endTransaction();
            } catch (Exception ignore) {}
            db.close();
        }
    }

}



