// features/lotes/LoteServiceImpl.java
package com.example.gestipork_v3_sincronizacion.features.lotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.base.IdUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.*;
import com.example.gestipork_v3_sincronizacion.data.repo.*;

public class LoteServiceImpl implements LoteService {

    private final DBHelper dbh;
    private final LoteRepository loteRepo;
    private final ParideraRepository parRepo;
    private final CubricionRepository cubRepo;
    private final ItacaRepository itaRepo;
    private final SalidaRepository salidaRepo;
    private final BajaRepository bajaRepo;
    private final AccionRepository accionRepo;
    private final NotaRepository notaRepo;
    private final PesoRepository pesoRepo;
    private final ContarRepository conteoRepo;

    public LoteServiceImpl(Context ctx) {
        this.dbh = new DBHelper(ctx);
        this.loteRepo = new LoteRepository(dbh);
        this.parRepo = new ParideraRepository(dbh);
        this.cubRepo = new CubricionRepository(dbh);
        this.itaRepo = new ItacaRepository(dbh);
        this.salidaRepo = new SalidaRepository(dbh);
        this.bajaRepo = new BajaRepository(dbh);
        this.accionRepo = new AccionRepository(dbh);
        this.notaRepo = new NotaRepository(dbh);
        this.pesoRepo = new PesoRepository(dbh);
        this.conteoRepo = new ContarRepository(dbh);
    }

    // ---------- 1:1 ----------
    @Override
    public String crearLoteConHijos(String idExplotacion, String nombreLote,
                                    String raza, String color, int estado, int nIniciales) {
        // Delegamos en el método transaccional del DBHelper que ya preparamos
        return dbh.crearLoteConHijos(idExplotacion, nombreLote, raza, color, estado, nIniciales);
    }

    @Override
    public Paridera getParidera(String idLote) { return parRepo.findByLote(idLote); }

    @Override
    public Cubricion getCubricion(String idLote) { return cubRepo.findByLote(idLote); }

    @Override
    public Itaca getItaca(String idLote) { return itaRepo.findByLote(idLote); }

    @Override
    public void updateParidera(Paridera p) { parRepo.update(p); }

    @Override
    public void updateCubricion(Cubricion c) { cubRepo.update(c); }

    @Override
    public void updateItaca(Itaca i) { itaRepo.update(i); }

    // ---------- 1:N ----------
    @Override
    public String agregarBaja(String idLote, String idExplotacion, String fechaISO,
                              int cantidad, String causa) {
        String now = FechaUtils.ahoraIso();
        String id = IdUtils.uuid();

        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1) Insert baja
            ContentValues v = new ContentValues();
            v.put("id", id);
            v.put("id_lote", idLote);
            v.put("id_explotacion", idExplotacion);
            v.put("fecha", fechaISO);
            v.put("cantidad", cantidad);
            v.put("causa", causa);
            v.put("fecha_actualizacion", now);
            v.put("sincronizado", 0);
            v.put("eliminado", 0);
            db.insertOrThrow("bajas", null, v);

            // 2) Decrementar stock en lotes (no negativo)
            int actuales = getDisponibles(db, idLote);
            int nuevo = Math.max(0, actuales - cantidad);
            ContentValues up = new ContentValues();
            up.put("nDisponibles", nuevo);
            up.put("fecha_actualizacion", now);
            up.put("sincronizado", 0);
            db.update("lotes", up, "id=?", new String[]{idLote});

            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public String agregarSalida(String idLote, String idExplotacion, String fechaISO,
                                int cantidad, String destino, String observaciones,
                                boolean descuentaStock) {
        String now = FechaUtils.ahoraIso();
        String id = IdUtils.uuid();

        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1) Insert salida
            ContentValues v = new ContentValues();
            v.put("id", id);
            v.put("id_lote", idLote);
            v.put("id_explotacion", idExplotacion);
            v.put("fecha", fechaISO);
            v.put("cantidad", cantidad);
            v.put("destino", destino);
            v.put("observaciones", observaciones);
            v.put("fecha_actualizacion", now);
            v.put("sincronizado", 0);
            v.put("eliminado", 0);
            db.insertOrThrow("salidas", null, v);

            // 2) (Opcional) actualizar stock
            if (descuentaStock) {
                int actuales = getDisponibles(db, idLote);
                int nuevo = Math.max(0, actuales - cantidad);
                ContentValues up = new ContentValues();
                up.put("nDisponibles", nuevo);
                up.put("fecha_actualizacion", now);
                up.put("sincronizado", 0);
                db.update("lotes", up, "id=?", new String[]{idLote});
            }

            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public String agregarAccion(Accion a) {
        a.setId(IdUtils.uuid());
        a.setFecha_actualizacion(FechaUtils.ahoraIso());
        a.setSincronizado(0);
        a.setEliminado(0);
        return accionRepo.insert(a);
    }

    // ---- helper
    private int getDisponibles(SQLiteDatabase db, String idLote) {
        try (Cursor c = db.rawQuery("SELECT nDisponibles FROM lotes WHERE id=? LIMIT 1", new String[]{idLote})) {
            if (c.moveToFirst()) return c.getInt(0);
            return 0;
        }
    }

    @Override
    public String agregarNota(String idLote, String idExplotacion, String fechaISO, String texto) {
        Nota n = new Nota();
        n.setId(IdUtils.uuid());
        n.setId_lote(idLote);
        n.setId_explotacion(idExplotacion);
        n.setFecha(fechaISO);
        n.setTexto(texto);
        n.setFecha_actualizacion(FechaUtils.ahoraIso());
        n.setSincronizado(0);
        n.setEliminado(0);
        return notaRepo.insert(n);
    }

    @Override
    public String agregarPeso(String idLote, String idExplotacion, String fechaISO,
                              int nAnimales, double pesoTotal) {
        Peso p = new Peso();
        p.setId(IdUtils.uuid());
        p.setId_lote(idLote);
        p.setId_explotacion(idExplotacion);
        p.setFecha(fechaISO);
        p.setnAnimales(nAnimales);
        p.setPesoTotal(pesoTotal);
        // Calcula pesoMedio si procede
        Double medio = (nAnimales > 0) ? (pesoTotal / nAnimales) : null;
        p.setPesoMedio(medio);
        p.setFecha_actualizacion(FechaUtils.ahoraIso());
        p.setSincronizado(0);
        p.setEliminado(0);
        return pesoRepo.insert(p);
    }

    @Override
    public String agregarConteo(String idLote, String idExplotacion, String fechaISO,
                                int nAnimales, String observaciones) {
        Contar c = new Contar();
        c.setId(IdUtils.uuid());
        c.setId_lote(idLote);
        c.setId_explotacion(idExplotacion);
        c.setFecha(fechaISO);
        c.setnAnimales(nAnimales);
        c.setObservaciones(observaciones);
        c.setFecha_actualizacion(FechaUtils.ahoraIso());
        c.setSincronizado(0);
        c.setEliminado(0);
        return conteoRepo.insert(c);
    }
}
