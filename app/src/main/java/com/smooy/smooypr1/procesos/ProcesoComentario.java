package com.smooy.smooypr1.procesos;

import com.google.gson.annotations.SerializedName;

public class ProcesoComentario {
    private int id;

    @SerializedName("proceso_id")
    private int procesoId;
    
    @SerializedName("usuario_id")
    private int usuarioId;
    
    private String comentario;
    
    @SerializedName("fecha_creacion")
    private String fechaCreacion;
    
    @SerializedName("nombre_usuario")
    private String nombreUsuario;

    // Constructor vacío para Retrofit
    public ProcesoComentario() {
    }
    public ProcesoComentario(int procesoId, int usuarioId, String comentario) {
        this.procesoId = procesoId;
        this.usuarioId = usuarioId;
        this.comentario = comentario;
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
    
    public int getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public String getComentario() {
        return comentario;
    }
    
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
    
    public String getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombre_usuario() {
        return nombreUsuario;
    }

    public void setNombre_usuario(String nombre_usuario) {
        this.nombreUsuario = nombre_usuario;
    }

    public String getFecha_creacion() {
        return fechaCreacion;
    }

    public void setFecha_creacion(String fecha_creacion) {
        this.fechaCreacion = fecha_creacion;
    }

    public void setIdProceso(int procesoId) {
        this.procesoId = procesoId;
    }
    
    public void setIdUsuario(int usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public void setContenido(String comentario) {
        this.comentario = comentario;
    }
}