package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

public class LoteRepository {

    private static final String TABLA = "lotes";
    private final DBHelper dbh;
    private final GenericSupabaseService api;

    public LoteRepository(DBHelper dbh) {
        this.dbh = dbh;
        this.api = ApiClient.get().create(GenericSupabaseService.class);
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
}
