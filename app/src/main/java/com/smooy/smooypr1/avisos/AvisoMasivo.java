package com.smooy.smooypr1.avisos;

import java.util.List;

public class AvisoMasivo {
    private String nombre;
    private String categoria;
    private String descripcion;
    private int usuarioId;
    private Integer procesoId; // Opcional, para avisos desde tareas
    private List<Integer> establecimientos; // NUEVO CAMPO REQUERIDO

    // Constructor básico - AHORA REQUIERE establecimientos
    public AvisoMasivo(String nombre, String categoria, String descripcion, int usuarioId, List<Integer> establecimientos) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.usuarioId = usuarioId;
        this.establecimientos = establecimientos;
    }

    // Constructor con proceso ID
    public AvisoMasivo(String nombre, String categoria, String descripcion, int usuarioId, List<Integer> establecimientos, int procesoId) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.usuarioId = usuarioId;
        this.establecimientos = establecimientos;
        this.procesoId = procesoId;
    }

    // Getters y Setters
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

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Integer getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(Integer procesoId) {
        this.procesoId = procesoId;
    }

    // NUEVO getter y setter para establecimientos
    public List<Integer> getEstablecimientos() {
        return establecimientos;
    }

    public void setEstablecimientos(List<Integer> establecimientos) {
        this.establecimientos = establecimientos;
    }
}