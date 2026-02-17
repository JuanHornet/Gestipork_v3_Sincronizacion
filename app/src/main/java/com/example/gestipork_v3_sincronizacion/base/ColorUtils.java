package com.example.gestipork_v3_sincronizacion.base;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.example.gestipork_v3_sincronizacion.R;

public class ColorUtils {

    public static int mapColorNameToHex(Context context, String colorName) {
        if (colorName == null || colorName.trim().isEmpty() || colorName.equalsIgnoreCase("Seleccione color de crotal")) {
            return ContextCompat.getColor(context, R.color.gray);    // ✅ color por defecto
        }

        switch (colorName.toLowerCase()) {
            case "azul": return Color.parseColor("#2196F3");
            case "naranja": return Color.parseColor("#FF9800");
            case "rojo": return Color.parseColor("#F44336");
            case "verde": return Color.parseColor("#4CAF50");
            case "rosa": return Color.parseColor("#F307B7");
            default: return ContextCompat.getColor(context, R.color.gray);
        }
    }

}
