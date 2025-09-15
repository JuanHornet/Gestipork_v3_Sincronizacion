// ui/explotaciones/MembersActivity.java
package com.example.gestipork_v3_sincronizacion.ui.explotaciones;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.auth.SessionManager;
import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember;
import com.example.gestipork_v3_sincronizacion.data.models.MemberRow;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.ExplotacionMembersService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MembersActivity extends AppCompatActivity {

    public static final String EXTRA_ID_EXPLOT = "id_explotacion";
    public static final String EXTRA_NOMBRE    = "nombre_explotacion";

    private Toolbar toolbar;
    private SwipeRefreshLayout swipe;
    private RecyclerView rv;
    private ProgressBar progress;

    private String idExplot;
    private String nombreExplot;
    private String currentUserId;

    private MembersAdapter adapter;
    private ExplotacionMembersService api;
    private DBHelper db;

    private static final String SELECT_JOIN =
            "id,id_explotacion,id_usuario,rol,estado_invitacion,fecha_actualizacion," +
                    "usuario:usuarios!fk_members_usuario(id,email,nombre)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members);

        idExplot    = getIntent().getStringExtra(EXTRA_ID_EXPLOT);
        nombreExplot= getIntent().getStringExtra(EXTRA_NOMBRE);
        currentUserId = new SessionManager(this).getUserId();

        toolbar = findViewById(R.id.toolbar_estandar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setTitle("Miembros" + (TextUtils.isEmpty(nombreExplot) ? "" : (" · " + nombreExplot)));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        swipe = findViewById(R.id.swipe);
        rv = findViewById(R.id.recycler);
        progress = findViewById(R.id.progress);

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new MembersAdapter();
        rv.setAdapter(adapter);

        api = ApiClient.get().create(ExplotacionMembersService.class);
        db  = new DBHelper(this);

        findViewById(R.id.fabInvite).setOnClickListener(v -> {
            InviteMemberDialogFragment
                    .newInstance(idExplot, nombreExplot)
                    .show(getSupportFragmentManager(), "invitar");
        });

        swipe.setOnRefreshListener(this::loadMembers);
        loadMembers();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()==android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void loadMembers() {
        if (TextUtils.isEmpty(idExplot)) {
            Toast.makeText(this, "Explotación no válida", Toast.LENGTH_SHORT).show();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        // Filtramos los revocados para no mostrarlos
        api.listarJoinUsuarios("eq."+idExplot, "neq.revoked", SELECT_JOIN, "rol.asc")
                .enqueue(new Callback<List<MemberRow>>() {
                    @Override public void onResponse(Call<List<MemberRow>> call, Response<List<MemberRow>> res) {
                        progress.setVisibility(View.GONE);
                        swipe.setRefreshing(false);
                        if (res.isSuccessful() && res.body()!=null) {
                            adapter.setData(res.body());
                            // guarda (opcional) en SQLite del owner para offline rápido
                            saveLocal(res.body());
                        } else {
                            Toast.makeText(MembersActivity.this, "No se pudo cargar ("+res.code()+")", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<List<MemberRow>> call, Throwable t) {
                        progress.setVisibility(View.GONE);
                        swipe.setRefreshing(false);
                        Toast.makeText(MembersActivity.this, "Fallo de red", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void saveLocal(List<MemberRow> members) {
        try {
            android.database.sqlite.SQLiteDatabase wdb = db.getWritableDatabase();
            // estrategia simple: upsert fila a fila
            for (MemberRow m : members) {
                android.content.ContentValues v = new android.content.ContentValues();
                String id = m.getId()!=null ? m.getId() : java.util.UUID.randomUUID().toString();
                v.put("id", id);
                v.put("id_explotacion", m.getIdExplotacion());
                v.put("id_usuario", m.getIdUsuario());
                v.put("rol", m.getRol());
                v.put("estado_invitacion", m.getEstadoInvitacion());
                v.put("fecha_actualizacion", m.getFechaActualizacion());
                v.put("sincronizado", 1);
                wdb.insertWithOnConflict("explotacion_members", null, v,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (Exception ignored) {}
    }

    // ===== Adapter =====
    private class MembersAdapter extends RecyclerView.Adapter<MemberVH> {
        private final List<MemberRow> data = new ArrayList<>();

        void setData(List<MemberRow> list) {
            data.clear();
            if (list!=null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull @Override public MemberVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_member, parent, false);
            return new MemberVH(v);
        }

        @Override public void onBindViewHolder(@NonNull MemberVH h, int pos) {
            MemberRow m = data.get(pos);
            String nombre = (m.getUsuario()!=null && !TextUtils.isEmpty(m.getUsuario().nombre))
                    ? m.getUsuario().nombre : "(Sin nombre)";
            String email  = (m.getUsuario()!=null && !TextUtils.isEmpty(m.getUsuario().email))
                    ? m.getUsuario().email : m.getIdUsuario();

            h.txtNombre.setText(nombre);
            h.txtEmail.setText(email);
            h.txtRol.setText(m.getRol());

            // Deshabilitar acciones contra owners (según reglas)
            boolean soyYo = !TextUtils.isEmpty(currentUserId) && currentUserId.equals(m.getIdUsuario());
            h.btnMenu.setOnClickListener(v -> showRowMenu(v, m, soyYo));
        }

        @Override public int getItemCount() { return data.size(); }

        private void showRowMenu(View anchor, MemberRow m, boolean soyYo) {
            PopupMenu pm = new PopupMenu(MembersActivity.this, anchor);
            pm.getMenuInflater().inflate(R.menu.menu_member_row, pm.getMenu());
            // reglas: no permitir tocar a owners desde aquí (transferencia aparte)
            if ("owner".equalsIgnoreCase(m.getRol())) {
                pm.getMenu().findItem(R.id.action_edit_role).setEnabled(false);
                pm.getMenu().findItem(R.id.action_revoke).setEnabled(false);
            }
            // opcional: no permitir que el usuario se auto-revoque si es el único owner
            pm.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit_role) {
                    showEditRoleDialog(m);
                    return true;
                } else if (id == R.id.action_revoke) {
                    confirmRevoke(m);
                    return true;
                }
                return false;
            });
            pm.show();
        }
    }

    private static class MemberVH extends RecyclerView.ViewHolder {
        TextView txtNombre, txtEmail, txtRol;
        ImageButton btnMenu;
        MemberVH(@NonNull View v) {
            super(v);
            txtNombre = v.findViewById(R.id.txtNombre);
            txtEmail  = v.findViewById(R.id.txtEmail);
            txtRol    = v.findViewById(R.id.txtRol);
            btnMenu   = v.findViewById(R.id.btnRowMenu);
        }
    }

    private void showEditRoleDialog(MemberRow m) {
        final String[] roles = new String[]{"manager","employee","viewer"};
        int pre = 2;
        for (int i=0;i<roles.length;i++) if (roles[i].equalsIgnoreCase(m.getRol())) { pre = i; break; }

        new AlertDialog.Builder(this)
                .setTitle("Editar rol")
                .setSingleChoiceItems(roles, pre, null)
                .setPositiveButton("Guardar", (d,w)->{
                    int idx = ((AlertDialog)d).getListView().getCheckedItemPosition();
                    String nuevo = roles[idx];
                    if (!nuevo.equalsIgnoreCase(m.getRol())) {
                        patchMember(m, nuevo, null);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmRevoke(MemberRow m) {
        new AlertDialog.Builder(this)
                .setTitle("Revocar acceso")
                .setMessage("¿Quitar acceso a " + (m.getUsuario()!=null ? m.getUsuario().email : m.getIdUsuario()) + "?")
                .setPositiveButton("Revocar", (d,w)-> patchMember(m, null, "revoked"))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void patchMember(MemberRow m, String nuevoRol, String nuevoEstado) {
        Map<String, Object> cambios = new HashMap<>();
        if (nuevoRol != null) cambios.put("rol", nuevoRol);
        if (nuevoEstado != null) cambios.put("estado_invitacion", nuevoEstado);
        cambios.put("fecha_actualizacion", FechaUtils.ahoraIso());

        ApiClient.get().create(ExplotacionMembersService.class)
                .patchPorId("eq."+m.getId(), cambios)
                .enqueue(new Callback<List<ExplotacionMember>>() {
                    @Override public void onResponse(Call<List<ExplotacionMember>> call, Response<List<ExplotacionMember>> res) {
                        if (res.isSuccessful()) {
                            if (nuevoRol != null)  m.setRol(nuevoRol);
                            if (nuevoEstado != null) m.setEstadoInvitacion(nuevoEstado);
                            m.setFechaActualizacion(FechaUtils.ahoraIso());
                            // Actualiza local
                            try {
                                android.database.sqlite.SQLiteDatabase wdb = db.getWritableDatabase();
                                android.content.ContentValues v = new android.content.ContentValues();
                                if (nuevoRol != null) v.put("rol", nuevoRol);
                                if (nuevoEstado != null) v.put("estado_invitacion", nuevoEstado);
                                v.put("fecha_actualizacion", m.getFechaActualizacion());
                                v.put("sincronizado", 1);
                                wdb.update("explotacion_members", v, "id=?", new String[]{ m.getId() });
                            } catch (Exception ignored) {}
                            // refrescar
                            loadMembers();
                        } else {
                            Toast.makeText(MembersActivity.this, "Error ("+res.code()+")", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<List<ExplotacionMember>> call, Throwable t) {
                        Toast.makeText(MembersActivity.this, "Fallo de red", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
