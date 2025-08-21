// network/services/AuthService.java
package com.example.gestipork_v3_sincronizacion.network.services;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthService {

    // Login: /auth/v1/token?grant_type=password
    @Headers({
            "Content-Type: application/json"
    })
    @POST("auth/v1/token")
    Call<JsonObject> passwordGrant(
            @Query("grant_type") String grantType, // "password"
            @Body JsonObject body // { "email": "...", "password": "..." }
    );

    // Refresh: /auth/v1/token?grant_type=refresh_token
    @Headers({
            "Content-Type: application/json"
    })
    @POST("auth/v1/token")
    Call<JsonObject> refreshToken(
            @Query("grant_type") String grantType, // "refresh_token"
            @Body JsonObject body // { "refresh_token": "..." }
    );
}
