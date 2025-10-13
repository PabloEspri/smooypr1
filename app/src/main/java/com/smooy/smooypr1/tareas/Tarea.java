package com.smooy.smooypr1.tareas;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Tarea implements Serializable {
    private int id;
    
    @SerializedName("proceso_id")
    private int procesoId;
    
    private String nombre;
    private String descripcion;
    private int orden;
    private String estado;
    
    @SerializedName("fecha_creacion")
    private String fechaCreacion;
    
    @SerializedName("fecha_completado")
    private String fechaCompletado;
    
    @SerializedName("usuario_completado_id")
    private Integer usuarioCompletadoId;
    
    @SerializedName("nombre_usuario_completado")
    private String nombreUsuarioCompletado;
    
    @SerializedName("usuario_completado")
    private String usuarioCompletado;

    public Tarea() {}

    public Tarea(int procesoId, String nombre, String descripcion, int orden, String estado) {
        this.procesoId = procesoId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.orden = orden;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getProcesoId() {
        return procesoId;
    }
    
    public void setProcesoId(int procesoId) {
        this.procesoId = procesoId;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public int getOrden() {
        return orden;
    }
    
    public void setOrden(int orden) {
        this.orden = orden;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public String getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public String getFechaCompletado() {
        return fechaCompletado;
    }
    
    public void setFechaCompletado(String fechaCompletado) {
        this.fechaCompletado = fechaCompletado;
    }
    
    public Integer getUsuarioCompletadoId() {
        return usuarioCompletadoId;
    }
    
    public void setUsuarioCompletadoId(Integer usuarioCompletadoId) {
        this.usuarioCompletadoId = usuarioCompletadoId;
    }
    
    public String getNombreUsuarioCompletado() {
        return nombreUsuarioCompletado;
    }
    
    public void setNombreUsuarioCompletado(String nombreUsuarioCompletado) {
        this.nombreUsuarioCompletado = nombreUsuarioCompletado;
    }
    
    public String getUsuarioCompletado() {
        return usuarioCompletado;
    }
    
    public void setUsuarioCompletado(String usuarioCompletado) {
        this.usuarioCompletado = usuarioCompletado;
    }
}