package com.example.gestipork_v3_sincronizacion.sync;

import android.content.Context;
import android.util.Log;

import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember;
import com.example.gestipork_v3_sincronizacion.data.repo.ExplotacionMembersRepository;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.ExplotacionMembersService;
import com.example.gestipork_v3_sincronizacion.base.FechaUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;

public class SincronizadorMembresias {
    private final ExplotacionMembersRepository repo;
    private final ExplotacionMembersService api;
    private static final String TAG = "SyncMembresias";

    public SincronizadorMembresias(Context ctx, DBHelper dbh) {
        this.repo = new ExplotacionMembersRepository(dbh);
        this.api = ApiClient.get().create(ExplotacionMembersService.class);
    }

    public Set<String> sincronizarYObtenerExplotaciones(String idUsuario) {
        try {
            // 1) DESCARGA remota de membresías del usuario
            Call<List<ExplotacionMember>> call = api.listarPorUsuario("eq." + idUsuario, "*");
            Response<List<ExplotacionMember>> res = call.execute();
            if (res.isSuccessful() && res.body() != null) {
                for (ExplotacionMember m : res.body()) {
                    m.setFechaActualizacion(FechaUtils.ahoraIso());
                    repo.upsertLocal(m, true); // marcar sincronizado
                }
            } else {
                Log.w(TAG, "Fallo GET membresías: " + (res.errorBody()!=null?res.errorBody().string():res.code()));
            }

            // 2) SUBIDA local pendiente
            List<ExplotacionMember> pendientes = repo.noSincronizados();
            if (!pendientes.isEmpty()) {
                Response<List<ExplotacionMember>> up = api.upsert(pendientes).execute();
                if (up.isSuccessful()) {
                    for (ExplotacionMember m : pendientes) repo.marcarSincronizado(m.getId());
                } else {
                    Log.w(TAG, "Fallo POST upsert membresías: " + (up.errorBody()!=null?up.errorBody().string():up.code()));
                }
            }

            // 3) Devolver set de explotaciones autorizadas
            return repo.explotacionesAutorizadas(idUsuario);

        } catch (Exception e) {
            Log.e(TAG, "Error sincronizando membresías", e);
            return new HashSet<>();
        }
    }
}
