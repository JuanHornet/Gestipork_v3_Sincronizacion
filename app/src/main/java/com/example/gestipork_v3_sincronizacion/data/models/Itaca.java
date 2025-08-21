// Itaca.java
package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class Itaca extends BaseEntity {

    @SerializedName("id_lote")
    private String id_lote;

    @SerializedName("id_explotacion")
    private String id_explotacion;

    @SerializedName("nombre_lote")
    private String nombre_lote;   // nombre visual, sustituye a cod_itaca

    @SerializedName("nAnimales")
    private Integer nAnimales;

    @SerializedName("nMadres")
    private Integer nMadres;

    @SerializedName("nPadres")
    private Integer nPadres;

    @SerializedName("fechaPrimerNacimiento")
    private String fechaPrimerNacimiento;

    @SerializedName("fechaUltimoNacimiento")
    private String fechaUltimoNacimiento;

    @SerializedName("raza")
    private String raza;

    @SerializedName("color")
    private String color;

    @SerializedName("crotalesSolicitados")
    private Integer crotalesSolicitados;

    @SerializedName("dcer")       // 🔹 nuevo campo añadido
    private String dcer;

    public Itaca() {}

    public String getId_lote() { return id_lote; }
    public void setId_lote(String id_lote) { this.id_lote = id_lote; }

    public String getDcer() {
        return dcer;
    }

    public void setDcer(String dcer) {
        this.dcer = dcer;
    }

    public String getId_explotacion() { return id_explotacion; }
    public void setId_explotacion(String id_explotacion) { this.id_explotacion = id_explotacion; }

    public String getNombre_lote() { return nombre_lote; }
    public void setNombre_lote(String nombre_lote) { this.nombre_lote = nombre_lote; }

    public Integer getnAnimales() { return nAnimales; }
    public void setnAnimales(Integer nAnimales) { this.nAnimales = nAnimales; }

    public Integer getnMadres() { return nMadres; }
    public void setnMadres(Integer nMadres) { this.nMadres = nMadres; }

    public Integer getnPadres() { return nPadres; }
    public void setnPadres(Integer nPadres) { this.nPadres = nPadres; }

    public String getFechaPrimerNacimiento() { return fechaPrimerNacimiento; }
    public void setFechaPrimerNacimiento(String fechaPrimerNacimiento) { this.fechaPrimerNacimiento = fechaPrimerNacimiento; }

    public String getFechaUltimoNacimiento() { return fechaUltimoNacimiento; }
    public void setFechaUltimoNacimiento(String fechaUltimoNacimiento) { this.fechaUltimoNacimiento = fechaUltimoNacimiento; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getCrotalesSolicitados() { return crotalesSolicitados; }
    public void setCrotalesSolicitados(Integer crotalesSolicitados) { this.crotalesSolicitados = crotalesSolicitados; }
}
