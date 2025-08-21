package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Accion;

public class AccionRepository {
    private final DBHelper dbh;
    public AccionRepository(DBHelper dbh) { this.dbh = dbh; }

    public String insert(Accion a) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", a.getId());
        v.put("id_lote", a.getId_lote());
        v.put("id_explotacion", a.getId_explotacion());
        v.put("tipo", a.getTipo());
        v.put("fecha", a.getFecha());
        v.put("cantidad", a.getCantidad());
        v.put("observaciones", a.getObservaciones());
        v.put("fecha_actualizacion", a.getFecha_actualizacion());
        v.put("sincronizado", a.getSincronizado());
        v.put("eliminado", a.getEliminado());
        db.insertOrThrow("acciones", null, v);
        return a.getId();
    }
}
