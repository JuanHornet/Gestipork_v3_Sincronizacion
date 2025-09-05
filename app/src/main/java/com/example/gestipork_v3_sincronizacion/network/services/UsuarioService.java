package com.example.gestipork_v3_sincronizacion.network.services;

import com.google.gson.JsonObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Endpoints PostgREST para public.usuarios */
public interface UsuarioService {

    // Upsert por id (si existe actualiza; si no, inserta)
    @Headers({
            "Content-Type: application/json",
            "Prefer: resolution=merge-duplicates, return=minimal"
    })
    @POST("usuarios?on_conflict=id")
    Call<Void> upsert(@Body JsonObject body);

    // Leer por id (pásale "eq.<uuid>")
    @GET("usuarios")
    Call<List<JsonObject>> getById(
            @Query("id") String idFilter,     // "eq.<uuid>"
            @Query("select") String select    // p.ej. "*"
    );

    // NUEVO: buscar por email para el flujo de invitaciones
    @GET("usuarios")
    Call<List<JsonObject>> getByEmail(
            @Query("email") String emailFilter, // "eq.<email>"
            @Query("select") String select      // p.ej. "id,email,nombre"
    );
}
