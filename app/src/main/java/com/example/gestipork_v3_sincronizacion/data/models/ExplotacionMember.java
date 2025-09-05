package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class ExplotacionMember {
    @SerializedName("id")                private String id;
    @SerializedName("id_explotacion")    private String idExplotacion;
    @SerializedName("id_usuario")        private String idUsuario;
    @SerializedName("rol")               private String rol;               // owner|manager|employee|viewer
    @SerializedName("estado_invitacion") private String estadoInvitacion;  // pending|accepted|revoked
    @SerializedName("fecha_actualizacion") private String fechaActualizacion; // ISO 8601 -> timestamptz en PG

    // SOLO local: no debe viajar al backend
    private transient Integer sincronizado;

    // Getters/Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdExplotacion() { return idExplotacion; }
    public void setIdExplotacion(String idExplotacion) { this.idExplotacion = idExplotacion; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstadoInvitacion() { return estadoInvitacion; }
    public void setEstadoInvitacion(String estadoInvitacion) { this.estadoInvitacion = estadoInvitacion; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public Integer getSincronizado() { return sincronizado; }
    public void setSincronizado(Integer sincronizado) { this.sincronizado = sincronizado; }
}
