package com.example.gestipork_v3_sincronizacion.network.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface GenericSupabaseService {
    @GET("{table}")
    Call<JsonArray> fetch(@Path("table") String table, @QueryMap Map<String, String> query);

    @Headers("Prefer: return=representation")
    @POST("{table}")
    Call<JsonArray> insert(@Path("table") String table, @Body JsonObject body);

    @Headers("Prefer: return=representation")
    @PATCH("{table}")
    Call<JsonArray> update(@Path("table") String table,
                           @QueryMap Map<String, String> match,
                           @Body JsonObject body);

    @DELETE("{table}")
    Call<ResponseBody> delete(@Path("table") String table, @QueryMap Map<String, String> match);
}

