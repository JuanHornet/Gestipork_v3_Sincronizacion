// auth/SessionManager.java
package com.example.gestipork_v3_sincronizacion.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "auth_session";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USER_ID = "user_id"; // auth.user id (UUID)
    private static final String KEY_EXPIRES_AT = "expires_at"; // epoch seconds

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        this.sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(String access, String refresh, String userId, long expiresAt) {
        sp.edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .putString(KEY_USER_ID, userId)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .apply();
    }

    public String getAccessToken()  { return sp.getString(KEY_ACCESS,  null); }
    public String getRefreshToken() { return sp.getString(KEY_REFRESH, null); }
    public String getUserId()       { return sp.getString(KEY_USER_ID, null); }
    public long   getExpiresAt()    { return sp.getLong(KEY_EXPIRES_AT, 0L); }

    public boolean isLoggedIn() { return getAccessToken() != null && getRefreshToken() != null; }

    public void clear() {
        sp.edit().clear().apply();
    }
}
