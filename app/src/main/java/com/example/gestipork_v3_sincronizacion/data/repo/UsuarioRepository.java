package com.example.gestipork_v3_sincronizacion.data.repo;

import android.util.Log;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.UsuarioService;
import com.google.gson.JsonObject;
import java.util.List;
import retrofit2.Response;

public class UsuarioRepository {
    private static final String TAG = "UsuarioRepository";
    private final UsuarioService api;

    public UsuarioRepository() {
        this.api = ApiClient.get().create(UsuarioService.class);
    }

    /** Crea/actualiza tu propio perfil en public.usuarios */
    public boolean upsertSelf(String userId, String email, String nombre) {
        try {
            if (userId == null || userId.isEmpty() || email == null || email.isEmpty()) {
                Log.w(TAG, "upsertSelf: userId/email vacíos");
                return false;
            }
            JsonObject body = new JsonObject();
            body.addProperty("id", userId);
            body.addProperty("email", email);
            if (nombre != null) body.addProperty("nombre", nombre);

            // ⬇️ Marca de tiempo para resolución de conflictos y PULL incremental
            body.addProperty("fecha_actualizacion", FechaUtils.ahoraIso());

            Response<Void> r = api.upsert(body).execute();
            if (r.isSuccessful()) {
                Log.i(TAG, "upsertSelf OK");
                return true;
            } else {
                Log.w(TAG, "upsertSelf fallo: " + r.code());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "upsertSelf error", e);
            return false;
        }
    }

    /** (Opcional) Obtener tu perfil actual */
    public JsonObject getMe(String userId) {
        try {
            Response<List<JsonObject>> r = api.getById("eq." + userId, "*").execute();
            if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                return r.body().get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "getMe error", e);
        }
        return null;
    }
}
