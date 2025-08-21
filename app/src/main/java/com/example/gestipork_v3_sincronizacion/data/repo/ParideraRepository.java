// data/repo/ParideraRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Paridera;

public class ParideraRepository {
    private final DBHelper dbh;
    public ParideraRepository(DBHelper dbh) { this.dbh = dbh; }

    public Paridera findByLote(String idLote) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, id_lote, id_explotacion, nombre_lote, nMadres, nPadres, fechaInicio, fechaFin, fecha_actualizacion, sincronizado, eliminado FROM parideras WHERE id_lote=? LIMIT 1",
                new String[]{idLote})) {
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
            return p;
        }
    }

    public void update(Paridera p) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id_explotacion", p.getId_explotacion());
        v.put("nombre_lote", p.getNombre_lote());
        v.put("nMadres", p.getnMadres());
        v.put("nPadres", p.getnPadres());
        v.put("fechaInicio", p.getFechaInicio());
        v.put("fechaFin", p.getFechaFin());
        v.put("fecha_actualizacion", p.getFecha_actualizacion());
        v.put("sincronizado", p.getSincronizado());
        v.put("eliminado", p.getEliminado());
        db.update("parideras", v, "id=?", new String[]{p.getId()});
    }
}
