package com.smooy.smooypr1.tareas;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class TareaImagen implements Serializable {
    private int id;
    
    @SerializedName("tarea_id")
    private int tareaId;
    
    @SerializedName("usuario_id")
    private int usuarioId;
    
    @SerializedName("ruta_imagen")
    private String rutaImagen;
    
    @SerializedName("nombre_imagen")
    private String nombreImagen;
    
    @SerializedName("fecha_subida")
    private String fechaSubida;
    
    @SerializedName("nombre_usuario")
    private String nombreUsuario;

    public TareaImagen() { }
    
    // Getters y setters
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

    public String getRutaImagen() {
        return rutaImagen;
    }
    
    public void setRutaImagen(String rutaImagen) {
        this.rutaImagen = rutaImagen;
    }

    public String getNombreImagen() {
        return nombreImagen;
    }
    
    public void setNombreImagen(String nombreImagen) {
        this.nombreImagen = nombreImagen;
    }
    
    public String getFechaSubida() {
        return fechaSubida;
    }
    
    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    @Override
    public String toString() {
        return "TareaImagen{" +
                "id=" + id +
                ", tareaId=" + tareaId +
                ", usuarioId=" + usuarioId +
                ", rutaImagen='" + rutaImagen + '\'' +
                ", nombreImagen='" + nombreImagen + '\'' +
                ", fechaSubida='" + fechaSubida + '\'' +
                ", nombreUsuario='" + nombreUsuario + '\'' +
                '}';
    }
}