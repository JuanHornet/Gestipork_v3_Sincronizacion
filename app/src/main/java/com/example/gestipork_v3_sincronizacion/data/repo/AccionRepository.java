// data/repo/AccionRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Accion;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class AccionRepository {
    private final DBHelper dbh;
    private final GenericSupabaseService api;
    private final Gson gson = new Gson();

    public AccionRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }

    // ====== CRUD LOCAL BÁSICO ======
    public String insert(Accion a) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", a.getId());
        v.put("id_lote", a.getId_lote());
        v.put("id_explotacion", a.getId_explotacion());
        v.put("tipo", a.getTipo());
        v.put("fecha", a.getFecha());
        v.put("cantidad", a.getCantidad());
        v.put("observaciones", a.getObservaciones());
        v.put("fecha_actualizacion", a.getFecha_actualizacion());
        v.put("sincronizado", a.getSincronizado());
        v.put("eliminado", a.getEliminado());
        v.put("fecha_eliminado", a.getFecha_eliminado());
        db.insertOrThrow("acciones", null, v);
        return a.getId();
    }

    // ====== SYNC: PUSH (local -> Supabase) ======
    public int pushPendientes() {
        List<Accion> pendientes = obtenerNoSincronizadas();
        if (pendientes.isEmpty()) return 0;

        // Empaquetar en un array JSON (upsert en lote)
        JsonArray body = new JsonArray();
        for (Accion a : pendientes) {
            body.add(accionToJson(a));
        }

        try {
            Response<JsonArray> resp = api.insertMany("acciones", body).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                // Marcar sincronizadas
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i = 0; i < resp.body().size(); i++) {
                        String id = resp.body().get(i).getAsJsonObject().get("id").getAsString();
                        marcarSincronizada(db, id);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return resp.body().size();
            } else {
                // Log (resp.code, resp.errorBody)
                return 0;
            }
        } catch (Exception e) {
            // Log excepción
            return 0;
        }
    }

    // ====== SYNC: PULL (Supabase -> local) ======
    public int pullDesde(String isoUltimaSync) {
        Map<String, String> q = new HashMap<>();
        q.put("fecha_actualizacion", "gt." + isoUltimaSync);
        q.put("order", "fecha_actualizacion.asc");

        try {
            Response<JsonArray> resp = api.fetch("acciones", q).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                int count = 0;
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i = 0; i < resp.body().size(); i++) {
                        JsonObject o = resp.body().get(i).getAsJsonObject();
                        Accion a = jsonToAccion(o);
                        upsertLocal(db, a);
                        count++;
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return count;
            } else {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // ====== SYNC: Delete lógico remoto ======
    public boolean marcarEliminadoEnSupabase(String id, String fechaIso) {
        Map<String, String> match = new HashMap<>();
        match.put("id", "eq." + id);

        JsonObject patch = new JsonObject();
        patch.addProperty("eliminado", 1);
        patch.addProperty("fecha_eliminado", fechaIso);
        patch.addProperty("fecha_actualizacion", fechaIso);

        try {
            Response<JsonArray> resp = api.update("acciones", match, patch).execute();
            return resp.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    // ====== Helpers ======
    private List<Accion> obtenerNoSincronizadas() {
        List<Accion> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, tipo, fecha, cantidad, observaciones, fecha_actualizacion, sincronizado, eliminado, fecha_eliminado " +
                        "FROM acciones WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                Accion a = new Accion();
                a.setId(c.getString(0));
                a.setId_lote(c.getString(1));
                a.setId_explotacion(c.getString(2));
                a.setTipo(c.getString(3));
                a.setFecha(c.getString(4));
                a.setCantidad(c.getInt(5));
                a.setObservaciones(c.getString(6));
                a.setFecha_actualizacion(c.getString(7));
                a.setSincronizado(c.getInt(8));
                a.setEliminado(c.getInt(9));
                a.setFecha_eliminado(c.getString(10));
                out.add(a);
            }
        }
        return out;
    }

    private void marcarSincronizada(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues();
        v.put("sincronizado", 1);
        db.update("acciones", v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, Accion a) {
        ContentValues v = new ContentValues();
        v.put("id", a.getId());
        v.put("id_lote", a.getId_lote());
        v.put("id_explotacion", a.getId_explotacion());
        v.put("tipo", a.getTipo());
        v.put("fecha", a.getFecha());
        v.put("cantidad", a.getCantidad());
        v.put("observaciones", a.getObservaciones());
        v.put("fecha_actualizacion", a.getFecha_actualizacion());
        v.put("sincronizado", 1); // viene de remoto
        v.put("eliminado", a.getEliminado());
        v.put("fecha_eliminado", a.getFecha_eliminado());

        int updated = db.update("acciones", v, "id=?", new String[]{a.getId()});
        if (updated == 0) db.insert("acciones", null, v);
    }

    private JsonObject accionToJson(Accion a) {
        JsonObject o = new JsonObject();
        o.addProperty("id", a.getId());
        o.addProperty("id_lote", a.getId_lote());
        o.addProperty("id_explotacion", a.getId_explotacion());
        o.addProperty("tipo", a.getTipo());
        o.addProperty("fecha", a.getFecha());
        o.addProperty("cantidad", a.getCantidad());
        o.addProperty("observaciones", a.getObservaciones());
        o.addProperty("fecha_actualizacion", a.getFecha_actualizacion());
        o.addProperty("eliminado", a.getEliminado());
        o.addProperty("fecha_eliminado", a.getFecha_eliminado());
        return o;
    }

    private Accion jsonToAccion(JsonObject o) {
        // Usa GSON directo si quieres: return gson.fromJson(o, Accion.class);
        Accion a = new Accion();
        a.setId(getString(o, "id"));
        a.setId_lote(getString(o, "id_lote"));
        a.setId_explotacion(getString(o, "id_explotacion"));
        a.setTipo(getString(o, "tipo"));
        a.setFecha(getString(o, "fecha"));
        a.setCantidad(getInt(o, "cantidad"));
        a.setObservaciones(getString(o, "observaciones"));
        a.setFecha_actualizacion(getString(o, "fecha_actualizacion"));
        a.setEliminado(getInt(o, "eliminado"));
        a.setFecha_eliminado(getString(o, "fecha_eliminado"));
        a.setSincronizado(1);
        return a;
    }

    private String getString(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }
    private int getInt(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
    }
}
