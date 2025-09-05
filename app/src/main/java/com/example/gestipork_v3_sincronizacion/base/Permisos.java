package com.example.gestipork_v3_sincronizacion.base;

public final class Permisos {
    private Permisos() {}

    public static boolean puede(String accion, String rol) {
        switch (accion) {
            case "GESTION_MIEMBROS":   return "owner".equals(rol) || "manager".equals(rol);
            case "BORRAR_EXPLOTACION": return "owner".equals(rol);
            case "EDITAR_EXPLOTACION": return "owner".equals(rol) || "manager".equals(rol);
            case "CRUD_DATOS":         return !"viewer".equals(rol);
            case "VER_DATOS":          return true;
            default:                   return false;
        }
    }
}
