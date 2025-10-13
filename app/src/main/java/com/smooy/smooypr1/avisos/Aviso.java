package com.smooy.smooypr1.avisos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;  // Añadir esta importación

public class Aviso implements Serializable {  // Añadir "implements Serializable"
    private static final long serialVersionUID = 1L;  // Recomendado para serialización

    private int id;
    private String nombre;
    private String descripcion;
    private String categoria;

    @SerializedName("estado")
    private String estado;

    @SerializedName("establecimientoId")
    private int establecimientoId;

    @SerializedName("nombreEstablecimiento")
    private String establecimientoNombre;

    @SerializedName("usuarioId")
    private int usuarioId;

    @SerializedName("nombreUsuario")
    private String usuarioNombre;

    @SerializedName("fechaCreacion")
    private String fechaCreacion;
    
    @SerializedName("procesoId")
    private int procesoId;


    public Aviso() {
    }

    public Aviso(String nombre, String categoria, String descripcion,
                 int establecimientoId, int usuarioId) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.establecimientoId = establecimientoId;
        this.usuarioId = usuarioId;
        this.procesoId = -1; // Default value
    }
    
    public Aviso(String nombre, String categoria, String descripcion,
                int establecimientoId, int usuarioId, int procesoId) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.establecimientoId = establecimientoId;
        this.usuarioId = usuarioId;
        this.procesoId = procesoId;
    }

    public Aviso(String nombre, String categoria, String descripcion,
                 int establecimientoId, int usuarioId, String fechaCreacion,
                 String establecimientoNombre, String usuarioNombre) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.establecimientoId = establecimientoId;
        this.usuarioId = usuarioId;
        this.fechaCreacion = fechaCreacion;
        this.establecimientoNombre = establecimientoNombre;
        this.usuarioNombre = usuarioNombre;
        this.estado="Pendiente";
        this.procesoId = -1; // Default value
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public int getEstablecimientoId() {
        return establecimientoId;
    }

    public void setEstablecimientoId(int establecimientoId) {
        this.establecimientoId = establecimientoId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEstablecimientoNombre() {
        return establecimientoNombre;
    }

    public void setEstablecimientoNombre(String establecimientoNombre) {
        this.establecimientoNombre = establecimientoNombre;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }
    
    public int getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(int procesoId) {
        this.procesoId = procesoId;
    }
}