package com.example.gestipork_v3_sincronizacion.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Response;

/** Bootstrap inicial: descarga explotaciones + lotes y los guarda en SQLite. */
public class BootstrapSync {
    private static final String TAG = "BootstrapSync";
    private final Context ctx;
    private final DBHelper dbh;
    private final BootstrapService api;

    public BootstrapSync(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.dbh = new DBHelper(this.ctx);
        this.api = ApiClient.get().create(BootstrapService.class);
    }

    /** ¿Ya hay datos locales? (usamos explotaciones como indicador) */
    public boolean hasLocalData() {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM explotaciones", null);
        try {
            boolean ok = false;
            if (c.moveToFirst()) ok = c.getLong(0) > 0;
            return ok;
        } finally {
            c.close();
        }
    }

    /** Pull completo (explotaciones + lotes) y guardado en SQLite. */
    public boolean seedAll() {
        try {
            String select = "*,lotes(*)";
            Response<List<JsonObject>> r = api.getTodo(select, null).execute();
            if (!r.isSuccessful() || r.body() == null) return false;

            SQLiteDatabase db = dbh.getWritableDatabase();
            db.beginTransaction();
            try {
                // Limpieza (solo en bootstrap; si prefieres merge, quita estos deletes)
                db.delete("lotes", null, null);
                db.delete("explotaciones", null, null);

                for (JsonObject exp : r.body()) {
                    insertExplotacion(db, exp);
                    insertArrayLotes(db, exp.getAsJsonArray("lotes"));
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            Log.i(TAG, "Bootstrap (explotaciones+lotes) OK");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Bootstrap fallo", e);
            return false;
        }
    }

    private void insertExplotacion(SQLiteDatabase db, JsonObject exp) {
        SQLiteStatement stmt = db.compileStatement(
                "INSERT OR REPLACE INTO explotaciones " +
                        "(id, nombre, codigo, direccion, municipio, provincia, cp, telefono, email, " +
                        " fecha_actualizacion, sincronizado, eliminado, fecha_eliminado) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
        );
        stmt.bindString(1, getS(exp, "id"));
        stmt.bindString(2, optS(exp, "nombre"));
        stmt.bindString(3, optS(exp, "codigo"));
        stmt.bindString(4, optS(exp, "direccion"));
        stmt.bindString(5, optS(exp, "municipio"));
        stmt.bindString(6, optS(exp, "provincia"));
        stmt.bindString(7, optS(exp, "cp"));
        stmt.bindString(8, optS(exp, "telefono"));
        stmt.bindString(9, optS(exp, "email"));
        stmt.bindString(10, optS(exp, "fecha_actualizacion"));
        stmt.bindLong(11, 1); // vienen sincronizadas
        stmt.bindLong(12, optI(exp, "eliminado"));
        stmt.bindString(13, optS(exp, "fecha_eliminado"));
        stmt.executeInsert();
        stmt.close();
    }

    private void insertArrayLotes(SQLiteDatabase db, JsonArray arr) {
        if (arr == null) return;
        for (int i = 0; i < arr.size(); i++) {
            JsonObject l = arr.get(i).getAsJsonObject();
            insertLote(db, l);
        }
    }

    private void insertLote(SQLiteDatabase db, JsonObject l) {
        SQLiteStatement stmt = db.compileStatement(
                "INSERT OR REPLACE INTO lotes " +
                        "(id, id_explotacion, nDisponibles, nIniciales, nombre_lote, raza, estado, color, " +
                        " fecha_actualizacion, sincronizado, eliminado, fecha_eliminado) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
        );
        stmt.bindString(1, getS(l, "id"));
        stmt.bindString(2, optS(l, "id_explotacion"));
        stmt.bindLong(3, optI(l, "nDisponibles"));
        stmt.bindLong(4, optI(l, "nIniciales"));
        stmt.bindString(5, optS(l, "nombre_lote"));
        stmt.bindString(6, optS(l, "raza"));
        stmt.bindLong(7, optI(l, "estado"));
        stmt.bindString(8, optS(l, "color"));
        stmt.bindString(9, optS(l, "fecha_actualizacion"));
        stmt.bindLong(10, 1);
        stmt.bindLong(11, optI(l, "eliminado"));
        stmt.bindString(12, optS(l, "fecha_eliminado"));
        stmt.executeInsert();
        stmt.close();
    }

    private String optS(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : null;
    }

    private int optI(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsInt() : 0;
    }

    private String getS(JsonObject o, String k) {
        return o.get(k).getAsString();
    }
}

/* Servicio Retrofit con embedding (sin public para que pueda ir en el mismo .java) */
interface BootstrapService {
    @retrofit2.http.GET("explotaciones")
    retrofit2.Call<java.util.List<com.google.gson.JsonObject>> getTodo(
            @retrofit2.http.Query("select") String select,
            @retrofit2.http.Query("order") String orderBy
    );
}
