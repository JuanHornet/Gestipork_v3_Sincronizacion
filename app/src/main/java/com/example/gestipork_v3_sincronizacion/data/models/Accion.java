// Accion.java
package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

public class Accion extends BaseEntity {

    @SerializedName("id_lote")
    private String id_lote;

    @SerializedName("id_explotacion")
    private String id_explotacion;

    @SerializedName("tipo")
    private String tipo;

    // ISO 8601 "yyyy-MM-dd'T'HH:mm:ss"
    @SerializedName("fecha")
    private String fecha;

    @SerializedName("cantidad")
    private Integer cantidad;

    @SerializedName("observaciones")
    private String observaciones;

    public Accion() {}

    public String getId_lote() { return id_lote; }
    public void setId_lote(String id_lote) { this.id_lote = id_lote; }

    public String getId_explotacion() { return id_explotacion; }
    public void setId_explotacion(String id_explotacion) { this.id_explotacion = id_explotacion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
