package com.smooy.smooypr1.procesos;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProcesosResponse {
    @SerializedName("procesos")
    private List<Proceso> procesos;

    public List<Proceso> getProcesos() {
        return procesos;
    }

    public void setProcesos(List<Proceso> procesos) {
        this.procesos = procesos;
    }
}