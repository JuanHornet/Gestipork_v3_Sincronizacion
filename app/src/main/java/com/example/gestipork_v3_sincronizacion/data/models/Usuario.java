package com.example.gestipork_v3_sincronizacion.data.models;

public class Usuario extends BaseEntity {
    private String email;
    private String nombre;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}

