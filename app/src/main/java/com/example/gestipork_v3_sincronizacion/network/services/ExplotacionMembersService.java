package com.example.gestipork_v3_sincronizacion.network.services;

import com.example.gestipork_v3_sincronizacion.data.models.ExplotacionMember;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ExplotacionMembersService {

    // Ejemplo: listarPorUsuario("eq."+userId, "*")
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

    @Headers({"Prefer: resolution=merge-duplicates, return=representation"})
    @POST("explotacion_members")
    Call<List<ExplotacionMember>> upsert(@Body List<ExplotacionMember> miembros);

    // Patch por id (PostgREST: id=eq.{id})
    @PATCH("explotacion_members")
    Call<List<ExplotacionMember>> patchPorId(
            @Query("id") String eqId,      // pásale "eq."+id
            @Body Map<String, Object> cambios
    );
}
