package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.SerializedName;

/**
 * Representa el estado de la alimentación de un lote
 * (Bellota / Cebo Campo / Cebo) en una explotación.
 *
 * Se apoya en BaseEntity para:
 *  - id (UUID)
 *  - fecha_actualizacion (ISO)
 *  - sincronizado (0/1)
 *  - eliminado (0/1)
 *  - fecha_eliminado (ISO)
 */
public class Alimentacion extends BaseEntity {

    @SerializedName("id_lote")
    private String id_lote;

    @SerializedName("id_explotacion")
    private String id_explotacion;

    /**
     * Tipo de alimentación:
     *  - "Bellota"
     *  - "Cebo Campo"
     *  - "Cebo"
     */
    @SerializedName("tipo_alimentacion")
    private String tipoAlimentacion;

    /**
     * Número de animales en este tipo de alimentación.
     */
    @SerializedName("n_animales")
    private Integer nAnimales;

    /**
     * Fecha de inicio de esta fase de alimentación (ISO, ej. 2025-11-19).
     */
    @SerializedName("fecha_inicio")
    private String fechaInicio;

    public Alimentacion() {
        super();
        // Por defecto, al crearla en local todavía no está sincronizada ni eliminada
        setSincronizado(0);
        setEliminado(0);
    }

    // ==== Getters / Setters ====

    public String getId_lote() {
        return id_lote;
    }

    public void setId_lote(String id_lote) {
        this.id_lote = id_lote;
    }

    public String getId_explotacion() {
        return id_explotacion;
    }

    public void setId_explotacion(String id_explotacion) {
        this.id_explotacion = id_explotacion;
    }

    public String getTipoAlimentacion() {
        return tipoAlimentacion;
    }

    public void setTipoAlimentacion(String tipoAlimentacion) {
        this.tipoAlimentacion = tipoAlimentacion;
    }

    public Integer getNAnimales() {
        return nAnimales;
    }

    public void setNAnimales(Integer nAnimales) {
        this.nAnimales = nAnimales;
    }

    // Compatibilidad con posibles usos getnAnimales/setnAnimales
    public Integer getnAnimales() {
        return getNAnimales();
    }

    public void setnAnimales(Integer nAnimales) {
        setNAnimales(nAnimales);
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
}
