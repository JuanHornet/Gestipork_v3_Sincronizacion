package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;

public class BajaRepository {
    private final DBHelper dbh;
    public BajaRepository(DBHelper dbh) { this.dbh = dbh; }

    public String insertRaw(String id, String idLote, String idExplotacion, String fecha,
                            int cantidad, String causa, String fechaAct,
                            int sincronizado, int eliminado) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("id_lote", idLote);
        v.put("id_explotacion", idExplotacion);
        v.put("fecha", fecha);
        v.put("cantidad", cantidad);
        v.put("causa", causa);
        v.put("fecha_actualizacion", fechaAct);
        v.put("sincronizado", sincronizado);
        v.put("eliminado", eliminado);
        db.insertOrThrow("bajas", null, v);
        return id;
    }
}
