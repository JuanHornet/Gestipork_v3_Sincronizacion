// Cubricion.java
package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class Cubricion extends BaseEntity {

    @SerializedName("id_lote")
    private String id_lote;

    @SerializedName("id_explotacion")
    private String id_explotacion;

    @SerializedName("nombre_lote")
    private String nombre_lote;   // nombre visual, sustituye a cod_cubricion

    @SerializedName("nmadres")
    private Integer nMadres;

    @SerializedName("npadres")
    private Integer nPadres;

    @SerializedName("fechainiciocubricion")
    private String fechaInicioCubricion;

    @SerializedName("fechafincubricion")
    private String fechaFinCubricion;

    public Cubricion() {}

    public String getId_lote() { return id_lote; }
    public void setId_lote(String id_lote) { this.id_lote = id_lote; }

    public String getId_explotacion() { return id_explotacion; }
    public void setId_explotacion(String id_explotacion) { this.id_explotacion = id_explotacion; }

    public String getNombre_lote() { return nombre_lote; }
    public void setNombre_lote(String nombre_lote) { this.nombre_lote = nombre_lote; }

    public Integer getnMadres() { return nMadres; }
    public void setnMadres(Integer nMadres) { this.nMadres = nMadres; }

    public Integer getnPadres() { return nPadres; }
    public void setnPadres(Integer nPadres) { this.nPadres = nPadres; }

    public String getFechaInicioCubricion() { return fechaInicioCubricion; }
    public void setFechaInicioCubricion(String fechaInicioCubricion) { this.fechaInicioCubricion = fechaInicioCubricion; }

    public String getFechaFinCubricion() { return fechaFinCubricion; }
    public void setFechaFinCubricion(String fechaFinCubricion) { this.fechaFinCubricion = fechaFinCubricion; }
}
