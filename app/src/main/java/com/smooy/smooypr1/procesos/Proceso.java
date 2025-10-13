package com.smooy.smooypr1.procesos;

import com.google.gson.annotations.SerializedName;

public class Proceso {
    private int id;
    
    @SerializedName("tipo_proceso")
    private String tipo_proceso;
    
    private String descripcion;
    
    private String frecuencia;
    
    private String horario;
    
    @SerializedName("fecha_inicio")
    private String fecha_inicio;
    
    @SerializedName("fecha_fin")
    private String fecha_fin;
    
    private String estado;
    
    @SerializedName("establecimiento_id")
    private int establecimiento_id;
    
    // Constructor
    public Proceso(String tipo_proceso, String descripcion, String frecuencia, 
                   String horario, String fecha_inicio, String fecha_fin, 
                   String estado, int establecimiento_id) {
        this.tipo_proceso = tipo_proceso;
        this.descripcion = descripcion;
        this.frecuencia = frecuencia;
        this.horario = horario;
        this.fecha_inicio = fecha_inicio;
        this.fecha_fin = fecha_fin;
        this.estado = estado;
        this.establecimiento_id = establecimiento_id;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipo_proceso() { return tipo_proceso; }
    public void setTipo_proceso(String tipo_proceso) { this.tipo_proceso = tipo_proceso; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }

    public String getFecha_inicio() { return fecha_inicio; }
    public void setFecha_inicio(String fecha_inicio) { this.fecha_inicio = fecha_inicio; }

    public String getFecha_fin() { return fecha_fin; }
    public void setFecha_fin(String fecha_fin) { this.fecha_fin = fecha_fin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getEstablecimiento_id() { return establecimiento_id; }
    public void setEstablecimiento_id(int establecimiento_id) { this.establecimiento_id = establecimiento_id; }

}