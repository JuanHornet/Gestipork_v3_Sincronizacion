// Conteo.java
package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class Conteo extends BaseEntity {

    @SerializedName("id_lote") private String id_lote;
    @SerializedName("id_explotacion") private String id_explotacion;
    @SerializedName("fecha") private String fecha;        // ISO
    @SerializedName("nAnimales") private Integer nAnimales;
    @SerializedName("observaciones") private String observaciones;

    public String getId_lote() { return id_lote; }
    public void setId_lote(String id_lote) { this.id_lote = id_lote; }
    public String getId_explotacion() { return id_explotacion; }
    public void setId_explotacion(String id_explotacion) { this.id_explotacion = id_explotacion; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public Integer getnAnimales() { return nAnimales; }
    public void setnAnimales(Integer nAnimales) { this.nAnimales = nAnimales; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
