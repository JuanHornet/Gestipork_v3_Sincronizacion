// NotaRepository.java
package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Nota;

public class NotaRepository {
    private final DBHelper dbh;
    public NotaRepository(DBHelper dbh) { this.dbh = dbh; }

    public String insert(Nota n) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", n.getId());
        v.put("id_lote", n.getId_lote());
        v.put("id_explotacion", n.getId_explotacion());
        v.put("fecha", n.getFecha());
        v.put("texto", n.getTexto());
        v.put("fecha_actualizacion", n.getFecha_actualizacion());
        v.put("sincronizado", n.getSincronizado());
        v.put("eliminado", n.getEliminado());
        db.insertOrThrow("notas", null, v);
        return n.getId();
    }
}
