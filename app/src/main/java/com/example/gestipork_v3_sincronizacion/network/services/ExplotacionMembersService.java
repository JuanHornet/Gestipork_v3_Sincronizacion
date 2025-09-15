package com.example.gestipork_v3_sincronizacion.network.services;

import com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember;
import com.example.gestipork_v3_sincronizacion.data.models.MemberRow;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ExplotacionMembersService {

    @GET("explotacion_members")
    Call<List<ExplotacionMember>> listarPorUsuario(
            @Query("id_usuario") String eqIdUsuario,
            @Query("select") String select
    );

    @GET("explotacion_members")
    Call<List<ExplotacionMember>> listarPorExplotacion(
            @Query("id_explotacion") String eqIdExplotacion,
            @Query("select") String select
    );

    @Headers({
            "Prefer: resolution=merge-duplicates, return=representation"
    })
    @POST("explotacion_members")
    Call<List<ExplotacionMember>> upsert(@Body List<ExplotacionMember> miembros);

    // ✅ PATCH que devuelve representación (evita 204 sin cuerpo)
    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("explotacion_members")
    Call<List<ExplotacionMember>> patchPorId(
            @Query("id") String eqId,      // pásale "eq."+id
            @Body Map<String, Object> cambios
    );

    // Join con usuarios (usa el !<fk> correcto en el select)
    @GET("explotacion_members")
    Call<List<MemberRow>> listarJoinUsuarios(
            @Query("id_explotacion") String eqIdExplotacion,    // "eq.<uuid>"
            @Query("estado_invitacion") String estadoFilter,     // p.ej. "neq.revoked"
            @Query("select") String select,                      // "...,usuario:usuarios!fk_members_usuario(id,email,nombre)"
            @Query("order") String order
    );

    // (Opcional) DELETE físico si prefieres eliminar en lugar de 'revoked'
    @Headers({
            "Prefer: return=minimal"
    })
    @DELETE("explotacion_members")
    Call<Void> deleteById(@Query("id") String eqId); // "eq."+id
}
