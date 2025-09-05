package com.example.gestipork_v3_sincronizacion.ui.explotaciones;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.gestipork_v3_sincronizacion.R;

public class RoleInfoDialogFragment extends DialogFragment {
    private static final String ARG_NOMBRE = "nombre_explotacion";
    private static final String ARG_ROL    = "rol";

    public static RoleInfoDialogFragment newInstance(String nombreExplotacion, String rol) {
        RoleInfoDialogFragment f = new RoleInfoDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_NOMBRE, nombreExplotacion);
        b.putString(ARG_ROL, rol);
        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String nombre = getArguments() != null ? getArguments().getString(ARG_NOMBRE) : "";
        String rol    = getArguments() != null ? getArguments().getString(ARG_ROL)    : "";

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_role_info, null);
        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvRol   = v.findViewById(R.id.tvRol);
        TextView tvPerms = v.findViewById(R.id.tvPermisos);

        tvTitle.setText("Ahora tienes acceso a: " + (TextUtils.isEmpty(nombre) ? "Explotación" : nombre));
        tvRol.setText("Tu rol: " + rol);
        tvPerms.setText(permisosTexto(rol));

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setPositiveButton("Entendido", (d, w) -> d.dismiss())
                .create();
    }

    private String permisosTexto(String rol) {
        if ("owner".equals(rol)) {
            return "• Gestionar miembros\n• Borrar explotación\n• Editar datos (lotes, acciones, etc.)\n• Ver datos";
        } else if ("manager".equals(rol)) {
            return "• Gestionar miembros (no al owner)\n• Editar datos (lotes, acciones, etc.)\n• Ver datos";
        } else if ("employee".equals(rol)) {
            return "• Editar datos (lotes, acciones, etc.)\n• Ver datos";
        } else {
            return "• Ver datos";
        }
    }
}
