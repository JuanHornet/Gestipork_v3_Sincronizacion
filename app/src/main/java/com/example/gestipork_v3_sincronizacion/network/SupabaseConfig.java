package com.example.gestipork_v3_sincronizacion.network;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class SupabaseConfig {

    // URL base de Supabase REST
    public static final String BASE_URL = "https://muwcidvjzvmjctqlvlhv.supabase.co/rest/v1/";

    // API key (movida a una constante privada)
    public static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im11d2NpZHZqenZtamN0cWx2bGh2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDE1NDg5OTgsImV4cCI6MjA1NzEyNDk5OH0.EuJyw_sdvZ0AOyFvN9GJpLN2sVimE6bq8Ou3q41JkNE"; // Sustituye por la real en producción

    // Token de autenticación Bearer
    public static final String AUTH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im11d2NpZHZqenZtamN0cWx2bGh2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDE1NDg5OTgsImV4cCI6MjA1NzEyNDk5OH0.EuJyw_sdvZ0AOyFvN9GJpLN2sVimE6bq8Ou3q41JkNE"; // Sustituye por el token válido

    public static final String AUTH_BEARER = "Bearer " + API_KEY;

    // Content-Type para todas las llamadas JSON
    public static final String CONTENT_TYPE = "application/json";

    // Devuelve el header Authorization con el formato correcto
    public static String getAuthHeader() {
        return "Bearer " + AUTH_TOKEN;
    }

    public static boolean hayConexionInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }

        return false;
    }

}

