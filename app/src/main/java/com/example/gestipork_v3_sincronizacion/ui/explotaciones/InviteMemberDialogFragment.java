package com.example.gestipork_v3_sincronizacion.ui.explotaciones;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.base.NetworkUtils;
import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.ExplotacionMembersService;
import com.example.gestipork_v3_sincronizacion.network.services.UsuarioService;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InviteMemberDialogFragment extends DialogFragment {

    private static final String ARG_ID_EXPLOT = "id_explotacion";
    private static final String ARG_NOMBRE    = "nombre_explotacion";

    public static InviteMemberDialogFragment newInstance(String idExplotacion, String nombre) {
        InviteMemberDialogFragment f = new InviteMemberDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ID_EXPLOT, idExplotacion);
        b.putString(ARG_NOMBRE, nombre);
        f.setArguments(b);
        return f;
    }

    private EditText inputEmail;
    private Spinner spinnerRol;
    private AlertDialog dialogRef; // para controlar botones

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String nombre = getArguments() != null ? getArguments().getString(ARG_NOMBRE) : "";

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_invite_member, null);
        inputEmail = view.findViewById(R.id.inputEmail);
        spinnerRol = view.findViewById(R.id.spinnerRol);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("manager","employee","viewer")
        );
        spinnerRol.setAdapter(adapter);

        dialogRef = new AlertDialog.Builder(requireContext())
                .setTitle("Compartir: " + (TextUtils.isEmpty(nombre) ? "Explotación" : nombre))
                .setView(view)
                .setPositiveButton("Invitar", null) // lo reemplazamos en onStart para no cerrar auto
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();
        return dialogRef;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (dialogRef != null) {
            Button positive = dialogRef.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> onInvitePressed(positive));
        }
    }

    private void onInvitePressed(Button positiveBtn) {
        String idExplot = getArguments() != null ? getArguments().getString(ARG_ID_EXPLOT) : null;
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String rol = (String) spinnerRol.getSelectedItem();

        if (TextUtils.isEmpty(idExplot)) {
            Toast.makeText(requireContext(), "Explotación no válida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Email no válido", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtils.hayInternet(requireContext())) {
            Toast.makeText(requireContext(), "Se requiere conexión para invitar", Toast.LENGTH_LONG).show();
            return;
        }

        setButtonsEnabled(false, positiveBtn);

        UsuarioService usuarioApi = ApiClient.get().create(UsuarioService.class);
        ExplotacionMembersService membersApi = ApiClient.get().create(ExplotacionMembersService.class);

        // 1) Buscar usuario por email (debe existir)
        usuarioApi.getByEmail("eq." + email, "id,email").enqueue(new Callback<List<JsonObject>>() {
            @Override public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    setButtonsEnabled(true, positiveBtn);
                    Toast.makeText(requireContext(), "Error buscando usuario", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<JsonObject> lista = res.body();
                if (lista.isEmpty() || !lista.get(0).has("id")) {
                    setButtonsEnabled(true, positiveBtn);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Usuario no registrado")
                            .setMessage("No existe ningún usuario con el email:\n\n" + email +
                                    "\n\nPídele que se registre en la app antes de poder compartir la explotación.")
                            .setPositiveButton("Entendido", null)
                            .show();
                    return;
                }

                // 2) Conceder acceso (upsert membresía accepted)
                String idUsuario = lista.get(0).get("id").getAsString();

                ExplotacionMember m = new ExplotacionMember();
                m.setId(null);
                m.setIdExplotacion(idExplot);
                m.setIdUsuario(idUsuario);
                m.setRol(rol);
                m.setEstadoInvitacion("accepted");

                membersApi.upsert(Collections.singletonList(m)).enqueue(new Callback<List<ExplotacionMember>>() {
                    @Override public void onResponse(Call<List<ExplotacionMember>> call, Response<List<ExplotacionMember>> res2) {
                        setButtonsEnabled(true, positiveBtn);

                        if (res2.isSuccessful() && res2.body() != null && !res2.body().isEmpty()) {
                            // 2.1 Guardar localmente la membresía creada (para que el owner la vea en su SQLite)
                            guardarMembresiaLocal(res2.body().get(0));

                            // 2.2 Informar al owner: éxito
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Acceso concedido")
                                    .setMessage("Se ha concedido acceso a " + email +
                                            " con el rol \"" + rol + "\".\n" +
                                            "Le aparecerá la explotación tras sincronizar su app.")
                                    .setPositiveButton("Ok", (d,w) -> dismissAllowingStateLoss())
                                    .show();

                        } else {
                            int code = res2.code();
                            if (code == 401) {
                                Toast.makeText(requireContext(), "Sesión caducada. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
                            } else if (code == 403) {
                                Toast.makeText(requireContext(), "Permisos insuficientes para invitar (RLS).", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(), "Error concediendo acceso (" + code + ")", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override public void onFailure(Call<List<ExplotacionMember>> call, Throwable t) {
                        setButtonsEnabled(true, positiveBtn);
                        Toast.makeText(requireContext(), "Fallo de red al conceder acceso", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                setButtonsEnabled(true, positiveBtn);
                Toast.makeText(requireContext(), "Fallo de red buscando usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setButtonsEnabled(boolean enabled, Button positiveBtn) {
        try {
            positiveBtn.setEnabled(enabled);
            if (dialogRef != null) {
                Button neg = dialogRef.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (neg != null) neg.setEnabled(enabled);
            }
        } catch (Exception ignored) {}
    }

    // Guarda la membresía en SQLite del dispositivo del owner
    private void guardarMembresiaLocal(ExplotacionMember m) {
        try {
            DBHelper dbh = new DBHelper(requireContext());
            android.database.sqlite.SQLiteDatabase wdb = dbh.getWritableDatabase();
            android.content.ContentValues v = new android.content.ContentValues();

            String id = m.getId() != null ? m.getId() : java.util.UUID.randomUUID().toString();
            v.put("id", id);
            v.put("id_explotacion", m.getIdExplotacion());
            v.put("id_usuario", m.getIdUsuario());
            v.put("rol", m.getRol());
            v.put("estado_invitacion", m.getEstadoInvitacion() == null ? "accepted" : m.getEstadoInvitacion());
            v.put("fecha_actualizacion", FechaUtils.ahoraIso());
            v.put("sincronizado", 1); // ya subido al servidor

            wdb.insertWithOnConflict("explotacion_members", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception ignored) { }
    }
}
