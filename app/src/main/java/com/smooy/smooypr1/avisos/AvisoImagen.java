package com.smooy.smooypr1.avisos;

import com.google.gson.annotations.SerializedName;

public class AvisoImagen {
    private int id;
    
    @SerializedName("aviso_id")
    private int avisoId;
    
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

    // Constructor vacío para Gson
    public AvisoImagen() { }
    
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
        return "AvisoImagen{" +
                "id=" + id +
                ", avisoId=" + avisoId +
                ", usuarioId=" + usuarioId +
                ", rutaImagen='" + rutaImagen + '\'' +
                ", nombreImagen='" + nombreImagen + '\'' +
                ", fechaSubida='" + fechaSubida + '\'' +
                ", nombreUsuario='" + nombreUsuario + '\'' +
                '}';
    }
}