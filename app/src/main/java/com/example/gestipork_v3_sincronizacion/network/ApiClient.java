package com.example.gestipork_v3_sincronizacion.network;

import android.content.Context;

import com.example.gestipork_v3_sincronizacion.auth.SessionManager;
import com.example.gestipork_v3_sincronizacion.network.services.AuthService;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient con:
 *  - Authorization dinámico:
 *      * Si hay sesión → "Bearer <access_token>"
 *      * Si no hay sesión → "Bearer <API_KEY>" (anon)
 *  - Auto-refresh en 401 usando Supabase Auth (refresh_token)
 *
 * Uso recomendado:
 *  - Llama a ApiClient.setAppContext(getApplicationContext()) en tu Application.onCreate().
 *  - Usa ApiClient.get() para servicios REST (PostgREST: /rest/v1/...).
 *  - Usa ApiClient.getAuth() para AuthService (Auth: /auth/v1/...).
 */
public class ApiClient {

    private static Retrofit retrofit;       // REST (PostgREST)
    private static Retrofit authRetrofit;   // AUTH (/auth/v1)
    private static OkHttpClient client;
    private static Context appContext;

    // ====== Inicialización opcional del contexto de app (recomendado) ======
    public static void setAppContext(Context context) {
        appContext = context.getApplicationContext();
        // Si cambias de usuario o contexto, puedes forzar rebuild si quieres:
        // retrofit = null; authRetrofit = null; client = null;
    }

    // ====== Retrofit para REST (PostgREST) ======
    public static Retrofit get() {
        if (client == null) client = buildClient();
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.BASE_URL) // Debe apuntar a .../rest/v1/
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // ====== Retrofit para AUTH (/auth/v1) ======
    public static Retrofit getAuth() {
        if (authRetrofit == null) {
            String authBase = resolveAuthBaseUrl(SupabaseConfig.BASE_URL);
            authRetrofit = new Retrofit.Builder()
                    .baseUrl(authBase) // raíz del proyecto (terminará en /)
                    .client(buildAuthClient()) // logging liviano, sin authenticator
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }

    // ====== OkHttp con interceptores y authenticator ======
    private static OkHttpClient buildClient() {
        // Interceptor que añade headers y el Bearer adecuado
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();

            String bearer;
            if (appContext != null) {
                SessionManager sm = new SessionManager(appContext);
                String access = sm.getAccessToken();
                if (access != null && !access.isEmpty()) {
                    bearer = "Bearer " + access;
                } else {
                    bearer = "Bearer " + SupabaseConfig.API_KEY; // anon
                }
            } else {
                // Fallback sin contexto (no recordará sesión, usa anon)
                bearer = "Bearer " + SupabaseConfig.API_KEY;
            }

            Request req = original.newBuilder()
                    .header("apikey", SupabaseConfig.API_KEY)
                    .header("Authorization", bearer)
                    .header("Content-Type", "application/json")
                    // Para PostgREST (schemas). Ajusta si usas otro esquema.
                    .header("Accept-Profile", "public")
                    .header("Content-Profile", "public")
                    .build();

            return chain.proceed(req);
        };

        // Authenticator: si recibimos 401, intentamos refrescar el token
        Authenticator tokenRefresher = new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                // Evitar bucles infinitos
                if (responseCount(response) >= 1) return null;

                if (appContext == null) return null;
                SessionManager sm = new SessionManager(appContext);
                String refresh = sm.getRefreshToken();
                if (refresh == null || refresh.isEmpty()) return null;

                try {
                    AuthService auth = getAuth().create(AuthService.class);

                    JsonObject body = new JsonObject();
                    body.addProperty("refresh_token", refresh);

                    retrofit2.Response<JsonObject> r = auth.refreshToken("refresh_token", body).execute();
                    if (!r.isSuccessful() || r.body() == null) return null;

                    JsonObject b = r.body();
                    String newAccess = b.get("access_token").getAsString();
                    String newRefresh = b.has("refresh_token") && !b.get("refresh_token").isJsonNull()
                            ? b.get("refresh_token").getAsString()
                            : refresh; // algunos flujos no devuelven refresh nuevo
                    long expiresIn = b.get("expires_in").getAsLong();
                    long expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn;
                    String userId = b.has("user") && !b.get("user").isJsonNull()
                            ? b.get("user").getAsJsonObject().get("id").getAsString()
                            : sm.getUserId();

                    // Guardar sesión nueva
                    sm.save(newAccess, newRefresh, userId, expiresAt);

                    // Reintentar request original con nuevo access
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + newAccess)
                            .build();

                } catch (Exception e) {
                    return null;
                }
            }
        };

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .authenticator(tokenRefresher)
                .addInterceptor(logging)
                .build();
    }

    // Cliente simple para Auth (sin authenticator para evitar recursion con /auth/v1/token)
    private static OkHttpClient buildAuthClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY);

        // Para Auth, los headers obligatorios los pone cada request del servicio.
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    // ====== Utilidades ======

    /**
     * A partir de BASE_URL de PostgREST (…/rest/v1/) calcula la base del proyecto (…/).
     * Si ya es raíz, la devuelve tal cual.
     */
    private static String resolveAuthBaseUrl(String baseUrl) {
        if (baseUrl == null) return "";
        String s = baseUrl.trim();
        // Normaliza: queremos acabar en "/"
        if (!s.endsWith("/")) s = s + "/";
        // Si viene como .../rest/v1/ -> recortamos hasta la raíz del proyecto
        int idx = s.indexOf("/rest/v1/");
        if (idx > 0) {
            s = s.substring(0, idx + 1); // incluye la barra final
        }
        return s;
    }

    private static int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
