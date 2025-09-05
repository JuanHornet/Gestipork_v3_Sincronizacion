package com.example.gestipork_v3_sincronizacion.network.services;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthService {

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token")
    Call<JsonObject> passwordGrant(
            @Query("grant_type") String grantType, // "password"
            @Body JsonObject body                  // { "email": "...", "password": "..." }
    );

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token")
    Call<JsonObject> refreshToken(
            @Query("grant_type") String grantType, // "refresh_token"
            @Body JsonObject body                  // { "refresh_token": "..." }
    );

    // Registro: POST /auth/v1/signup
    @Headers("Content-Type: application/json")
    @POST("auth/v1/signup")
    Call<JsonObject> signUp(@Body JsonObject body); // { "email":"...", "password":"...", "data": {...} }

    // Recuperación: POST /auth/v1/recover?redirect_to=...
    @Headers("Content-Type: application/json")
    @POST("auth/v1/recover")
    Call<Void> recover(
            @Query("redirect_to") String redirectTo, // puede ser null
            @Body JsonObject body                    // { "email": "..." }
    );
    @POST("auth/v1/resend")
    Call<Void> resend(@Body JsonObject body);

}
