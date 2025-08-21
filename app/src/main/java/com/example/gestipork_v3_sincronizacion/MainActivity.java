package com.example.gestipork_v3_sincronizacion;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.base.IdUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.features.lotes.LoteService;
import com.example.gestipork_v3_sincronizacion.features.lotes.LoteServiceImpl;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GPv3-Test";
    private DBHelper dbh;
    private LoteService loteService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbh = new DBHelper(this);
        loteService = new LoteServiceImpl(this);

        Button btn = findViewById(R.id.btnCrearLote);
        btn.setOnClickListener(v -> runTest());
    }

    private void runTest() {
        try {
            // 1) Asegurar USUARIO y EXPLOTACIÓN de prueba (FK)
            String usuarioId = ensureUsuario();
            String explotacionId = ensureExplotacion(usuarioId);

            // 2) Crear LOTE + hijos 1:1
            String nombreLote = "Lote Demo " + System.currentTimeMillis();
            String idLote = loteService.crearLoteConHijos(
                    explotacionId,
                    nombreLote,
                    "Ibérico 100%",
                    "Rojo",
                    1,          // estado
                    120         // nIniciales (y nDisponibles)
            );

            Log.i(TAG, "Creado Lote id=" + idLote + " nombre=" + nombreLote);

            // 3) Verificar inserciones (contar filas relacionadas)
            int lotesCount = count("lotes", "id=?", new String[]{idLote});
            int parCount   = count("parideras", "id_lote=?", new String[]{idLote});
            int cubCount   = count("cubriciones", "id_lote=?", new String[]{idLote});
            int itaCount   = count("itaca", "id_lote=?", new String[]{idLote});

            Log.i(TAG, "lotes="+lotesCount+" parideras="+parCount+" cubriciones="+cubCount+" itaca="+itaCount);

            // al final de runTest(), después del log de conteos:
            runStockTest(idLote, explotacionId);

            Toast.makeText(this, "OK: Lote + hijos creados", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error en test", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void runStockTest(String idLote, String idExplotacion) {
        final String TAG = "GPv3-Test";
        // 1) leer disponibles antes
        int before = queryInt("SELECT nDisponibles FROM lotes WHERE id=?",
                new String[]{idLote});
        Log.i(TAG, "Stock antes = " + before);

        // 2) salida que descuenta stock (p.ej. 10)
        String fecha = FechaUtils.ahoraIso();
        loteService.agregarSalida(idLote, idExplotacion, fecha,
                10, "Venta mercado", "prueba salida", true);

        int afterSalida = queryInt("SELECT nDisponibles FROM lotes WHERE id=?",
                new String[]{idLote});
        Log.i(TAG, "Tras SALIDA(10) = " + afterSalida);

        // 3) baja que descuenta stock (p.ej. 3)
        loteService.agregarBaja(idLote, idExplotacion, FechaUtils.ahoraIso(),
                3, "muerte");

        int afterBaja = queryInt("SELECT nDisponibles FROM lotes WHERE id=?",
                new String[]{idLote});
        Log.i(TAG, "Tras BAJA(3) = " + afterBaja);

        // 4) listado rápido de movimientos
        dump("SELECT id, fecha, cantidad, destino FROM salidas WHERE id_lote=?",
                new String[]{idLote}, 4, "SALIDA");
        dump("SELECT id, fecha, cantidad, causa FROM bajas WHERE id_lote=?",
                new String[]{idLote}, 4, "BAJA");
    }

    private int queryInt(String sql, String[] args) {
        try (android.database.Cursor c =
                     dbh.getReadableDatabase().rawQuery(sql, args)) {
            if (c.moveToFirst()) return c.getInt(0);
            return 0;
        }
    }

    private void dump(String sql, String[] args, int cols, String label) {
        try (android.database.Cursor c =
                     dbh.getReadableDatabase().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                StringBuilder sb = new StringBuilder(label).append(": ");
                for (int i = 0; i < cols; i++) {
                    sb.append(c.getString(i)).append(i == cols - 1 ? "" : " | ");
                }
                android.util.Log.i("GPv3-Test", sb.toString());
            }
        }
    }

    // Crea usuario de prueba si no existe
    private String ensureUsuario() {
        SQLiteDatabase db = dbh.getWritableDatabase();
        String email = "demo@gestipork.local";
        String id = querySingleString("SELECT id FROM usuarios WHERE email=? LIMIT 1", new String[]{email});
        if (id != null) return id;

        id = IdUtils.uuid();
        db.execSQL("INSERT INTO usuarios (id, email, nombre, fecha_actualizacion, sincronizado, eliminado) " +
                        "VALUES (?, ?, ?, ?, 0, 0)",
                new Object[]{id, email, "Usuario Demo", FechaUtils.ahoraIso()});
        Log.i(TAG, "Usuario demo creado: " + id);
        return id;
    }

    // Crea explotación de prueba si no existe
    private String ensureExplotacion(String usuarioId) {
        String nombre = "Explotación Demo";
        String id = querySingleString("SELECT id FROM explotaciones WHERE nombre=? AND id_usuario=? LIMIT 1",
                new String[]{nombre, usuarioId});
        if (id != null) return id;

        SQLiteDatabase db = dbh.getWritableDatabase();
        id = IdUtils.uuid();
        db.execSQL("INSERT INTO explotaciones (id, id_usuario, nombre, fecha_actualizacion, sincronizado, eliminado) " +
                        "VALUES (?, ?, ?, ?, 0, 0)",
                new Object[]{id, usuarioId, nombre, FechaUtils.ahoraIso()});
        Log.i(TAG, "Explotación demo creada: " + id);
        return id;
    }

    private int count(String table, String where, String[] args) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + table + (where != null ? " WHERE " + where : "");
        try (Cursor c = db.rawQuery(sql, args)) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    private String querySingleString(String sql, String[] args) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, args)) {
            if (c.moveToFirst()) return c.getString(0);
        }
        return null;
    }

    //ñkdndfdkksdfpsefpbsdkjnksadnfñksnd
}
