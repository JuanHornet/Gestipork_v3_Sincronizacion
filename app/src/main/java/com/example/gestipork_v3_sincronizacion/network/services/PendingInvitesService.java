package com.example.gestipork_v3_sincronizacion.network.services;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface PendingInvitesService {
    @Headers({"Prefer: resolution=merge-duplicates"})
    @POST("pending_invites")
    Call<List<Map<String, Object>>> insert(@Body List<Map<String, Object>> invites);
}
