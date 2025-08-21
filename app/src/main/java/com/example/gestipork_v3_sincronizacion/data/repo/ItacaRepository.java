// data/repo/ItacaRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Itaca;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;
import retrofit2.Response;

public class ItacaRepository {

    private static final String TABLA = "itaca";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public ItacaRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }

    public Itaca findByLote(String idLote) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, nombre_lote, nAnimales, nMadres, nPadres, " +
                        "fechaPrimerNacimiento, fechaUltimoNacimiento, raza, color, crotalesSolicitados, dcer, " +
                        "fecha_actualizacion, sincronizado, eliminado, fecha_eliminado " +
                        "FROM itaca WHERE id_lote=? LIMIT 1", new String[]{idLote})) {
            if (!c.moveToFirst()) return null;
            Itaca i = new Itaca();
            i.setId(c.getString(0));
            i.setId_lote(c.getString(1));
            i.setId_explotacion(c.getString(2));
            i.setNombre_lote(c.getString(3));
            i.setnAnimales(c.getInt(4));
            i.setnMadres(c.getInt(5));
            i.setnPadres(c.getInt(6));
            i.setFechaPrimerNacimiento(c.getString(7));
            i.setFechaUltimoNacimiento(c.getString(8));
            i.setRaza(c.getString(9));
            i.setColor(c.getString(10));
            i.setCrotalesSolicitados(c.getInt(11));
            i.setDcer(c.getString(12));
            i.setFecha_actualizacion(c.getString(13));
            i.setSincronizado(c.getInt(14));
            i.setEliminado(c.getInt(15));
            i.setFecha_eliminado(c.getString(16));
            return i;
        }
    }

    // PUSH
    public int pushPendientes() {
        List<Itaca> pendientes = obtenerNoSincronizadas();
        if (pendientes.isEmpty()) return 0;
        JsonArray body = new JsonArray();
        for (Itaca it : pendientes) body.add(toJson(it));
        try {
            Response<JsonArray> resp = api.insertMany(TABLA, body).execute();
            if (resp.isSuccessful() && resp.body()!=null) {
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i=0;i<resp.body().size();i++) {
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

    // PULL
    public int pullDesde(String isoUltimaSync) {
        Map<String,String> q = new HashMap<>();
        q.put("fecha_actualizacion", "gt."+isoUltimaSync);
        q.put("order", "fecha_actualizacion.asc");
        try {
            Response<JsonArray> resp = api.fetch(TABLA, q).execute();
            if (resp.isSuccessful() && resp.body()!=null) {
                int count=0;
                SQLiteDatabase db = dbh.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (int i=0;i<resp.body().size();i++) {
                        Itaca it = fromJson(resp.body().get(i).getAsJsonObject());
                        upsertLocal(db, it);
                        count++;
                    }
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                return count;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // Delete lógico remoto
    public boolean marcarEliminadoEnSupabase(String id, String fechaIso) {
        Map<String,String> match = new HashMap<>(); match.put("id","eq."+id);
        JsonObject patch = new JsonObject();
        patch.addProperty("eliminado",1);
        patch.addProperty("fecha_eliminado",fechaIso);
        patch.addProperty("fecha_actualizacion",fechaIso);
        try {
            Response<JsonArray> resp = api.update(TABLA, match, patch).execute();
            return resp.isSuccessful();
        } catch (Exception ignored) {}
        return false;
    }

    // Helpers
    private List<Itaca> obtenerNoSincronizadas() {
        List<Itaca> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, nombre_lote, nAnimales, nMadres, nPadres, " +
                        "fechaPrimerNacimiento, fechaUltimoNacimiento, raza, color, crotalesSolicitados, dcer, " +
                        "fecha_actualizacion, eliminado, fecha_eliminado " +
                        "FROM itaca WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                Itaca i = new Itaca();
                i.setId(c.getString(0));
                i.setId_lote(c.getString(1));
                i.setId_explotacion(c.getString(2));
                i.setNombre_lote(c.getString(3));
                i.setnAnimales(c.getInt(4));
                i.setnMadres(c.getInt(5));
                i.setnPadres(c.getInt(6));
                i.setFechaPrimerNacimiento(c.getString(7));
                i.setFechaUltimoNacimiento(c.getString(8));
                i.setRaza(c.getString(9));
                i.setColor(c.getString(10));
                i.setCrotalesSolicitados(c.getInt(11));
                i.setDcer(c.getString(12));
                i.setFecha_actualizacion(c.getString(13));
                i.setEliminado(c.getInt(14));
                i.setFecha_eliminado(c.getString(15));
                i.setSincronizado(0);
                out.add(i);
            }
        }
        return out;
    }

    private void marcarSincronizada(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues(); v.put("sincronizado",1);
        db.update(TABLA, v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, Itaca i) {
        ContentValues v = new ContentValues();
        v.put("id", i.getId());
        v.put("id_lote", i.getId_lote());
        v.put("id_explotacion", i.getId_explotacion());
        v.put("nombre_lote", i.getNombre_lote());
        v.put("nAnimales", i.getnAnimales());
        v.put("nMadres", i.getnMadres());
        v.put("nPadres", i.getnPadres());
        v.put("fechaPrimerNacimiento", i.getFechaPrimerNacimiento());
        v.put("fechaUltimoNacimiento", i.getFechaUltimoNacimiento());
        v.put("raza", i.getRaza());
        v.put("color", i.getColor());
        v.put("crotalesSolicitados", i.getCrotalesSolicitados());
        v.put("dcer", i.getDcer());
        v.put("fecha_actualizacion", i.getFecha_actualizacion());
        v.put("sincronizado", 1);
        v.put("eliminado", i.getEliminado());
        v.put("fecha_eliminado", i.getFecha_eliminado());
        int updated = db.update(TABLA, v, "id=?", new String[]{i.getId()});
        if (updated == 0) db.insert(TABLA, null, v);
    }

    private JsonObject toJson(Itaca i) {
        JsonObject o = new JsonObject();
        o.addProperty("id", i.getId());
        o.addProperty("id_lote", i.getId_lote());
        o.addProperty("id_explotacion", i.getId_explotacion());
        o.addProperty("nombre_lote", i.getNombre_lote());
        o.addProperty("nAnimales", i.getnAnimales());
        o.addProperty("nMadres", i.getnMadres());
        o.addProperty("nPadres", i.getnPadres());
        o.addProperty("fechaPrimerNacimiento", i.getFechaPrimerNacimiento());
        o.addProperty("fechaUltimoNacimiento", i.getFechaUltimoNacimiento());
        o.addProperty("raza", i.getRaza());
        o.addProperty("color", i.getColor());
        o.addProperty("crotalesSolicitados", i.getCrotalesSolicitados());
        o.addProperty("dcer", i.getDcer());
        o.addProperty("fecha_actualizacion", i.getFecha_actualizacion());
        o.addProperty("eliminado", i.getEliminado());
        o.addProperty("fecha_eliminado", i.getFecha_eliminado());
        return o;
    }

    private Itaca fromJson(JsonObject o) {
        Itaca i = new Itaca();
        i.setId(getS(o,"id"));
        i.setId_lote(getS(o,"id_lote"));
        i.setId_explotacion(getS(o,"id_explotacion"));
        i.setNombre_lote(getS(o,"nombre_lote"));
        i.setnAnimales(getI(o,"nAnimales"));
        i.setnMadres(getI(o,"nMadres"));
        i.setnPadres(getI(o,"nPadres"));
        i.setFechaPrimerNacimiento(getS(o,"fechaPrimerNacimiento"));
        i.setFechaUltimoNacimiento(getS(o,"fechaUltimoNacimiento"));
        i.setRaza(getS(o,"raza"));
        i.setColor(getS(o,"color"));
        i.setCrotalesSolicitados(getI(o,"crotalesSolicitados"));
        i.setDcer(getS(o,"dcer"));
        i.setFecha_actualizacion(getS(o,"fecha_actualizacion"));
        i.setEliminado(getI(o,"eliminado"));
        i.setFecha_eliminado(getS(o,"fecha_eliminado"));
        i.setSincronizado(1);
        return i;
    }

    private String getS(JsonObject o, String k){ return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():null; }
    private int getI(JsonObject o, String k){ return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsInt():0; }
}
