package com.smooy.smooypr1.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EstablecimientoResponse {
    @SerializedName("establecimientos")
    private List<Establecimiento> establecimientos;

    public List<Establecimiento> getEstablecimientos() {
        return establecimientos;
    }

    public void setEstablecimientos(List<Establecimiento> establecimientos) {
        this.establecimientos = establecimientos;
    }
}