package com.smooy.smooypr1.procesos;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ComentariosResponse {
    @SerializedName("comentarios")
    private List<ProcesoComentario> comentarios;

    public List<ProcesoComentario> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<ProcesoComentario> comentarios) {
        this.comentarios = comentarios;
    }
}