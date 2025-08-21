// data/repo/CubricionRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Cubricion;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.GenericSupabaseService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;
import retrofit2.Response;

public class CubricionRepository {

    private static final String TABLA = "cubriciones";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public CubricionRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
    }

    public Cubricion findByLote(String idLote) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, nombre_lote, nMadres, nPadres, fechaInicioCubricion, fechaFinCubricion, " +
                        "fecha_actualizacion, sincronizado, eliminado, fecha_eliminado " +
                        "FROM cubriciones WHERE id_lote=? LIMIT 1", new String[]{idLote})) {
            if (!c.moveToFirst()) return null;
            Cubricion cu = new Cubricion();
            cu.setId(c.getString(0));
            cu.setId_lote(c.getString(1));
            cu.setId_explotacion(c.getString(2));
            cu.setNombre_lote(c.getString(3));
            cu.setnMadres(c.getInt(4));
            cu.setnPadres(c.getInt(5));
            cu.setFechaInicioCubricion(c.getString(6));
            cu.setFechaFinCubricion(c.getString(7));
            cu.setFecha_actualizacion(c.getString(8));
            cu.setSincronizado(c.getInt(9));
            cu.setEliminado(c.getInt(10));
            cu.setFecha_eliminado(c.getString(11));
            return cu;
        }
    }

    // PUSH
    public int pushPendientes() {
        List<Cubricion> pendientes = obtenerNoSincronizadas();
        if (pendientes.isEmpty()) return 0;
        JsonArray body = new JsonArray();
        for (Cubricion cu : pendientes) body.add(toJson(cu));
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
                        Cubricion cu = fromJson(resp.body().get(i).getAsJsonObject());
                        upsertLocal(db, cu);
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
    private List<Cubricion> obtenerNoSincronizadas() {
        List<Cubricion> out = new ArrayList<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_lote, id_explotacion, nombre_lote, nMadres, nPadres, fechaInicioCubricion, fechaFinCubricion, " +
                        "fecha_actualizacion, eliminado, fecha_eliminado FROM cubriciones WHERE sincronizado=0", null)) {
            while (c.moveToNext()) {
                Cubricion cu = new Cubricion();
                cu.setId(c.getString(0));
                cu.setId_lote(c.getString(1));
                cu.setId_explotacion(c.getString(2));
                cu.setNombre_lote(c.getString(3));
                cu.setnMadres(c.getInt(4));
                cu.setnPadres(c.getInt(5));
                cu.setFechaInicioCubricion(c.getString(6));
                cu.setFechaFinCubricion(c.getString(7));
                cu.setFecha_actualizacion(c.getString(8));
                cu.setEliminado(c.getInt(9));
                cu.setFecha_eliminado(c.getString(10));
                cu.setSincronizado(0);
                out.add(cu);
            }
        }
        return out;
    }

    private void marcarSincronizada(SQLiteDatabase db, String id) {
        ContentValues v = new ContentValues(); v.put("sincronizado",1);
        db.update(TABLA, v, "id=?", new String[]{id});
    }

    private void upsertLocal(SQLiteDatabase db, Cubricion cu) {
        ContentValues v = new ContentValues();
        v.put("id", cu.getId());
        v.put("id_lote", cu.getId_lote());
        v.put("id_explotacion", cu.getId_explotacion());
        v.put("nombre_lote", cu.getNombre_lote());
        v.put("nMadres", cu.getnMadres());
        v.put("nPadres", cu.getnPadres());
        v.put("fechaInicioCubricion", cu.getFechaInicioCubricion());
        v.put("fechaFinCubricion", cu.getFechaFinCubricion());
        v.put("fecha_actualizacion", cu.getFecha_actualizacion());
        v.put("sincronizado", 1);
        v.put("eliminado", cu.getEliminado());
        v.put("fecha_eliminado", cu.getFecha_eliminado());
        int updated = db.update(TABLA, v, "id=?", new String[]{cu.getId()});
        if (updated == 0) db.insert(TABLA, null, v);
    }

    private JsonObject toJson(Cubricion cu) {
        JsonObject o = new JsonObject();
        o.addProperty("id", cu.getId());
        o.addProperty("id_lote", cu.getId_lote());
        o.addProperty("id_explotacion", cu.getId_explotacion());
        o.addProperty("nombre_lote", cu.getNombre_lote());
        o.addProperty("nMadres", cu.getnMadres());
        o.addProperty("nPadres", cu.getnPadres());
        o.addProperty("fechaInicioCubricion", cu.getFechaInicioCubricion());
        o.addProperty("fechaFinCubricion", cu.getFechaFinCubricion());
        o.addProperty("fecha_actualizacion", cu.getFecha_actualizacion());
        o.addProperty("eliminado", cu.getEliminado());
        o.addProperty("fecha_eliminado", cu.getFecha_eliminado());
        return o;
    }

    private Cubricion fromJson(JsonObject o) {
        Cubricion cu = new Cubricion();
        cu.setId(getS(o,"id"));
        cu.setId_lote(getS(o,"id_lote"));
        cu.setId_explotacion(getS(o,"id_explotacion"));
        cu.setNombre_lote(getS(o,"nombre_lote"));
        cu.setnMadres(getI(o,"nMadres"));
        cu.setnPadres(getI(o,"nPadres"));
        cu.setFechaInicioCubricion(getS(o,"fechaInicioCubricion"));
        cu.setFechaFinCubricion(getS(o,"fechaFinCubricion"));
        cu.setFecha_actualizacion(getS(o,"fecha_actualizacion"));
        cu.setEliminado(getI(o,"eliminado"));
        cu.setFecha_eliminado(getS(o,"fecha_eliminado"));
        cu.setSincronizado(1);
        return cu;
    }

    private String getS(JsonObject o, String k){ return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():null; }
    private int getI(JsonObject o, String k){ return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsInt():0; }
}
