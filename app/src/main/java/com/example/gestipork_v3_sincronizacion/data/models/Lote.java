// Lote.java
package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class Lote extends BaseEntity {

    @SerializedName("id_explotacion")
    private String id_explotacion;

    @SerializedName("nDisponibles")
    private Integer nDisponibles;

    @SerializedName("nIniciales")
    private Integer nIniciales;

    @SerializedName("nombre_lote")
    private String nombre_lote;

    @SerializedName("raza")
    private String raza;

    @SerializedName("estado")
    private Integer estado; // 0/1 u otros estados codificados

    @SerializedName("color")
    private String color;

    public Lote() {}

    // Getters & Setters
    public String getId_explotacion() { return id_explotacion; }
    public void setId_explotacion(String id_explotacion) { this.id_explotacion = id_explotacion; }

    public Integer getnDisponibles() { return nDisponibles; }
    public void setnDisponibles(Integer nDisponibles) { this.nDisponibles = nDisponibles; }

    public Integer getnIniciales() { return nIniciales; }
    public void setnIniciales(Integer nIniciales) { this.nIniciales = nIniciales; }

    public String getNombre_lote() { return nombre_lote; }
    public void setNombre_lote(String nombre_lote) { this.nombre_lote = nombre_lote; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public Integer getEstado() { return estado; }
    public void setEstado(Integer estado) { this.estado = estado; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

