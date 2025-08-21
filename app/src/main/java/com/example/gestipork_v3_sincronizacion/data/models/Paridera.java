// Paridera.java
package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class Paridera extends BaseEntity {

    @SerializedName("id_lote")
    private String id_lote;

    @SerializedName("id_explotacion")
    private String id_explotacion;

    @SerializedName("nombre_lote")
    private String nombre_lote;   // nombre visual, sustituye a cod_paridera

    @SerializedName("nMadres")
    private Integer nMadres;

    @SerializedName("nPadres")
    private Integer nPadres;

    @SerializedName("fechaInicio")
    private String fechaInicio;

    @SerializedName("fechaFin")
    private String fechaFin;

    public Paridera() {}

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

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
}
