package com.example.gestipork_v3_sincronizacion;

import android.app.Application;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.network.ApiClient;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        ApiClient.setAppContext(this);
        DBHelper.ensureCreated(this); // fuerza onCreate() del helper si no existe
    }
}

