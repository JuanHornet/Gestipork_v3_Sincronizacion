package com.example.gestipork_v3_sincronizacion.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.base.IdUtils;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "gestipork_v3.db";
    private static final int DB_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ===================== TABLAS BASE =====================

    private static final String CREATE_TABLE_USUARIOS =
            "CREATE TABLE usuarios (" +
                    "id TEXT PRIMARY KEY, " +
                    "email TEXT, " +
                    "nombre TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT" +
                    ")";

    private static final String SQL_CREATE_EXPLOTACION_MEMBERS =
            "CREATE TABLE IF NOT EXISTS explotacion_members (" +
                    " id TEXT PRIMARY KEY," +
                    " id_explotacion TEXT NOT NULL," +
                    " id_usuario TEXT NOT NULL," +
                    " rol TEXT NOT NULL CHECK (rol IN ('owner','manager','employee','viewer'))," +
                    " invitado_por TEXT," +
                    " estado_invitacion TEXT NOT NULL DEFAULT 'accepted' CHECK (estado_invitacion IN ('pending','accepted','revoked'))," +
                    " fecha_actualizacion TEXT NOT NULL," +
                    " sincronizado INTEGER NOT NULL DEFAULT 0" +
                    ");";

    private static final String SQL_IDX_EXPMEM_UNIQUE =
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_explomem_explot_usu " +
                    "ON explotacion_members(id_explotacion, id_usuario);";

    private static final String SQL_IDX_EXPMEM_USUARIO =
            "CREATE INDEX IF NOT EXISTS idx_explomem_usuario " +
                    "ON explotacion_members(id_usuario);";

    private static final String SQL_IDX_EXPMEM_EXPLOT =
            "CREATE INDEX IF NOT EXISTS idx_explomem_explot " +
                    "ON explotacion_members(id_explotacion);";


    private static final String CREATE_TABLE_EXPLOTACIONES =
            "CREATE TABLE explotaciones (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_usuario TEXT, " +          // FK -> usuarios.id
                    "nombre TEXT, " +              // visible
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_usuario) REFERENCES usuarios(id)" +
                    ")";
    private static final String CREATE_INDEX_EXPLOTACIONES_USUARIO =
            "CREATE INDEX idx_explotaciones_usuario ON explotaciones(id_usuario)";

    // ===================== LOTES (AGGREGATE ROOT) =====================

    private static final String CREATE_TABLE_LOTES =
            "CREATE TABLE lotes (" +
                    "id TEXT PRIMARY KEY, " +       // UUID lote
                    "id_explotacion TEXT, " +       // FK -> explotaciones.id
                    "nDisponibles INTEGER, " +
                    "nIniciales INTEGER, " +
                    "nombre_lote TEXT, " +          // visual
                    "raza TEXT, " +
                    "estado INTEGER, " +            // 0/1 u otros
                    "sincronizado INTEGER DEFAULT 0, " +
                    "fecha_actualizacion TEXT, " +
                    "color TEXT, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_LOTES_EXPLOTACION =
            "CREATE INDEX idx_lotes_explotacion ON lotes(id_explotacion)";

    // ===================== RELACIONES 1:1 =====================

    private static final String CREATE_TABLE_PARIDERAS =
            "CREATE TABLE parideras (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "nombre_lote TEXT, " +          // visual
                    "nMadres INTEGER, " +
                    "nPadres INTEGER, " +
                    "fechaInicio TEXT, " +
                    "fechaFin TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_PARIDERAS_LOTE =
            "CREATE INDEX idx_parideras_lote ON parideras(id_lote)";
    private static final String CREATE_INDEX_PARIDERAS_EXPLOTACION =
            "CREATE INDEX idx_parideras_explotacion ON parideras(id_explotacion)";
    private static final String CREATE_UX_PARIDERAS_LOTE =
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_parideras_lote ON parideras(id_lote, id_explotacion)";

    private static final String CREATE_TABLE_CUBRICIONES =
            "CREATE TABLE cubriciones (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "nombre_lote TEXT, " +
                    "nMadres INTEGER, " +
                    "nPadres INTEGER, " +
                    "fechaInicioCubricion TEXT, " +
                    "fechaFinCubricion TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_CUBRICIONES_LOTE =
            "CREATE INDEX idx_cubriciones_lote ON cubriciones(id_lote)";
    private static final String CREATE_INDEX_CUBRICIONES_EXPLOTACION =
            "CREATE INDEX idx_cubriciones_explotacion ON cubriciones(id_explotacion)";
    private static final String CREATE_UX_CUBRICIONES_LOTE =
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_cubriciones_lote ON cubriciones(id_lote, id_explotacion)";

    private static final String CREATE_TABLE_ITACA =
            "CREATE TABLE itaca (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "nombre_lote TEXT, " +
                    "nAnimales INTEGER, " +
                    "nMadres INTEGER, " +
                    "nPadres INTEGER, " +
                    "fechaPrimerNacimiento TEXT, " +
                    "fechaUltimoNacimiento TEXT, " +
                    "raza TEXT, " +
                    "dcer TEXT," +
                    "color TEXT, " +
                    "crotalesSolicitados INTEGER, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_ITACA_LOTE =
            "CREATE INDEX idx_itaca_lote ON itaca(id_lote)";
    private static final String CREATE_INDEX_ITACA_EXPLOTACION =
            "CREATE INDEX idx_itaca_explotacion ON itaca(id_explotacion)";
    private static final String CREATE_UX_ITACA_LOTE =
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_itaca_lote ON itaca(id_lote, id_explotacion)";

    // ===================== RELACIONES 1:N =====================

    private static final String CREATE_TABLE_ACCIONES =
            "CREATE TABLE acciones (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "tipo TEXT, " +
                    "fecha TEXT, " +
                    "cantidad INTEGER, " +
                    "observaciones TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_ACCIONES_LOTE =
            "CREATE INDEX idx_acciones_lote ON acciones(id_lote)";
    private static final String CREATE_INDEX_ACCIONES_EXPLOTACION =
            "CREATE INDEX idx_acciones_explotacion ON acciones(id_explotacion)";
    private static final String CREATE_INDEX_ACCIONES_FECHA =
            "CREATE INDEX idx_acciones_fecha ON acciones(fecha)";

    // SALIDAS
    private static final String CREATE_TABLE_SALIDAS =
            "CREATE TABLE salidas (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "fecha TEXT, " +
                    "cantidad INTEGER, " +
                    "destino TEXT, " +
                    "observaciones TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_SALIDAS_LOTE =
            "CREATE INDEX idx_salidas_lote ON salidas(id_lote)";
    private static final String CREATE_INDEX_SALIDAS_EXPLOTACION =
            "CREATE INDEX idx_salidas_explotacion ON salidas(id_explotacion)";
    private static final String CREATE_INDEX_SALIDAS_FECHA =
            "CREATE INDEX idx_salidas_fecha ON salidas(fecha)";

    // BAJAS
    private static final String CREATE_TABLE_BAJAS =
            "CREATE TABLE bajas (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "fecha TEXT, " +
                    "cantidad INTEGER, " +
                    "causa TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_BAJAS_LOTE =
            "CREATE INDEX idx_bajas_lote ON bajas(id_lote)";
    private static final String CREATE_INDEX_BAJAS_EXPLOTACION =
            "CREATE INDEX idx_bajas_explotacion ON bajas(id_explotacion)";
    private static final String CREATE_INDEX_BAJAS_FECHA =
            "CREATE INDEX idx_bajas_fecha ON bajas(fecha)";

    // NOTAS
    private static final String CREATE_TABLE_NOTAS =
            "CREATE TABLE notas (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "fecha TEXT, " +
                    "texto TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_NOTAS_LOTE =
            "CREATE INDEX idx_notas_lote ON notas(id_lote)";
    private static final String CREATE_INDEX_NOTAS_EXPLOTACION =
            "CREATE INDEX idx_notas_explotacion ON notas(id_explotacion)";
    private static final String CREATE_INDEX_NOTAS_FECHA =
            "CREATE INDEX idx_notas_fecha ON notas(fecha)";

    // PESOS
    private static final String CREATE_TABLE_PESOS =
            "CREATE TABLE pesos (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "fecha TEXT, " +
                    "nAnimales INTEGER, " +
                    "pesoTotal REAL, " +
                    "pesoMedio REAL, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_PESOS_LOTE =
            "CREATE INDEX idx_pesos_lote ON pesos(id_lote)";
    private static final String CREATE_INDEX_PESOS_EXPLOTACION =
            "CREATE INDEX idx_pesos_explotacion ON pesos(id_explotacion)";
    private static final String CREATE_INDEX_PESOS_FECHA =
            "CREATE INDEX idx_pesos_fecha ON pesos(fecha)";

    // CONTAR
    private static final String CREATE_TABLE_CONTAR =
            "CREATE TABLE contar (" +
                    "id TEXT PRIMARY KEY, " +
                    "id_lote TEXT, " +
                    "id_explotacion TEXT, " +
                    "fecha TEXT, " +
                    "nAnimales INTEGER, " +
                    "observaciones TEXT, " +
                    "fecha_actualizacion TEXT, " +
                    "sincronizado INTEGER DEFAULT 0, " +
                    "eliminado INTEGER DEFAULT 0, " +
                    "fecha_eliminado TEXT, " +
                    "FOREIGN KEY(id_lote) REFERENCES lotes(id), " +
                    "FOREIGN KEY(id_explotacion) REFERENCES explotaciones(id)" +
                    ")";
    private static final String CREATE_INDEX_CONTAR_LOTE =
            "CREATE INDEX idx_contar_lote ON contar(id_lote)";
    private static final String CREATE_INDEX_CONTAR_EXPLOTACION =
            "CREATE INDEX idx_contar_explotacion ON contar(id_explotacion)";
    private static final String CREATE_INDEX_CONTAR_FECHA =
            "CREATE INDEX idx_contar_fecha ON contar(fecha)";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USUARIOS);

        db.execSQL(CREATE_TABLE_EXPLOTACIONES);
        db.execSQL(CREATE_INDEX_EXPLOTACIONES_USUARIO);

        db.execSQL(CREATE_TABLE_LOTES);
        db.execSQL(CREATE_INDEX_LOTES_EXPLOTACION);

        // 1:1
        db.execSQL(CREATE_TABLE_PARIDERAS);
        db.execSQL(CREATE_INDEX_PARIDERAS_LOTE);
        db.execSQL(CREATE_INDEX_PARIDERAS_EXPLOTACION);
        db.execSQL(CREATE_UX_PARIDERAS_LOTE);

        db.execSQL(CREATE_TABLE_CUBRICIONES);
        db.execSQL(CREATE_INDEX_CUBRICIONES_LOTE);
        db.execSQL(CREATE_INDEX_CUBRICIONES_EXPLOTACION);
        db.execSQL(CREATE_UX_CUBRICIONES_LOTE);

        db.execSQL(CREATE_TABLE_ITACA);
        db.execSQL(CREATE_INDEX_ITACA_LOTE);
        db.execSQL(CREATE_INDEX_ITACA_EXPLOTACION);
        db.execSQL(CREATE_UX_ITACA_LOTE);

        // 1:N
        db.execSQL(CREATE_TABLE_ACCIONES);
        db.execSQL(CREATE_INDEX_ACCIONES_LOTE);
        db.execSQL(CREATE_INDEX_ACCIONES_EXPLOTACION);
        db.execSQL(CREATE_INDEX_ACCIONES_FECHA);

        db.execSQL(CREATE_TABLE_SALIDAS);
        db.execSQL(CREATE_INDEX_SALIDAS_LOTE);
        db.execSQL(CREATE_INDEX_SALIDAS_EXPLOTACION);
        db.execSQL(CREATE_INDEX_SALIDAS_FECHA);

        db.execSQL(CREATE_TABLE_BAJAS);
        db.execSQL(CREATE_INDEX_BAJAS_LOTE);
        db.execSQL(CREATE_INDEX_BAJAS_EXPLOTACION);
        db.execSQL(CREATE_INDEX_BAJAS_FECHA);

        db.execSQL(CREATE_TABLE_NOTAS);
        db.execSQL(CREATE_INDEX_NOTAS_LOTE);
        db.execSQL(CREATE_INDEX_NOTAS_EXPLOTACION);
        db.execSQL(CREATE_INDEX_NOTAS_FECHA);

        db.execSQL(CREATE_TABLE_PESOS);
        db.execSQL(CREATE_INDEX_PESOS_LOTE);
        db.execSQL(CREATE_INDEX_PESOS_EXPLOTACION);
        db.execSQL(CREATE_INDEX_PESOS_FECHA);

        db.execSQL(CREATE_TABLE_CONTAR);
        db.execSQL(CREATE_INDEX_CONTAR_LOTE);
        db.execSQL(CREATE_INDEX_CONTAR_EXPLOTACION);
        db.execSQL(CREATE_INDEX_CONTAR_FECHA);

        db.execSQL(SQL_CREATE_EXPLOTACION_MEMBERS);
        db.execSQL(SQL_IDX_EXPMEM_UNIQUE);
        db.execSQL(SQL_IDX_EXPMEM_USUARIO);
        db.execSQL(SQL_IDX_EXPMEM_EXPLOT);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 1:N
        db.execSQL("DROP TABLE IF EXISTS contar");
        db.execSQL("DROP TABLE IF EXISTS pesos");
        db.execSQL("DROP TABLE IF EXISTS notas");
        db.execSQL("DROP TABLE IF EXISTS bajas");
        db.execSQL("DROP TABLE IF EXISTS salidas");
        db.execSQL("DROP TABLE IF EXISTS acciones");

        // 1:1
        db.execSQL("DROP TABLE IF EXISTS itaca");
        db.execSQL("DROP TABLE IF EXISTS cubriciones");
        db.execSQL("DROP TABLE IF EXISTS parideras");

        // raíz
        db.execSQL("DROP TABLE IF EXISTS lotes");
        db.execSQL("DROP TABLE IF EXISTS explotaciones");
        db.execSQL("DROP TABLE IF EXISTS usuarios");

        db.execSQL("DROP TABLE IF EXISTS explotacion_members");

        onCreate(db);
    }

    /**
     * Crea un Lote y, automáticamente, sus registros 1:1 en Parideras, Cubriciones e Itaca.
     * Todo en una sola transacción. Devuelve el id (UUID) del lote creado.
     */
    public String crearLoteConHijos(String idExplotacion,
                                    String nombreLote,
                                    String raza,
                                    String color,
                                    int estado,
                                    int nIniciales) {

        String now = FechaUtils.ahoraIso();

        String idLote = IdUtils.uuid();
        String idParidera = IdUtils.uuid();
        String idCubricion = IdUtils.uuid();
        String idItaca = IdUtils.uuid();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // LOTE
            ContentValues vLote = new ContentValues();
            vLote.put("id", idLote);
            vLote.put("id_explotacion", idExplotacion);
            vLote.put("nDisponibles", nIniciales);
            vLote.put("nIniciales", nIniciales);
            vLote.put("nombre_lote", nombreLote);
            vLote.put("raza", raza);
            vLote.put("estado", estado);
            vLote.put("sincronizado", 0);
            vLote.put("fecha_actualizacion", now);
            vLote.put("color", color);
            vLote.put("eliminado", 0);
            db.insertOrThrow("lotes", null, vLote);

            // PARIDERA
            ContentValues vPar = new ContentValues();
            vPar.put("id", idParidera);
            vPar.put("id_lote", idLote);
            vPar.put("id_explotacion", idExplotacion);
            vPar.put("nombre_lote", nombreLote);
            vPar.put("nMadres", 0);
            vPar.put("nPadres", 0);
            vPar.put("fechaInicio", (String) null);
            vPar.put("fechaFin", (String) null);
            vPar.put("fecha_actualizacion", now);
            vPar.put("sincronizado", 0);
            vPar.put("eliminado", 0);
            db.insertOrThrow("parideras", null, vPar);

            // CUBRICION
            ContentValues vCub = new ContentValues();
            vCub.put("id", idCubricion);
            vCub.put("id_lote", idLote);
            vCub.put("id_explotacion", idExplotacion);
            vCub.put("nombre_lote", nombreLote);
            vCub.put("nMadres", 0);
            vCub.put("nPadres", 0);
            vCub.put("fechaInicioCubricion", (String) null);
            vCub.put("fechaFinCubricion", (String) null);
            vCub.put("fecha_actualizacion", now);
            vCub.put("sincronizado", 0);
            vCub.put("eliminado", 0);
            db.insertOrThrow("cubriciones", null, vCub);

            // ITACA
            ContentValues vIta = new ContentValues();
            vIta.put("id", idItaca);
            vIta.put("id_lote", idLote);
            vIta.put("id_explotacion", idExplotacion);
            vIta.put("nombre_lote", nombreLote);
            vIta.put("nAnimales", 0);
            vIta.put("nMadres", 0);
            vIta.put("nPadres", 0);
            vIta.put("fechaPrimerNacimiento", (String) null);
            vIta.put("fechaUltimoNacimiento", (String) null);
            vIta.put("raza", raza);
            vIta.put("dcer", "");
            vIta.put("color", color);
            vIta.put("crotalesSolicitados", 0);
            vIta.put("fecha_actualizacion", now);
            vIta.put("sincronizado", 0);
            vIta.put("eliminado", 0);
            db.insertOrThrow("itaca", null, vIta);

            db.setTransactionSuccessful();
            return idLote;
        } catch (SQLiteConstraintException e) {
            throw e;
        } finally {
            db.endTransaction();
        }
    }
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
    public static void ensureCreated(Context ctx) {
        try (SQLiteDatabase db = new DBHelper(ctx).getWritableDatabase()) {
            android.util.Log.i("DBHelper", "DB path: " + ctx.getDatabasePath(DB_NAME).getAbsolutePath());
        }
    }

}

