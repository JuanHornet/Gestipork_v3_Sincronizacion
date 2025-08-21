package com.example.gestipork_v3_sincronizacion.base;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FechaUtils {
    public static String ahoraIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
