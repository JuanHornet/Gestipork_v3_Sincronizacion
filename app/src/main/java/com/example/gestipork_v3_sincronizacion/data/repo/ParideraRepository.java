// data/repo/ParideraRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Paridera;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Response;

public class ParideraRepository {

    private static final String TABLA = "parideras";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public ParideraRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }

    // ===== Local read (si lo usas en UI)
    public Paridera findByLote(String idLote) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, nombre_lote, nMadres, nPadres, fechaInicio, fechaFin, " +
                        "fecha_actualizacion, sincronizado, eliminado, fecha_eliminado " +
                        "FROM parideras WHERE id_lote=? LIMIT 1", new String[]{idLote})) {
            if (!c.moveToFirst()) return null;
            Paridera p = new Paridera();
            p.setId(c.getString(0));
            p.setId_lote(c.getString(1));
            p.setId_explotacion(c.getString(2));
            p.setNombre_lote(c.getString(3));
            p.setnMadres(c.getInt(4));
            p.setnPadres(c.getInt(5));
            p.setFechaInicio(c.getString(6));
            p.setFechaFin(c.getString(7));
            p.setFecha_actualizacion(c.getString(8));
            p.setSincronizado(c.getInt(9));
            p.setEliminado(c.getInt(10));
            p.setFecha_eliminado(c.getString(11));
            return p;
        }
    }

    // ===== PUSH
    public int pushPendientes() {
        List<Paridera> pendientes = obtenerNoSincronizadas();
        if (pendientes.isEmpty()) return 0;

        JsonArray body = new JsonArray();
        for (Paridera p : pendientes) body.add(toJson(p));

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
                } finally { db.endTransaction(); }
                return resp.body().size();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // ===== PULL
    public int pullDesde(String isoUltimaSync) {
        Map<String,String> q = new HashMap<>();
        q.put("fecha_actualizacion", "gt." + isoUltimaSync);
        q.put("order", "fecha_actualizacion.asc");
        try {
            Response<JsonArray> resp = api.fetch(TABLA, q).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                int count = 0;
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i=0;i<resp.body().size();i++) {
                        Paridera p = fromJson(resp.body().get(i).getAsJsonObject());
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

    // ===== Delete lógico remoto
    public boolean marcarEliminadoEnSupabase(String id, String fechaIso) {
        Map<String,String> match = new HashMap<>();
        match.put("id", "eq."+id);
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

    // ===== Helpers
    private List<Paridera> obtenerNoSincronizadas() {
        List<Paridera> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, nombre_lote, nMadres, nPadres, fechaInicio, fechaFin, " +
                        "fecha_actualizacion, eliminado, fecha_eliminado " +
                        "FROM parideras WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                Paridera p = new Paridera();
                p.setId(c.getString(0));
                p.setId_lote(c.getString(1));
                p.setId_explotacion(c.getString(2));
                p.setNombre_lote(c.getString(3));
                p.setnMadres(c.getInt(4));
                p.setnPadres(c.getInt(5));
                p.setFechaInicio(c.getString(6));
                p.setFechaFin(c.getString(7));
                p.setFecha_actualizacion(c.getString(8));
                p.setEliminado(c.getInt(9));
                p.setFecha_eliminado(c.getString(10));
                p.setSincronizado(0);
                out.add(p);
            }
        }
        return out;
    }

    private void marcarSincronizada(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues(); v.put("sincronizado", 1);
        db.update(TABLA, v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, Paridera p) {
        ContentValues v = new ContentValues();
        v.put("id", p.getId());
        v.put("id_lote", p.getId_lote());
        v.put("id_explotacion", p.getId_explotacion());
        v.put("nombre_lote", p.getNombre_lote());
        v.put("nMadres", p.getnMadres());
        v.put("nPadres", p.getnPadres());
        v.put("fechaInicio", p.getFechaInicio());
        v.put("fechaFin", p.getFechaFin());
        v.put("fecha_actualizacion", p.getFecha_actualizacion());
        v.put("sincronizado", 1);
        v.put("eliminado", p.getEliminado());
        v.put("fecha_eliminado", p.getFecha_eliminado());
        int updated = db.update(TABLA, v, "id=?", new String[]{p.getId()});
        if (updated == 0) db.insert(TABLA, null, v);
    }

    private JsonObject toJson(Paridera p) {
        JsonObject o = new JsonObject();
        o.addProperty("id", p.getId());
        o.addProperty("id_lote", p.getId_lote());
        o.addProperty("id_explotacion", p.getId_explotacion());
        o.addProperty("nombre_lote", p.getNombre_lote());
        o.addProperty("nMadres", p.getnMadres());
        o.addProperty("nPadres", p.getnPadres());
        o.addProperty("fechaInicio", p.getFechaInicio());
        o.addProperty("fechaFin", p.getFechaFin());
        o.addProperty("fecha_actualizacion", p.getFecha_actualizacion());
        o.addProperty("eliminado", p.getEliminado());
        o.addProperty("fecha_eliminado", p.getFecha_eliminado());
        return o;
    }

    private Paridera fromJson(JsonObject o) {
        Paridera p = new Paridera();
        p.setId(getS(o,"id"));
        p.setId_lote(getS(o,"id_lote"));
        p.setId_explotacion(getS(o,"id_explotacion"));
        p.setNombre_lote(getS(o,"nombre_lote"));
        p.setnMadres(getI(o,"nMadres"));
        p.setnPadres(getI(o,"nPadres"));
        p.setFechaInicio(getS(o,"fechaInicio"));
        p.setFechaFin(getS(o,"fechaFin"));
        p.setFecha_actualizacion(getS(o,"fecha_actualizacion"));
        p.setEliminado(getI(o,"eliminado"));
        p.setFecha_eliminado(getS(o,"fecha_eliminado"));
        p.setSincronizado(1);
        return p;
    }

    /** Actualiza una paridera en SQLite, marca sincronizado=0 y refresca fecha_actualizacion. */
    public int update(Paridera p) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id_lote", p.getId_lote());
        v.put("id_explotacion", p.getId_explotacion());
        v.put("nombre_lote", p.getNombre_lote());
        v.put("nMadres", p.getnMadres());
        v.put("nPadres", p.getnPadres());
        v.put("fechaInicio", p.getFechaInicio());
        v.put("fechaFin", p.getFechaFin());
        v.put("fecha_actualizacion", FechaUtils.ahoraIso());
        v.put("sincronizado", 0);
        // no tocamos eliminado/fecha_eliminado aquí
        return db.update("parideras", v, "id=?", new String[]{ p.getId() });
    }




    private String getS(JsonObject o, String k){ return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():null; }
    private int getI(JsonObject o, String k){ return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsInt():0; }
}
