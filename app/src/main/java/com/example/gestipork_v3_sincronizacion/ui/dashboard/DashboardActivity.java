package com.example.gestipork_v3_sincronizacion.ui.dashboard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.auth.SessionManager;
import com.example.gestipork_v3_sincronizacion.base.Permisos;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.repo.ExplotacionMembersRepository;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.sync.workers.SyncWorker;
import com.example.gestipork_v3_sincronizacion.ui.explotaciones.InviteMemberDialogFragment;
import com.example.gestipork_v3_sincronizacion.ui.explotaciones.MembersActivity;
import com.example.gestipork_v3_sincronizacion.ui.explotaciones.RoleInfoDialogFragment;
import com.example.gestipork_v3_sincronizacion.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dashboard principal que muestra:
 *  - Selector de explotación
 *  - Resumen de lotes (Ibérico 100%, Cruz 50%, Total) + Aforo editable
 *  - Botón "Sincronizar ahora"
 *
 * Usa un RecyclerView con un único item (tu card de resumen).
 */
public class DashboardActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerExplotaciones;
    private TextView txtVacio;
    private RecyclerView recyclerResumen;
    private Button btnSincronizar;

    private DBHelper db;
    private ArrayAdapter<String> spinnerAdapter;
    private final List<String> explotacionNames = new ArrayList<>();
    private final List<String> explotacionIds = new ArrayList<>();
    private final Map<String, Integer> nameIndexById = new HashMap<>();

    private static final String PREFS_MEMBERS = "members_prefs";
    private static final String KEY_KNOWN_EXPL = "known_explotaciones_csv";

    private ResumenAdapter resumenAdapter;

    // Preferencias para guardar el Aforo por explotación
    private static final String PREFS_AFORO = "aforo_prefs";

    // Activity/Fragment de lotes (ajusta si usas otro)
    private static final String LOTES_TARGET_CLASS =
            "com.example.gestipork_v3_sincronizacion.ui.lotes.LotesActivity";

    // === NUEVO: membresías/roles ===
    private ExplotacionMembersRepository membersRepo;
    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ApiClient.setAppContext(getApplicationContext());
        db = new DBHelper(this);

        // NUEVO: init membresías + userId
        membersRepo = new ExplotacionMembersRepository(db);
        userId = new SessionManager(this).getUserId();

        bindViews();
        setupToolbar();
        setupRecycler();
        setupSyncButton();
        loadExplotacionesIntoSpinner();

        spinnerExplotaciones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < explotacionIds.size()) {
                    String expId = explotacionIds.get(position);
                    refreshResumen(expId);
                    // NUEVO: refrescar visibilidad del menú según rol
                    invalidateOptionsMenu();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar_estandar);
        spinnerExplotaciones = findViewById(R.id.spinner_explotaciones);
        txtVacio = findViewById(R.id.txtVacio);
        recyclerResumen = findViewById(R.id.recycler_resumen);
        btnSincronizar = findViewById(R.id.btnSincronizar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("GestiPork");
        toolbar.setNavigationIcon(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    // NUEVO: controlar visibilidad de “Compartir explotación” según rol
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemShare = menu.findItem(R.id.action_share_explotacion);
        boolean visible = false;
        String idExplotacion = getExplotacionSeleccionadaId();

        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(idExplotacion)) {
            String rol = membersRepo.rolDe(userId, idExplotacion);
            visible = (rol != null) && Permisos.puede("GESTION_MIEMBROS", rol);
        }
        if (itemShare != null) itemShare.setVisible(visible);
        return super.onPrepareOptionsMenu(menu);
    }

    private void setupRecycler() {
        resumenAdapter = new ResumenAdapter(this, new ResumenItem(0, 0, 0, 0, null));
        recyclerResumen.setAdapter(resumenAdapter);
    }

    private void setupSyncButton() {
        btnSincronizar.setOnClickListener(v -> {
            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
            WorkManager.getInstance(getApplicationContext()).enqueue(req);
            Toast.makeText(this, "Sincronización lanzada", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Carga explotaciones desde SQLite. (NUEVO) Si hay userId, muestra solo explotaciones
     * a las que pertenece (membresías aceptadas). Si no, muestra todas no eliminadas.
     */
    private void loadExplotacionesIntoSpinner() {
        explotacionNames.clear();
        explotacionIds.clear();
        nameIndexById.clear();

        SQLiteDatabase rdb = db.getReadableDatabase();
        Cursor c = null;

        try {
            if (!TextUtils.isEmpty(userId)) {
                // Conjunto de explotaciones autorizadas por membresía
                Set<String> autorizadas = membersRepo.explotacionesAutorizadas(userId);
                if (autorizadas == null) autorizadas = new HashSet<>();
                if (autorizadas.isEmpty()) {
                    // Nada autorizado -> lista vacía
                } else {
                    // Query dinámica con IN (?, ?, ...)
                    StringBuilder sb = new StringBuilder("SELECT id, nombre FROM explotaciones WHERE eliminado=0 AND id IN (");
                    String[] args = new String[autorizadas.size()];
                    int i = 0;
                    for (String id : autorizadas) {
                        if (i > 0) sb.append(",");
                        sb.append("?");
                        args[i++] = id;
                    }
                    sb.append(") ORDER BY nombre ASC");
                    c = rdb.rawQuery(sb.toString(), args);
                }
            } else {
                c = rdb.rawQuery("SELECT id, nombre FROM explotaciones WHERE eliminado=0 ORDER BY nombre ASC", null);
            }

            if (c != null) {
                while (c.moveToNext()) {
                    String id = c.getString(0);
                    String nombre = c.getString(1);
                    explotacionIds.add(id);
                    explotacionNames.add(nombre == null ? "(Sin nombre)" : nombre);
                    nameIndexById.put(id, explotacionIds.size() - 1);
                }
            }
        } finally {
            if (c != null) c.close();
        }

        if (explotacionIds.isEmpty()) {
            txtVacio.setVisibility(View.VISIBLE);
            spinnerExplotaciones.setEnabled(false);
            resumenAdapter.updateData(new ResumenItem(0, 0, 0, 0, null));
        } else {
            txtVacio.setVisibility(View.GONE);
            spinnerExplotaciones.setEnabled(true);
            spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, explotacionNames);
            spinnerExplotaciones.setAdapter(spinnerAdapter);
            spinnerExplotaciones.setSelection(0);
            refreshResumen(explotacionIds.get(0));
        }


        checkNewMembershipsAndShowInfoIfNeeded();

        // 👇️ para refrescar visibilidad de “Compartir” en el primer render
        invalidateOptionsMenu();
    }
    private void persistKnownExplotacionesState() {
        SharedPreferences sp = getSharedPreferences(PREFS_MEMBERS, MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String id : explotacionIds) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        sp.edit().putString(KEY_KNOWN_EXPL, sb.toString()).apply();
    }


    private void checkNewMembershipsAndShowInfoIfNeeded() {
        if (TextUtils.isEmpty(userId) || explotacionIds.isEmpty()) return;

        // set actual
        java.util.HashSet<String> current = new java.util.HashSet<>(explotacionIds);

        // set conocido guardado
        SharedPreferences sp = getSharedPreferences(PREFS_MEMBERS, MODE_PRIVATE);
        String prevCsv = sp.getString(KEY_KNOWN_EXPL, "");
        java.util.HashSet<String> prev = new java.util.HashSet<>();
        if (!TextUtils.isEmpty(prevCsv)) {
            for (String s : prevCsv.split(",")) if (!TextUtils.isEmpty(s)) prev.add(s);
        }

        // nuevas = current - prev
        current.removeAll(prev);
        if (!current.isEmpty()) {
            // muestra info de la primera nueva
            String nuevaId = current.iterator().next();
            String nombre = "(Explotación)";
            Integer idxObj = nameIndexById.get(nuevaId);
            int idx = idxObj != null ? idxObj : -1;
            if (idx >= 0 && idx < explotacionNames.size()) nombre = explotacionNames.get(idx);

            String rol = membersRepo.rolDe(userId, nuevaId);
            if (rol == null) rol = "viewer";
            // ⛔️ No mostrar a owner en su propio dispositivo
            if ("owner".equalsIgnoreCase(rol)) {
                // aun así actualizamos los conocidos para no re-detectarla
                persistKnownExplotacionesState();
                return;
            }

            // Importa la clase o usa FQN
            RoleInfoDialogFragment
                    .newInstance(nombre, rol)
                    .show(getSupportFragmentManager(), "role_info");
        }

        // actualizar conocidos = estado actual completo
        persistKnownExplotacionesState();

    }


    /** Recalcula el resumen de la explotación seleccionada y actualiza el card. */
    private void refreshResumen(String idExplotacion) {
        int aforo = getAforoFor(idExplotacion);

        SQLiteDatabase rdb = db.getReadableDatabase();
        int total = scalarInt(rdb, "SELECT COUNT(*) FROM lotes WHERE eliminado=0 AND id_explotacion=?",
                new String[]{idExplotacion});

        int ib100 = scalarInt(rdb,
                "SELECT COUNT(*) FROM lotes WHERE eliminado=0 AND id_explotacion=? " +
                        "AND (UPPER(raza) LIKE '%IB%' AND (raza LIKE '%100%' OR UPPER(raza) LIKE '%100%'))",
                new String[]{idExplotacion});

        int cruz50 = scalarInt(rdb,
                "SELECT COUNT(*) FROM lotes WHERE eliminado=0 AND id_explotacion=? " +
                        "AND (UPPER(raza) LIKE '%CRUZ%' OR UPPER(raza) LIKE '%50%')",
                new String[]{idExplotacion});

        ResumenItem item = new ResumenItem(ib100, cruz50, total, aforo, idExplotacion);
        resumenAdapter.updateData(item);
    }

    private int scalarInt(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = db.rawQuery(sql, args);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    private int getAforoFor(String idExplotacion) {
        SharedPreferences sp = getSharedPreferences(PREFS_AFORO, MODE_PRIVATE);
        return sp.getInt("aforo_" + idExplotacion, 0);
    }

    private void setAforoFor(String idExplotacion, int aforo) {
        SharedPreferences sp = getSharedPreferences(PREFS_AFORO, MODE_PRIVATE);
        sp.edit().putInt("aforo_" + idExplotacion, aforo).apply();
    }

    // ======================= Adapter del resumen (un único item) =======================

    private static class ResumenItem {
        final int ib100;
        final int cruz50;
        final int total;
        final int aforo;
        final String idExplotacion;

        ResumenItem(int ib100, int cruz50, int total, int aforo, String idExplotacion) {
            this.ib100 = ib100;
            this.cruz50 = cruz50;
            this.total = total;
            this.aforo = aforo;
            this.idExplotacion = idExplotacion;
        }
    }

    private class ResumenAdapter extends RecyclerView.Adapter<ResumenAdapter.VH> {

        private final Context ctx;
        private ResumenItem data;

        ResumenAdapter(Context ctx, ResumenItem initial) {
            this.ctx = ctx;
            this.data = initial;
        }

        void updateData(ResumenItem d) {
            this.data = d;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dashboard_activity, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            h.lotesIb100.setText(String.valueOf(data.ib100));
            h.lotesCruz50.setText(String.valueOf(data.cruz50));
            h.lotesTotal.setText(String.valueOf(data.total));
            h.aforoInfo.setText(String.format(Locale.getDefault(), "Aforo máximo: %d animales", data.aforo));

            h.iconEditAforo.setOnClickListener(v -> {
                if (data.idExplotacion == null) {
                    Toast.makeText(ctx, "Selecciona una explotación", Toast.LENGTH_SHORT).show();
                    return;
                }
                showEditAforoDialog(data.idExplotacion, data.aforo);
            });

            h.btnVerLotes.setOnClickListener(v -> {
                try {
                    Class<?> target = Class.forName(LOTES_TARGET_CLASS);
                    Intent i = new Intent(ctx, target);
                    i.putExtra("id_explotacion", data.idExplotacion);
                    ctx.startActivity(i);
                } catch (ClassNotFoundException e) {
                    Toast.makeText(ctx, "Pantalla de Lotes no disponible aún", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return 1; }

        class VH extends RecyclerView.ViewHolder {
            TextView lotesIb100, lotesCruz50, lotesTotal, aforoInfo;
            ImageView iconEditAforo;
            Button btnVerLotes;

            VH(@NonNull View itemView) {
                super(itemView);
                lotesIb100 = itemView.findViewById(R.id.lotes_ib100);
                lotesCruz50 = itemView.findViewById(R.id.lotes_cruz50);
                lotesTotal = itemView.findViewById(R.id.lotes_total);
                aforoInfo = itemView.findViewById(R.id.text_aforo_info);
                iconEditAforo = itemView.findViewById(R.id.icon_edit_aforo);
                btnVerLotes = itemView.findViewById(R.id.btnVerLotes);
            }
        }
    }

    private void showEditAforoDialog(String idExplotacion, int current) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Aforo máximo");
        input.setText(String.valueOf(current));
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Editar aforo")
                .setView(input)
                .setPositiveButton("Guardar", (DialogInterface dialog, int which) -> {
                    String txt = input.getText() == null ? "" : input.getText().toString().trim();
                    int nuevo = 0;
                    try { nuevo = Integer.parseInt(txt); } catch (Exception ignored) {}
                    setAforoFor(idExplotacion, nuevo);
                    refreshResumen(idExplotacion);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // === NUEVO: helpers para item seleccionado del Spinner ===
    private String getExplotacionSeleccionadaId() {
        int pos = spinnerExplotaciones.getSelectedItemPosition();
        if (pos >= 0 && pos < explotacionIds.size()) return explotacionIds.get(pos);
        return null;
    }

    private String getExplotacionSeleccionadaNombre() {
        int pos = spinnerExplotaciones.getSelectedItemPosition();
        if (pos >= 0 && pos < explotacionNames.size()) return explotacionNames.get(pos);
        return "";
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // En DashboardActivity.onOptionsItemSelected(...)
        if (itemId == R.id.action_manage_members) {
            String idExplot = getExplotacionSeleccionadaId();
            String nombre   = getExplotacionSeleccionadaNombre();
            if (TextUtils.isEmpty(idExplot)) {
                Toast.makeText(this, "Selecciona una explotación", Toast.LENGTH_SHORT).show();
                return true;
            }
            Intent i = new Intent(this, com.example.gestipork_v3_sincronizacion.ui.explotaciones.MembersActivity.class);
            i.putExtra(MembersActivity.EXTRA_ID_EXPLOT, idExplot);
            i.putExtra(MembersActivity.EXTRA_NOMBRE, nombre);
            startActivity(i);
            return true;
        }


        if (itemId == R.id.menu_logout) {
            doLogout();
            return true;
        }

        if (itemId == R.id.action_add_explotacion) {
            showCrearExplotacionDialog();
            return true;
        }

        if (itemId == R.id.action_share_explotacion) {
            String idExplotacion = getExplotacionSeleccionadaId();
            String nombre = getExplotacionSeleccionadaNombre();
            if (TextUtils.isEmpty(idExplotacion)) {
                Toast.makeText(this, "Selecciona una explotación", Toast.LENGTH_SHORT).show();
                return true;
            }
            InviteMemberDialogFragment
                    .newInstance(idExplotacion, nombre)
                    .show(getSupportFragmentManager(), "invitar_miembro");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doLogout() {
        // 1) Cancelar sincronizaciones en curso
        WorkManager.getInstance(getApplicationContext()).cancelAllWork();

        // 2) Limpiar sesión (tokens, userId, email…)
        new SessionManager(this).clear();

        // 3) Resetear Retrofit/OkHttp para que no quede el Bearer cacheado
        ApiClient.reset();

        // 4) Ir a Login limpiando el back stack
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);

        // 5) Cerrar por si acaso
        finish();
    }


    private void showCrearExplotacionDialog() {
        final EditText input = new EditText(this);
        input.setHint("Nombre de la explotación");
        input.setSingleLine(true);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nueva explotación")
                .setView(input)
                .setPositiveButton("Crear", (d, w) -> {
                    String nombre = input.getText() == null ? "" : input.getText().toString().trim();
                    if (TextUtils.isEmpty(nombre)) {
                        Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    crearExplotacion(nombre);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void crearExplotacion(String nombre) {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Inicia sesión para crear explotaciones", Toast.LENGTH_LONG).show();
            return;
        }

        String idExplot = java.util.UUID.randomUUID().toString();
        String ahoraIso = com.example.gestipork_v3_sincronizacion.base.FechaUtils.ahoraIso();

        // 1) Insertar LOCAL en SQLite (explotaciones)
        android.database.sqlite.SQLiteDatabase wdb = db.getWritableDatabase();
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("id", idExplot);
        v.put("id_usuario", userId);
        v.put("nombre", nombre);
        v.put("fecha_actualizacion", ahoraIso);
        v.put("sincronizado", 0);
        v.put("eliminado", 0);
        wdb.insert("explotaciones", null, v);

        // 2) Insertar LOCAL en membresías como owner (para que aparezca en el spinner ya)
        android.content.ContentValues m = new android.content.ContentValues();
        m.put("id", java.util.UUID.randomUUID().toString());
        m.put("id_explotacion", idExplot);
        m.put("id_usuario", userId);
        m.put("rol", "owner");
        m.put("estado_invitacion", "accepted");
        m.put("fecha_actualizacion", ahoraIso);
        m.put("sincronizado", 0);
        wdb.insert("explotacion_members", null, m);

        // 3) Refrescar UI: añadimos al spinner y seleccionamos la nueva
        explotacionIds.add(idExplot);
        explotacionNames.add(nombre);
        nameIndexById.put(idExplot, explotacionIds.size() - 1);

        if (spinnerAdapter == null) {
            spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, explotacionNames);
            spinnerExplotaciones.setAdapter(spinnerAdapter);
        } else {
            spinnerAdapter.notifyDataSetChanged();
        }
        spinnerExplotaciones.setEnabled(true);
        int newPos = explotacionIds.size() - 1;
        spinnerExplotaciones.setSelection(newPos);
        refreshResumen(idExplot);
        invalidateOptionsMenu();
        persistKnownExplotacionesState();

        // 4) Intentar subir a Supabase (explotación + membresía owner)
        subirExplotacionYMembershipRemoto(idExplot, nombre);
    }

    private void subirExplotacionYMembershipRemoto(String idExplot, String nombre) {
        // Evitamos bloqueo de UI: simple hilo; si usas coroutines/Rx/WorkManager, adapta.
        new Thread(() -> {
            try {
                // 4.1 Upsert explotación
                com.example.gestipork_v3_sincronizacion.network.services.ExplotacionesService expApi =
                        com.example.gestipork_v3_sincronizacion.network.ApiClient.get()
                                .create(com.example.gestipork_v3_sincronizacion.network.services.ExplotacionesService.class);

                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("id", idExplot);
                row.put("id_usuario", userId);
                row.put("nombre", nombre);

                retrofit2.Response<Void> r1 = expApi.upsert(java.util.Collections.singletonList(row)).execute();

                if (r1.isSuccessful()) {
                    // marcar sincronizado local explotación
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put("sincronizado", 1);
                    db.getWritableDatabase().update("explotaciones", cv, "id=?", new String[]{idExplot});
                }

                // 4.2 Upsert membresía owner en servidor
                com.example.gestipork_v3_sincronizacion.network.services.ExplotacionMembersService memApi =
                        com.example.gestipork_v3_sincronizacion.network.ApiClient.get()
                                .create(com.example.gestipork_v3_sincronizacion.network.services.ExplotacionMembersService.class);

                com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember owner =
                        new com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember();
                owner.setId(null);
                owner.setIdExplotacion(idExplot);
                owner.setIdUsuario(userId);
                owner.setRol("owner");
                owner.setEstadoInvitacion("accepted");

                retrofit2.Response<java.util.List<com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember>> r2 =
                        memApi.upsert(java.util.Collections.singletonList(owner)).execute();

                if (r2.isSuccessful()) {
                    // marcar sincronizado local membresía (todas owner de esa explotación para este usuario)
                    android.content.ContentValues cv2 = new android.content.ContentValues();
                    cv2.put("sincronizado", 1);
                    db.getWritableDatabase().update(
                            "explotacion_members", cv2,
                            "id_explotacion=? AND id_usuario=?", new String[]{idExplot, userId}
                    );
                }

                runOnUiThread(() -> {
                    if (r1.isSuccessful() && r2.isSuccessful()) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Explotación creada")
                                .setMessage("Se ha creado \"" + nombre + "\" y asignado como propietario.")
                                .setPositiveButton("Ok", null)
                                .show();
                    } else {
                        Toast.makeText(this, "Creada localmente (sin conexión). Se subirá al sincronizar.", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Creada localmente. Error subiendo ahora; se subirá en la próxima sincronización.", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

}
