package com.smooy.smooypr1.tareas;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class TareaComentario implements Serializable {
    private int id;
    
    @SerializedName("tarea_id")
    private int tareaId;
    
    @SerializedName("usuario_id")
    private int usuarioId;
    
    private String comentario;
    
    @SerializedName("fecha_creacion")
    private String fechaCreacion;
    
    @SerializedName("nombre_usuario")
    private String nombreUsuario;

    public TareaComentario() {
    }

    public TareaComentario(int tareaId, int usuarioId, String comentario) {
        this.tareaId = tareaId;
        this.usuarioId = usuarioId;
        this.comentario = comentario;
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getTareaId() {
        return tareaId;
    }
    
    public void setTareaId(int tareaId) {
        this.tareaId = tareaId;
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
}