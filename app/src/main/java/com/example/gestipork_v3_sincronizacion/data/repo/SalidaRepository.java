package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.SalidasExplotacion;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class SalidaRepository {

    private static final String TABLA = "salidas";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public SalidaRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }

    // ========= CRUD LOCAL BÁSICO =========
    public String insert(SalidasExplotacion s) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", s.getId());
        v.put("id_lote", s.getId_lote());
        v.put("id_explotacion", s.getId_explotacion());
        v.put("tipo_salida", s.getTipoSalida());
        v.put("tipo_alimentacion", s.getTipoAlimentacion());
        v.put("fecha_salida", s.getFechaSalida());
        v.put("n_animales", s.getNAnimales());
        v.put("observacion", s.getObservacion());
        v.put("fecha_actualizacion", s.getFecha_actualizacion());
        v.put("sincronizado", s.getSincronizado());
        v.put("eliminado", s.getEliminado());
        v.put("fecha_eliminado", s.getFecha_eliminado());
        db.insertOrThrow(TABLA, null, v);
        return s.getId();
    }

    // ========= SYNC: PUSH (local -> Supabase) =========
    public int pushPendientes() {
        List<SalidasExplotacion> pendientes = obtenerNoSincronizadas();
        if (pendientes.isEmpty()) return 0;

        JsonArray body = new JsonArray();
        for (SalidasExplotacion s : pendientes) {
            body.add(toJson(s));
        }

        try {
            Response<JsonArray> resp = api.insertMany(TABLA, body).execute();
            if (resp.isSuccessful() && resp.body() != null) {
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
                        SalidasExplotacion s = fromJson(o);
                        upsertLocal(db, s);
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
    private List<SalidasExplotacion> obtenerNoSincronizadas() {
        List<SalidasExplotacion> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, tipo_salida, tipo_alimentacion, fecha_salida, n_animales, " +
                        "observacion, fecha_actualizacion, sincronizado, eliminado, fecha_eliminado " +
                        "FROM " + TABLA + " WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                SalidasExplotacion s = new SalidasExplotacion();
                s.setId(c.getString(0));
                s.setId_lote(c.getString(1));
                s.setId_explotacion(c.getString(2));
                s.setTipoSalida(c.getString(3));
                s.setTipoAlimentacion(c.getString(4));
                s.setFechaSalida(c.getString(5));
                s.setNAnimales(c.getInt(6));
                s.setObservacion(c.getString(7));
                s.setFecha_actualizacion(c.getString(8));
                s.setSincronizado(c.getInt(9));
                s.setEliminado(c.getInt(10));
                s.setFecha_eliminado(c.getString(11));
                out.add(s);
            }
        }
        return out;
    }

    private void marcarSincronizada(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues();
        v.put("sincronizado", 1);
        db.update(TABLA, v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, SalidasExplotacion s) {
        ContentValues v = new ContentValues();
        v.put("id", s.getId());
        v.put("id_lote", s.getId_lote());
        v.put("id_explotacion", s.getId_explotacion());
        v.put("tipo_salida", s.getTipoSalida());
        v.put("tipo_alimentacion", s.getTipoAlimentacion());
        v.put("fecha_salida", s.getFechaSalida());
        v.put("n_animales", s.getNAnimales());
        v.put("observacion", s.getObservacion());
        v.put("fecha_actualizacion", s.getFecha_actualizacion());
        v.put("sincronizado", 1); // viene de remoto
        v.put("eliminado", s.getEliminado());
        v.put("fecha_eliminado", s.getFecha_eliminado());

        int updated = db.update(TABLA, v, "id=?", new String[]{s.getId()});
        if (updated == 0) db.insert(TABLA, null, v);
    }

    private JsonObject toJson(SalidasExplotacion s) {
        JsonObject o = new JsonObject();
        o.addProperty("id", s.getId());
        o.addProperty("id_lote", s.getId_lote());
        o.addProperty("id_explotacion", s.getId_explotacion());
        o.addProperty("tipo_salida", s.getTipoSalida());
        o.addProperty("tipo_alimentacion", s.getTipoAlimentacion());
        o.addProperty("fecha_salida", s.getFechaSalida());
        o.addProperty("n_animales", s.getNAnimales());
        o.addProperty("observacion", s.getObservacion());
        o.addProperty("fecha_actualizacion", s.getFecha_actualizacion());
        o.addProperty("eliminado", s.getEliminado());
        o.addProperty("fecha_eliminado", s.getFecha_eliminado());
        return o;
    }

    private SalidasExplotacion fromJson(JsonObject o) {
        SalidasExplotacion s = new SalidasExplotacion();
        s.setId(getString(o, "id"));
        s.setId_lote(getString(o, "id_lote"));
        s.setId_explotacion(getString(o, "id_explotacion"));
        s.setTipoSalida(getString(o, "tipo_salida"));
        s.setTipoAlimentacion(getString(o, "tipo_alimentacion"));
        s.setFechaSalida(getString(o, "fecha_salida"));
        s.setNAnimales(getInt(o, "n_animales"));
        s.setObservacion(getString(o, "observacion"));
        s.setFecha_actualizacion(getString(o, "fecha_actualizacion"));
        s.setEliminado(getInt(o, "eliminado"));
        s.setFecha_eliminado(getString(o, "fecha_eliminado"));
        s.setSincronizado(1);
        return s;
    }

    private String getString(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }
    private int getInt(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0;
    }
}
