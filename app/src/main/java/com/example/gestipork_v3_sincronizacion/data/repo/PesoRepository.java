// PesoRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Peso;

public class PesoRepository {
    private final DBHelper dbh;
    public PesoRepository(DBHelper dbh) { this.dbh = dbh; }

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
        db.insertOrThrow("pesos", null, v);
        return p.getId();
    }
}
