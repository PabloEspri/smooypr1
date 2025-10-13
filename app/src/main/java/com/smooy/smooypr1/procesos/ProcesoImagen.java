package com.smooy.smooypr1.procesos;

public class ProcesoImagen {
    private int id;
    private int proceso_id;
    private int usuario_id;
    private String ruta_imagen;
    private String nombre_imagen;
    private String fecha_subida;

    private String nombre_usuario;

    public ProcesoImagen() { }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getProceso_id() {
        return proceso_id;
    }
    
    public void setProceso_id(int proceso_id) {
        this.proceso_id = proceso_id;
    }
    
    public int getUsuario_id() {
        return usuario_id;
    }
    
    public void setUsuario_id(int usuario_id) {
        this.usuario_id = usuario_id;
    }
    
    public String getRuta_imagen() {
        return ruta_imagen;
    }
    
    public void setRuta_imagen(String ruta_imagen) {
        this.ruta_imagen = ruta_imagen;
    }
    public String getRutaImagen() {
        return ruta_imagen;
    }

    public String getNombreImagen() {
        return nombre_imagen;
    }
    
    public String getNombre_imagen() {
        return nombre_imagen;
    }
    
    public void setNombre_imagen(String nombre_imagen) {
        this.nombre_imagen = nombre_imagen;
    }
    
    public String getFecha_subida() {
        return fecha_subida;
    }
    
    public void setFecha_subida(String fecha_subida) {
        this.fecha_subida = fecha_subida;
    }

    public String getFechaSubida() {
        return fecha_subida;
    }

    public String getNombre_usuario() {
        return nombre_usuario;
    }
    
    public void setNombre_usuario(String nombre_usuario) {
        this.nombre_usuario = nombre_usuario;
    }
    
    public String getNombreUsuario() {
        return nombre_usuario;
    }
    
    @Override
    public String toString() {
        return "ProcesoImagen{" +
                "id=" + id +
                ", proceso_id=" + proceso_id +
                ", usuario_id=" + usuario_id +
                ", ruta_imagen='" + ruta_imagen + '\'' +
                ", nombre_imagen='" + nombre_imagen + '\'' +
                ", fecha_subida='" + fecha_subida + '\'' +
                ", nombre_usuario='" + nombre_usuario + '\'' +
                '}';
    }
}