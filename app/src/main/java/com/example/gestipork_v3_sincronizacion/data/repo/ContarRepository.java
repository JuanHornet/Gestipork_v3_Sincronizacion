package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Contar; // usa tu modelo real
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class ContarRepository {

    private static final String TABLA = "contar";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public ContarRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }

    // ===== CRUD local (si lo necesitas) =====
    public String insert(Contar cto) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", cto.getId());
        v.put("id_lote", cto.getId_lote());
        v.put("id_explotacion", cto.getId_explotacion());
        v.put("fecha", cto.getFecha());
        v.put("nAnimales", cto.getnAnimales());
        v.put("observaciones", cto.getObservaciones());
        v.put("fecha_actualizacion", cto.getFecha_actualizacion());
        v.put("sincronizado", cto.getSincronizado());
        v.put("eliminado", cto.getEliminado());
        v.put("fecha_eliminado", cto.getFecha_eliminado());
        db.insertOrThrow(TABLA, null, v);
        return cto.getId();
    }

    // ===== PUSH (local -> Supabase) =====
    public int pushPendientes() {
        List<Contar> pendientes = obtenerNoSincronizados();
        if (pendientes.isEmpty()) return 0;

        JsonArray body = new JsonArray();
        for (Contar cto : pendientes) {
            body.add(toJson(cto)); // upsert en lote
        }

        try {
            Response<JsonArray> resp = api.insertMany(TABLA, body).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i = 0; i < resp.body().size(); i++) {
                        String id = resp.body().get(i).getAsJsonObject().get("id").getAsString();
                        marcarSincronizado(db, id);
                    }
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                return resp.body().size();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // ===== PULL (Supabase -> local) =====
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
                        Contar cto = fromJson(resp.body().get(i).getAsJsonObject());
                        upsertLocal(db, cto);
                        count++;
                    }
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                return count;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // ===== Delete lógico remoto =====
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

    // ===== Helpers =====
    private List<Contar> obtenerNoSincronizados() {
        List<Contar> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, fecha, nAnimales, observaciones, " +
                        "fecha_actualizacion, eliminado, fecha_eliminado " +
                        "FROM " + TABLA + " WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                Contar co = new Contar();
                co.setId(c.getString(0));
                co.setId_lote(c.getString(1));
                co.setId_explotacion(c.getString(2));
                co.setFecha(c.getString(3));
                co.setnAnimales(c.getInt(4));
                co.setObservaciones(c.getString(5));
                co.setFecha_actualizacion(c.getString(6));
                co.setEliminado(c.getInt(7));
                co.setFecha_eliminado(c.getString(8));
                co.setSincronizado(0);
                out.add(co);
            }
        }
        return out;
    }

    private void marcarSincronizado(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues();
        v.put("sincronizado", 1);
        db.update(TABLA, v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, Contar co) {
        ContentValues v = new ContentValues();
        v.put("id", co.getId());
        v.put("id_lote", co.getId_lote());
        v.put("id_explotacion", co.getId_explotacion());
        v.put("fecha", co.getFecha());
        v.put("nAnimales", co.getnAnimales());
        v.put("observaciones", co.getObservaciones());
        v.put("fecha_actualizacion", co.getFecha_actualizacion());
        v.put("sincronizado", 1); // viene de remoto
        v.put("eliminado", co.getEliminado());
        v.put("fecha_eliminado", co.getFecha_eliminado());

        int updated = db.update(TABLA, v, "id=?", new String[]{co.getId()});
        if (updated == 0) db.insert(TABLA, null, v);
    }

    private JsonObject toJson(Contar co) {
        JsonObject o = new JsonObject();
        o.addProperty("id", co.getId());
        o.addProperty("id_lote", co.getId_lote());
        o.addProperty("id_explotacion", co.getId_explotacion());
        o.addProperty("fecha", co.getFecha());
        o.addProperty("nAnimales", co.getnAnimales());
        o.addProperty("observaciones", co.getObservaciones());
        o.addProperty("fecha_actualizacion", co.getFecha_actualizacion());
        o.addProperty("eliminado", co.getEliminado());
        o.addProperty("fecha_eliminado", co.getFecha_eliminado());
        return o;
    }

    private Contar fromJson(JsonObject o) {
        Contar co = new Contar();
        co.setId(getS(o, "id"));
        co.setId_lote(getS(o, "id_lote"));
        co.setId_explotacion(getS(o, "id_explotacion"));
        co.setFecha(getS(o, "fecha"));
        co.setnAnimales(getI(o, "nAnimales"));
        co.setObservaciones(getS(o, "observaciones"));
        co.setFecha_actualizacion(getS(o, "fecha_actualizacion"));
        co.setEliminado(getI(o, "eliminado"));
        co.setFecha_eliminado(getS(o, "fecha_eliminado"));
        co.setSincronizado(1);
        return co;
    }

    private String getS(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }
    private int getI(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
    }
}
