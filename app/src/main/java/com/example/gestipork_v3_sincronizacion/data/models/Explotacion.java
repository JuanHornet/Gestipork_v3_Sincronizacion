package com.example.gestipork_v3_sincronizacion.data.models;

public class Explotacion extends BaseEntity {
    private String id_usuario; // UUID usuario
    private String nombre;     // visible al usuario

    public String getId_usuario() { return id_usuario; }
    public void setId_usuario(String id_usuario) { this.id_usuario = id_usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
