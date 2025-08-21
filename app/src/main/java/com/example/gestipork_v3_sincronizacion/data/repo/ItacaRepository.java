// data/repo/ItacaRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Itaca;

public class ItacaRepository {
    private final DBHelper dbh;
    public ItacaRepository(DBHelper dbh) { this.dbh = dbh; }

    public Itaca findByLote(String idLote) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        final String sql =
                "SELECT " +
                        "id, " +
                        "id_lote, " +
                        "id_explotacion, " +
                        "nombre_lote, " +
                        "nAnimales, " +
                        "nMadres, " +
                        "nPadres, " +
                        "fechaPrimerNacimiento, " +
                        "fechaUltimoNacimiento, " +
                        "raza, " +
                        "color, " +
                        "crotalesSolicitados, " +
                        "dcer, " +                       // 🔹 nuevo campo
                        "fecha_actualizacion, " +
                        "sincronizado, " +
                        "eliminado " +
                        "FROM itaca WHERE id_lote=? LIMIT 1";

        try (Cursor c = db.rawQuery(sql, new String[]{idLote})) {
            if (!c.moveToFirst()) return null;

            Itaca i = new Itaca();
            i.setId(c.getString(c.getColumnIndexOrThrow("id")));
            i.setId_lote(c.getString(c.getColumnIndexOrThrow("id_lote")));
            i.setId_explotacion(c.getString(c.getColumnIndexOrThrow("id_explotacion")));
            i.setNombre_lote(c.getString(c.getColumnIndexOrThrow("nombre_lote")));
            i.setnAnimales(c.isNull(c.getColumnIndexOrThrow("nAnimales")) ? null : c.getInt(c.getColumnIndexOrThrow("nAnimales")));
            i.setnMadres(c.isNull(c.getColumnIndexOrThrow("nMadres")) ? null : c.getInt(c.getColumnIndexOrThrow("nMadres")));
            i.setnPadres(c.isNull(c.getColumnIndexOrThrow("nPadres")) ? null : c.getInt(c.getColumnIndexOrThrow("nPadres")));
            i.setFechaPrimerNacimiento(c.getString(c.getColumnIndexOrThrow("fechaPrimerNacimiento")));
            i.setFechaUltimoNacimiento(c.getString(c.getColumnIndexOrThrow("fechaUltimoNacimiento")));
            i.setRaza(c.getString(c.getColumnIndexOrThrow("raza")));
            i.setColor(c.getString(c.getColumnIndexOrThrow("color")));
            i.setCrotalesSolicitados(c.isNull(c.getColumnIndexOrThrow("crotalesSolicitados")) ? null : c.getInt(c.getColumnIndexOrThrow("crotalesSolicitados")));
            i.setDcer(c.getString(c.getColumnIndexOrThrow("dcer"))); // 🔹 leer dcer
            i.setFecha_actualizacion(c.getString(c.getColumnIndexOrThrow("fecha_actualizacion")));
            i.setSincronizado(c.getInt(c.getColumnIndexOrThrow("sincronizado")));
            i.setEliminado(c.getInt(c.getColumnIndexOrThrow("eliminado")));
            return i;
        }
    }

    public void update(Itaca i) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id_explotacion", i.getId_explotacion());
        v.put("nombre_lote", i.getNombre_lote());
        v.put("nAnimales", i.getnAnimales());
        v.put("nMadres", i.getnMadres());
        v.put("nPadres", i.getnPadres());
        v.put("fechaPrimerNacimiento", i.getFechaPrimerNacimiento());
        v.put("fechaUltimoNacimiento", i.getFechaUltimoNacimiento());
        v.put("raza", i.getRaza());
        v.put("color", i.getColor());
        v.put("crotalesSolicitados", i.getCrotalesSolicitados());
        v.put("dcer", i.getDcer()); // 🔹 actualizar dcer
        v.put("fecha_actualizacion", i.getFecha_actualizacion());
        v.put("sincronizado", i.getSincronizado());
        v.put("eliminado", i.getEliminado());
        db.update("itaca", v, "id=?", new String[]{i.getId()});
    }
}
