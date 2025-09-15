// data/models/MemberRow.java
package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class MemberRow {
    @SerializedName("id")                 private String id;
    @SerializedName("id_explotacion")     private String idExplotacion;
    @SerializedName("id_usuario")         private String idUsuario;
    @SerializedName("rol")                private String rol;               // owner|manager|employee|viewer
    @SerializedName("estado_invitacion")  private String estadoInvitacion;  // pending|accepted|revoked
    @SerializedName("fecha_actualizacion")private String fechaActualizacion;

    @SerializedName("usuario")            private UsuarioMin usuario;       // join usuarios(...)

    public static class UsuarioMin {
        @SerializedName("id")     public String id;
        @SerializedName("email")  public String email;
        @SerializedName("nombre") public String nombre;
    }

    // getters/setters
    public String getId() { return id; }
    public String getIdExplotacion() { return idExplotacion; }
    public String getIdUsuario() { return idUsuario; }
    public String getRol() { return rol; }
    public String getEstadoInvitacion() { return estadoInvitacion; }
    public String getFechaActualizacion() { return fechaActualizacion; }
    public UsuarioMin getUsuario() { return usuario; }
    public void setRol(String r){ this.rol = r; }
    public void setEstadoInvitacion(String e){ this.estadoInvitacion = e; }
    public void setFechaActualizacion(String f){ this.fechaActualizacion = f; }
}

