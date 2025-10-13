package com.smooy.smooypr1.avisos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class AvisoComentario implements Serializable {
    @SerializedName("id")
    private int id;
    
    @SerializedName("aviso_id")
    private int avisoId;
    
    @SerializedName("usuario_id")
    private int usuarioId;
    
    @SerializedName("comentario")
    private String comentario;
    
    @SerializedName("fecha_creacion")
    private String fechaCreacion;
    
    @SerializedName("nombre_usuario")
    private String nombreUsuario;

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAvisoId() {
        return avisoId;
    }

    public void setAvisoId(int avisoId) {
        this.avisoId = avisoId;
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