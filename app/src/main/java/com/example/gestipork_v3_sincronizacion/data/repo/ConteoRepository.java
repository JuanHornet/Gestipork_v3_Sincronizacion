// ConteoRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Conteo;

public class ConteoRepository {
    private final DBHelper dbh;
    public ConteoRepository(DBHelper dbh) { this.dbh = dbh; }

    public String insert(Conteo c) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", c.getId());
        v.put("id_lote", c.getId_lote());
        v.put("id_explotacion", c.getId_explotacion());
        v.put("fecha", c.getFecha());
        v.put("nAnimales", c.getnAnimales());
        v.put("observaciones", c.getObservaciones());
        v.put("fecha_actualizacion", c.getFecha_actualizacion());
        v.put("sincronizado", c.getSincronizado());
        v.put("eliminado", c.getEliminado());
        db.insertOrThrow("contar", null, v);
        return c.getId();
    }
}
