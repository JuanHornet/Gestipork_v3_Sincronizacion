package com.example.gestipork_v3_sincronizacion.network.services;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/** Endpoints PostgREST para public.explotaciones */
public interface ExplotacionesService {

    // Upsert por id (si existe actualiza; si no, inserta)
    @Headers({
            "Content-Type: application/json",
            "Prefer: resolution=merge-duplicates, return=minimal"
    })
    @POST("explotaciones?on_conflict=id")
    Call<Void> upsert(@Body List<Map<String, Object>> filas);
}
