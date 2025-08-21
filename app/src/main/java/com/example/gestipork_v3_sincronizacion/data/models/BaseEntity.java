package com.example.gestipork_v3_sincronizacion.data.models;

public class BaseEntity {
    protected String id;                  // UUID
    protected String fecha_actualizacion; // ISO
    protected int sincronizado;           // 0 local, 1 subido
    protected int eliminado;              // 0/1
    protected String fecha_eliminado;     // ISO o null

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFecha_actualizacion() { return fecha_actualizacion; }
    public void setFecha_actualizacion(String f) { this.fecha_actualizacion = f; }
    public int getSincronizado() { return sincronizado; }
    public void setSincronizado(int s) { this.sincronizado = s; }
    public int getEliminado() { return eliminado; }
    public void setEliminado(int e) { this.eliminado = e; }
    public String getFecha_eliminado() { return fecha_eliminado; }
    public void setFecha_eliminado(String f) { this.fecha_eliminado = f; }
}
