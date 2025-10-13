package com.smooy.smooypr1.usuarios;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Usuario {

    @SerializedName("ID")
    private int id;
    
    @SerializedName("Nombre")
    private String nombre;
    
    @SerializedName("apellido")
    private String apellido;
    
    @SerializedName("usuario")
    private String nombreUsuario;
    
    @SerializedName("Rol")
    private String rol;

    @SerializedName("establecimiento_nombre")
    private String establecimientoNombre;
    
    @SerializedName("establecimiento_id")
    private int establecimientoId;

    @SerializedName("establecimientos")
    private List<Map<String, Object>> establecimientos = new ArrayList<>();

    public Usuario() {
    }

    public Usuario(int id, String nombre, String apellido, String nombreUsuario, String rol, 
                  String establecimientoNombre, int establecimientoId) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.establecimientoNombre = establecimientoNombre;
        this.establecimientoId = establecimientoId;
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getApellido() {
        return apellido;
    }
    
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
    
    public String getNombreUsuario() {
        return nombreUsuario;
    }
    
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
    
    public String getRol() {
        return rol;
    }
    
    public void setRol(String rol) {
        this.rol = rol;
    }
    
    public String getEstablecimientoNombre() {
        return establecimientoNombre;
    }
    
    public void setEstablecimientoNombre(String establecimientoNombre) {
        this.establecimientoNombre = establecimientoNombre;
    }
    
    public int getEstablecimientoId() {
        return establecimientoId;
    }
    
    public void setEstablecimientoId(int establecimientoId) {
        this.establecimientoId = establecimientoId;
    }

    public List<Map<String, Object>> getEstablecimientos() {
        return establecimientos;
    }
    
    public void setEstablecimientos(List<Map<String, Object>> establecimientos) {
        this.establecimientos = establecimientos;
    }

    public String getEstablecimientosFormateados() {
        if (establecimientos == null || establecimientos.isEmpty()) {
            return establecimientoNombre != null ? establecimientoNombre : "Sin establecimientos";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < establecimientos.size(); i++) {
            Map<String, Object> est = establecimientos.get(i);
            sb.append(est.get("nombre"));
            if (i < establecimientos.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}