package com.example.gestipork_v3_sincronizacion.data.local;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import androidx.annotation.Nullable;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;

public class UsuarioLocalDao {

    private final DBHelper dbh;

    public UsuarioLocalDao(Context ctx) {
        this.dbh = new DBHelper(ctx.getApplicationContext());
    }

    /** Upsert en SQLite: si viene del servidor -> sincronizado=1; si es local -> 0 */
    public void upsert(String id, String email, @Nullable String nombre, boolean fromServer) {
        if (id == null || id.isEmpty() || email == null || email.isEmpty()) return;

        String sql = "INSERT OR REPLACE INTO usuarios " +
                "(id, email, nombre, fecha_actualizacion, sincronizado, eliminado, fecha_eliminado) " +
                "VALUES (?,?,?,?,?,?,?)";

        SQLiteDatabase db = dbh.getWritableDatabase();
        SQLiteStatement st = db.compileStatement(sql);
        try {
            st.bindString(1, id);
            st.bindString(2, email);
            if (nombre != null) st.bindString(3, nombre); else st.bindNull(3);
            st.bindString(4, FechaUtils.ahoraIso());
            st.bindLong(5, fromServer ? 1 : 0);
            st.bindLong(6, 0);
            st.bindNull(7);
            st.executeInsert();
        } finally {
            st.close();
        }
    }

    /** Obtiene un usuario por id (o null si no existe). */
    @Nullable
    public Usuario getById(String id) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, email, nombre, fecha_actualizacion, sincronizado, eliminado, fecha_eliminado " +
                        "FROM usuarios WHERE id = ? LIMIT 1",
                new String[]{ id }
        );
        try {
            if (!c.moveToFirst()) return null;
            Usuario u = new Usuario();
            u.id = c.getString(0);
            u.email = c.getString(1);
            u.nombre = c.isNull(2) ? null : c.getString(2);
            u.fechaActualizacion = c.isNull(3) ? null : c.getString(3);
            u.sincronizado = c.getInt(4);
            u.eliminado = c.getInt(5);
            u.fechaEliminado = c.isNull(6) ? null : c.getString(6);
            return u;
        } finally {
            c.close();
        }
    }

    /** Devuelve el primer usuario cacheado (útil para mostrar nombre/email en UI). */
    @Nullable
    public Usuario getAny() {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, email, nombre, fecha_actualizacion, sincronizado, eliminado, fecha_eliminado " +
                        "FROM usuarios LIMIT 1", null
        );
        try {
            if (!c.moveToFirst()) return null;
            Usuario u = new Usuario();
            u.id = c.getString(0);
            u.email = c.getString(1);
            u.nombre = c.isNull(2) ? null : c.getString(2);
            u.fechaActualizacion = c.isNull(3) ? null : c.getString(3);
            u.sincronizado = c.getInt(4);
            u.eliminado = c.getInt(5);
            u.fechaEliminado = c.isNull(6) ? null : c.getString(6);
            return u;
        } finally {
            c.close();
        }
    }

    /** Limpia la tabla local (no suele hacer falta). */
    public void clear() {
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.delete("usuarios", null, null);
    }

    /** POJO simple para la UI */
    public static class Usuario {
        public String id;
        public String email;
        public String nombre;
        public String fechaActualizacion;
        public int sincronizado;
        public int eliminado;
        public String fechaEliminado;
    }
}
