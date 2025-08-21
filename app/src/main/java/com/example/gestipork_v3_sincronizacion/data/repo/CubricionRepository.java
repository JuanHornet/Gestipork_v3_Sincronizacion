// data/repo/CubricionRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Cubricion;

public class CubricionRepository {
    private final DBHelper dbh;
    public CubricionRepository(DBHelper dbh) { this.dbh = dbh; }

    public Cubricion findByLote(String idLote) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, id_lote, id_explotacion, nombre_lote, nMadres, nPadres, fechaInicioCubricion, fechaFinCubricion, fecha_actualizacion, sincronizado, eliminado FROM cubriciones WHERE id_lote=? LIMIT 1",
                new String[]{idLote})) {
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
            return cu;
        }
    }

    public void update(Cubricion cu) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id_explotacion", cu.getId_explotacion());
        v.put("nombre_lote", cu.getNombre_lote());
        v.put("nMadres", cu.getnMadres());
        v.put("nPadres", cu.getnPadres());
        v.put("fechaInicioCubricion", cu.getFechaInicioCubricion());
        v.put("fechaFinCubricion", cu.getFechaFinCubricion());
        v.put("fecha_actualizacion", cu.getFecha_actualizacion());
        v.put("sincronizado", cu.getSincronizado());
        v.put("eliminado", cu.getEliminado());
        db.update("cubriciones", v, "id=?", new String[]{cu.getId()});
    }
}
