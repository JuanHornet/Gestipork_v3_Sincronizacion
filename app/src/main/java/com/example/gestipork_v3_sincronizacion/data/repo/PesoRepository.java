package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Peso;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class PesoRepository {

    private static final String TABLA = "pesos";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public PesoRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }

    // ========= CRUD LOCAL BÁSICO =========
    public String insert(Peso p) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", p.getId());
        v.put("id_lote", p.getId_lote());
        v.put("id_explotacion", p.getId_explotacion());
        v.put("fecha", p.getFecha());
        v.put("nAnimales", p.getnAnimales());
        v.put("pesoTotal", p.getPesoTotal());
        v.put("pesoMedio", p.getPesoMedio());
        v.put("fecha_actualizacion", p.getFecha_actualizacion());
        v.put("sincronizado", p.getSincronizado());
        v.put("eliminado", p.getEliminado());
        v.put("fecha_eliminado", p.getFecha_eliminado());
        db.insertOrThrow(TABLA, null, v);
        return p.getId();
    }

    // ========= SYNC: PUSH (local -> Supabase) =========
    public int pushPendientes() {
        List<Peso> pendientes = obtenerNoSincronizados();
        if (pendientes.isEmpty()) return 0;

        JsonArray body = new JsonArray();
        for (Peso p : pendientes) body.add(toJson(p));

        try {
            Response<JsonArray> resp = api.insertMany(TABLA, body).execute(); // upsert en lote
            if (resp.isSuccessful() && resp.body() != null) {
                // Marcar sincronizados por id devueltos
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
                        Peso p = fromJson(resp.body().get(i).getAsJsonObject());
                        upsertLocal(db, p);
                        count++;
                    }
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
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
    private List<Peso> obtenerNoSincronizados() {
        List<Peso> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, fecha, nAnimales, pesoTotal, pesoMedio, " +
                        "fecha_actualizacion, eliminado, fecha_eliminado " +
                        "FROM " + TABLA + " WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                Peso p = new Peso();
                p.setId(c.getString(0));
                p.setId_lote(c.getString(1));
                p.setId_explotacion(c.getString(2));
                p.setFecha(c.getString(3));
                p.setnAnimales(c.getInt(4));
                p.setPesoTotal(c.getDouble(5));
                p.setPesoMedio(c.getDouble(6));
                p.setFecha_actualizacion(c.getString(7));
                p.setEliminado(c.getInt(8));
                p.setFecha_eliminado(c.getString(9));
                p.setSincronizado(0);
                out.add(p);
            }
        }
        return out;
    }

    private void marcarSincronizado(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues();
        v.put("sincronizado", 1);
        db.update(TABLA, v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, Peso p) {
        ContentValues v = new ContentValues();
        v.put("id", p.getId());
        v.put("id_lote", p.getId_lote());
        v.put("id_explotacion", p.getId_explotacion());
        v.put("fecha", p.getFecha());
        v.put("nAnimales", p.getnAnimales());
        v.put("pesoTotal", p.getPesoTotal());
        v.put("pesoMedio", p.getPesoMedio());
        v.put("fecha_actualizacion", p.getFecha_actualizacion());
        v.put("sincronizado", 1); // viene de remoto
        v.put("eliminado", p.getEliminado());
        v.put("fecha_eliminado", p.getFecha_eliminado());

        int updated = db.update(TABLA, v, "id=?", new String[]{p.getId()});
        if (updated == 0) db.insert(TABLA, null, v);
    }

    private JsonObject toJson(Peso p) {
        JsonObject o = new JsonObject();
        o.addProperty("id", p.getId());
        o.addProperty("id_lote", p.getId_lote());
        o.addProperty("id_explotacion", p.getId_explotacion());
        o.addProperty("fecha", p.getFecha());
        o.addProperty("nAnimales", p.getnAnimales());
        o.addProperty("pesoTotal", p.getPesoTotal());
        o.addProperty("pesoMedio", p.getPesoMedio());
        o.addProperty("fecha_actualizacion", p.getFecha_actualizacion());
        o.addProperty("eliminado", p.getEliminado());
        o.addProperty("fecha_eliminado", p.getFecha_eliminado());
        return o;
    }

    private Peso fromJson(JsonObject o) {
        Peso p = new Peso();
        p.setId(getS(o, "id"));
        p.setId_lote(getS(o, "id_lote"));
        p.setId_explotacion(getS(o, "id_explotacion"));
        p.setFecha(getS(o, "fecha"));
        p.setnAnimales(getI(o, "nAnimales"));
        p.setPesoTotal(getD(o, "pesoTotal"));
        p.setPesoMedio(getD(o, "pesoMedio"));
        p.setFecha_actualizacion(getS(o, "fecha_actualizacion"));
        p.setEliminado(getI(o, "eliminado"));
        p.setFecha_eliminado(getS(o, "fecha_eliminado"));
        p.setSincronizado(1);
        return p;
    }

    private String getS(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }
    private int getI(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
    }
    private double getD(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0.0;
    }
}
