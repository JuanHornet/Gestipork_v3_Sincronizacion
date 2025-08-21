package com.example.gestipork_v3_sincronizacion.data.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SalidasExplotacion extends BaseEntity {

    @SerializedName("id_lote")
    @Expose
    private String id_lote;

    @SerializedName("id_explotacion")
    @Expose
    private String id_explotacion;

    @SerializedName("tipo_salida")
    @Expose
    private String tipoSalida;

    @SerializedName("tipo_alimentacion")
    @Expose
    private String tipoAlimentacion;

    // ISO corto yyyy-MM-dd
    @SerializedName("fecha_salida")
    @Expose
    private String fechaSalida;

    @SerializedName("n_animales")
    @Expose
    private int nAnimales;

    @SerializedName("observacion")
    @Expose
    private String observacion;

    public SalidasExplotacion() {
        super();
        // Al crear en local: no sincronizado, no eliminado
        setSincronizado(0);
        setEliminado(0);
    }

    public SalidasExplotacion(String id,
                              String id_lote,
                              String id_explotacion,
                              String tipoSalida,
                              String tipoAlimentacion,
                              String fechaSalida,
                              int nAnimales,
                              String observacion,
                              int sincronizado,
                              String fechaActualizacion,
                              int eliminado,
                              String fechaEliminado) {
        super();
        this.id_lote = id_lote;
        this.id_explotacion = id_explotacion;
        this.tipoSalida = tipoSalida;
        this.tipoAlimentacion = tipoAlimentacion;
        this.fechaSalida = fechaSalida;
        this.nAnimales = nAnimales;
        this.observacion = observacion;
    }

    /**
     * Helper para crear desde Date → convierte a yyyy-MM-dd.
     */
    public static SalidasExplotacion fromDate(String id,
                                              int nAnimales,
                                              String tipoSalida,
                                              String tipoAlimentacion,
                                              String id_lote,
                                              String id_explotacion,
                                              String observacion,
                                              Date fechaDate) {
        SalidasExplotacion s = new SalidasExplotacion();
        s.setId(id);
        s.setNAnimales(nAnimales);
        s.setTipoSalida(tipoSalida);
        s.setTipoAlimentacion(tipoAlimentacion);
        s.setId_lote(id_lote);
        s.setId_explotacion(id_explotacion);
        s.setObservacion(observacion);

        if (fechaDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            s.setFechaSalida(sdf.format(fechaDate));
        }

        s.setSincronizado(0);
        s.setEliminado(0);
        return s;
    }

    // ===== Getters / Setters =====
    public String getId_lote() { return id_lote; }
    public void setId_lote(String id_lote) { this.id_lote = id_lote; }

    public String getId_explotacion() { return id_explotacion; }
    public void setId_explotacion(String id_explotacion) { this.id_explotacion = id_explotacion; }

    public String getTipoSalida() { return tipoSalida; }
    public void setTipoSalida(String tipoSalida) { this.tipoSalida = tipoSalida; }

    public String getTipoAlimentacion() { return tipoAlimentacion; }
    public void setTipoAlimentacion(String tipoAlimentacion) { this.tipoAlimentacion = tipoAlimentacion; }

    public String getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(String fechaSalida) { this.fechaSalida = fechaSalida; }

    public int getNAnimales() { return nAnimales; }
    public void setNAnimales(int nAnimales) { this.nAnimales = nAnimales; }

    // Compatibilidad con código que use getnAnimales/setnAnimales
    public int getnAnimales() { return getNAnimales(); }
    public void setnAnimales(int nAnimales) { setNAnimales(nAnimales); }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
