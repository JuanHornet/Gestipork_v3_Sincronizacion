package com.example.gestipork_v3_sincronizacion.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class SessionManager {
    private static final String PREFS          = "auth_session";
    private static final String KEY_ACCESS     = "access_token";
    private static final String KEY_REFRESH    = "refresh_token";
    private static final String KEY_USER_ID    = "user_id";       // UUID de auth.user
    private static final String KEY_EXPIRES_AT = "expires_at";    // epoch seconds
    private static final String KEY_EMAIL      = "email";         // opcional

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        this.sp = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Guarda los tokens principales. */
    public void save(String access, String refresh, String userId, long expiresAt) {
        sp.edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .putString(KEY_USER_ID, userId)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .apply();
    }

    /** (Opcional) Guarda/lee email si lo manejas en login. */
    public void setEmail(String email) { sp.edit().putString(KEY_EMAIL, email).apply(); }
    public String getEmail()           { return sp.getString(KEY_EMAIL, null); }

    public String getAccessToken()  { return sp.getString(KEY_ACCESS,  null); }
    public String getRefreshToken() { return sp.getString(KEY_REFRESH, null); }
    public String getUserId()       { return sp.getString(KEY_USER_ID, null); }
    public long   getExpiresAt()    { return sp.getLong(KEY_EXPIRES_AT, 0L); }

    /** Sencillo chequeo de sesión (tokens presentes y no expirado si hay fecha). */
    public boolean isLoggedIn() {
        String at = getAccessToken();
        String rt = getRefreshToken();
        if (TextUtils.isEmpty(at) || TextUtils.isEmpty(rt)) return false;
        long exp = getExpiresAt();
        return exp == 0L || (System.currentTimeMillis() / 1000L) < exp;
    }

    /** Limpia TODO lo guardado en la sesión actual. */
    public void clear() {
        sp.edit().clear().apply();
    }
}
