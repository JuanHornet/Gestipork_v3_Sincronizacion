// data/repo/LoteRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Lote;

public class LoteRepository {
    private final DBHelper dbh;
    public LoteRepository(DBHelper dbh) { this.dbh = dbh; }

    public Lote findById(String id) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, id_explotacion, nDisponibles, nIniciales, nombre_lote, raza, estado, color, fecha_actualizacion, sincronizado, eliminado FROM lotes WHERE id=?",
                new String[]{id})) {
            if (!c.moveToFirst()) return null;
            Lote l = new Lote();
            l.setId(c.getString(0));
            l.setId_explotacion(c.getString(1));
            l.setnDisponibles(c.getInt(2));
            l.setnIniciales(c.getInt(3));
            l.setNombre_lote(c.getString(4));
            l.setRaza(c.getString(5));
            l.setEstado(c.getInt(6));
            l.setColor(c.getString(7));
            l.setFecha_actualizacion(c.getString(8));
            l.setSincronizado(c.getInt(9));
            l.setEliminado(c.getInt(10));
            return l;
        }
    }

    public void update(Lote l) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id_explotacion", l.getId_explotacion());
        v.put("nDisponibles", l.getnDisponibles());
        v.put("nIniciales", l.getnIniciales());
        v.put("nombre_lote", l.getNombre_lote());
        v.put("raza", l.getRaza());
        v.put("estado", l.getEstado());
        v.put("color", l.getColor());
        v.put("fecha_actualizacion", l.getFecha_actualizacion());
        v.put("sincronizado", l.getSincronizado());
        v.put("eliminado", l.getEliminado());
        db.update("lotes", v, "id=?", new String[]{l.getId()});
    }
}
