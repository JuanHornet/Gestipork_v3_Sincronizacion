// data/repo/SalidaRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;

public class SalidaRepository {
    private final DBHelper dbh;
    public SalidaRepository(DBHelper dbh) { this.dbh = dbh; }

    public String insertRaw(String id, String idLote, String idExplotacion, String fecha,
                            int cantidad, String destino, String observaciones,
                            String fechaAct, int sincronizado, int eliminado) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("id_lote", idLote);
        v.put("id_explotacion", idExplotacion);
        v.put("fecha", fecha);
        v.put("cantidad", cantidad);
        v.put("destino", destino);
        v.put("observaciones", observaciones);
        v.put("fecha_actualizacion", fechaAct);
        v.put("sincronizado", sincronizado);
        v.put("eliminado", eliminado);
        db.insertOrThrow("salidas", null, v);
        return id;
    }
}
