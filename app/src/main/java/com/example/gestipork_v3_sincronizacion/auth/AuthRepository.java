package com.example.gestipork_v3_sincronizacion.auth;

import android.content.Context;

import com.example.gestipork_v3_sincronizacion.data.repo.UsuarioRepository;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;
import com.example.gestipork_v3_sincronizacion.network.services.AuthService;
import com.google.gson.JsonObject;
import com.example.gestipork_v3_sincronizacion.data.local.UsuarioLocalDao;


import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Maneja login/registro/recuperación con Supabase Auth y persistencia de sesión.
 */
public class AuthRepository {

    private final Context appCtx;
    private final AuthService auth;
    private final SessionManager session;

    public AuthRepository(Context context) {
        this.appCtx = context.getApplicationContext();
        this.auth = ApiClient.getAuth().create(AuthService.class);
        this.session = new SessionManager(appCtx);
    }


    /** Login email+password. Guarda tokens si OK y upsertea el perfil en public.usuarios. */
    public boolean signIn(String email, String password) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("email", email);
            body.addProperty("password", password);

            Response<JsonObject> r = auth.passwordGrant("password", body).execute();
            if (!r.isSuccessful() || r.body() == null) return false;

            JsonObject b = r.body();
            String access   = b.get("access_token").getAsString();
            String refresh  = b.get("refresh_token").getAsString();
            long   expiresIn= b.get("expires_in").getAsLong();
            long   expiresAt= (System.currentTimeMillis()/1000L) + expiresIn;

            String userId = null, emailOut = null, nombreOut = null;
            if (b.has("user") && !b.get("user").isJsonNull()) {
                JsonObject u = b.getAsJsonObject("user");
                if (u.has("id") && !u.get("id").isJsonNull())
                    userId = u.get("id").getAsString();
                if (u.has("email") && !u.get("email").isJsonNull())
                    emailOut = u.get("email").getAsString();
                if (u.has("user_metadata") && u.get("user_metadata").isJsonObject()) {
                    JsonObject meta = u.getAsJsonObject("user_metadata");
                    if (meta.has("name") && !meta.get("name").isJsonNull())
                        nombreOut = meta.get("name").getAsString();
                }
            }

            session.save(access, refresh, userId, expiresAt);

            // Upsert del perfil en public.usuarios (no bloquea el login si falla)
            try {
                new UsuarioRepository().upsertSelf(
                        userId != null ? userId : session.getUserId(),
                        emailOut != null ? emailOut : email,
                        nombreOut
                );
            } catch (Exception ignore) {}

            // ... tras session.save(...) y el upsert remoto (UsuarioRepository.upsertSelf)
            try {
                new UsuarioLocalDao(appCtx)
                        .upsert(
                                userId != null ? userId : session.getUserId(),
                                emailOut != null ? emailOut : email,
                                nombreOut,
                                /*fromServer=*/ true   // viene del servidor → sincronizado=1
                        );
            } catch (Exception ignore) {}


            return true;
        } catch (Exception e) {
            return false;
        }

    }

    /** Registro email+password. Devuelve mensaje legible para UI. */
    // NUEVO: signup con nombre
    public Result signUp(String email, String password, String nombre) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("email", email);
            body.addProperty("password", password);

            if (nombre != null && !nombre.trim().isEmpty()) {
                JsonObject meta = new JsonObject();
                meta.addProperty("name", nombre.trim());
                body.add("data", meta);
            }

            // ✅ SIN "options" EN EL BODY
            Response<JsonObject> r = auth.signUp(body).execute();
            if (r.isSuccessful()) {
                return Result.ok("Registro iniciado. Revisa tu email para confirmar la cuenta.");
            } else {
                String err = extractError(r.errorBody());
                return Result.fail(err != null ? err : "No se pudo registrar");
            }
        } catch (Exception e) {
            return Result.fail("Error de red");
        }
    }


    // EXISTENTE: delega a la nueva con nombre = null
    public Result signUp(String email, String password) {
        return signUp(email, password, null);
    }


    /** Envía email de recuperación de contraseña. */
    public Result sendPasswordReset(String email, String redirectTo) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("email", email);

            Response<Void> r = auth.recover(redirectTo, body).execute();
            if (r.isSuccessful()) {
                return Result.ok("Te hemos enviado un email para restablecer la contraseña.");
            } else {
                return Result.fail("No se pudo enviar el email de recuperación");
            }
        } catch (Exception e) {
            return Result.fail("Error de red");
        }
    }

    public void signOut() { session.clear(); }
    public boolean isLoggedIn() { return session.isLoggedIn(); }

    private String extractError(ResponseBody eb) {
        try {
            if (eb == null) return null;
            return eb.string(); // Supabase: {"error":"...", "error_description":"..."} o {"msg":"..."}
        } catch (Exception ignored) { return null; }
    }

    // Resultado simple para UI
    public static class Result {
        public final boolean ok;
        public final String message;
        private Result(boolean ok, String message){ this.ok = ok; this.message = message; }
        public static Result ok(String m){ return new Result(true, m); }
        public static Result fail(String m){ return new Result(false, m); }
    }
    public Result resendConfirmation(String email, String redirectTo) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("type", "signup");     // reenviar confirmación
            body.addProperty("email", email);
            if (redirectTo != null && !redirectTo.isEmpty()) {
                JsonObject options = new JsonObject();
                options.addProperty("emailRedirectTo", redirectTo);
                body.add("options", options);
            }
            Response<Void> r = auth.resend(body).execute();
            if (r.isSuccessful()) {
                return Result.ok("Te hemos reenviado el email de confirmación.");
            } else {
                return Result.fail("No se pudo reenviar el email de confirmación.");
            }
        } catch (Exception e) {
            return Result.fail("Error de red");
        }
    }

}
