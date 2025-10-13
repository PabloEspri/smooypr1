package com.smooy.smooypr1.procesos;

import com.google.gson.annotations.SerializedName;

public class EstadoResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("mensaje")
    private String mensaje;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMensaje() {
        return mensaje;
    }
}