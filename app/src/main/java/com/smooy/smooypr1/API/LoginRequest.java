package com.smooy.smooypr1.API;

// Clase LoginRequest para los datos de entrada (usuario y contraseña)
public class LoginRequest {
    private String usuario;
    private String contraseña;

    public LoginRequest(String usuario, String contraseña) {
        this.usuario = usuario;
        this.contraseña = contraseña;
    }

    // Getters y setters
    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }
}

