package com.example.gestipork_v3_sincronizacion.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ExplotacionMembersRepository {
    private final DBHelper dbh;
    public ExplotacionMembersRepository(DBHelper dbh) { this.dbh = dbh; }

    public void upsertLocal(ExplotacionMember m, boolean marcarSincronizado) {
        if (m.getId() == null || m.getId().isEmpty()) m.setId(UUID.randomUUID().toString());
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", m.getId());
        v.put("id_explotacion", m.getIdExplotacion());
        v.put("id_usuario", m.getIdUsuario());
        v.put("rol", m.getRol());
        v.putNull("invitado_por");
        v.put("estado_invitacion", m.getEstadoInvitacion() == null ? "accepted" : m.getEstadoInvitacion());
        v.put("fecha_actualizacion", m.getFechaActualizacion());
        v.put("sincronizado", marcarSincronizado ? 1 : 0);
        db.insertWithOnConflict("explotacion_members", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<ExplotacionMember> listarPorUsuario(String idUsuario) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        List<ExplotacionMember> out = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_explotacion, id_usuario, rol, estado_invitacion, fecha_actualizacion, sincronizado " +
                        "FROM explotacion_members WHERE id_usuario=?", new String[]{idUsuario})) {
            while (c.moveToNext()) out.add(fromCursor(c));
        }
        return out;
    }

    public boolean esMiembro(String idUsuario, String idExplotacion) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM explotacion_members " +
                        "WHERE id_usuario=? AND id_explotacion=? AND estado_invitacion='accepted' LIMIT 1",
                new String[]{idUsuario, idExplotacion})) {
            return c.moveToFirst();
        }
    }

    public String rolDe(String idUsuario, String idExplotacion) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT rol FROM explotacion_members " +
                        "WHERE id_usuario=? AND id_explotacion=? AND estado_invitacion='accepted' LIMIT 1",
                new String[]{idUsuario, idExplotacion})) {
            if (c.moveToFirst()) return c.getString(0);
        }
        return null;
    }

    public Set<String> explotacionesAutorizadas(String idUsuario) {
        Set<String> ids = new HashSet<>();
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id_explotacion FROM explotacion_members " +
                        "WHERE id_usuario=? AND estado_invitacion='accepted'",
                new String[]{idUsuario})) {
            while (c.moveToNext()) ids.add(c.getString(0));
        }
        return ids;
    }

    public List<ExplotacionMember> noSincronizados() {
        SQLiteDatabase db = dbh.getReadableDatabase();
        List<ExplotacionMember> out = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT id, id_explotacion, id_usuario, rol, estado_invitacion, fecha_actualizacion, sincronizado " +
                        "FROM explotacion_members WHERE sincronizado=0", null)) {
            while (c.moveToNext()) out.add(fromCursor(c));
        }
        return out;
    }

    public void marcarSincronizado(String id) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("sincronizado", 1);
        db.update("explotacion_members", v, "id=?", new String[]{id});
    }

    private ExplotacionMember fromCursor(Cursor c) {
        ExplotacionMember m = new ExplotacionMember();
        m.setId(c.getString(0));
        m.setIdExplotacion(c.getString(1));
        m.setIdUsuario(c.getString(2));
        m.setRol(c.getString(3));
        m.setEstadoInvitacion(c.getString(4));
        m.setFechaActualizacion(c.getString(5));
        m.setSincronizado(c.getInt(6));
        return m;
    }
}
