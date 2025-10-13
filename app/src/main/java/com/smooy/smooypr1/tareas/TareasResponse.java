package com.smooy.smooypr1.tareas;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TareasResponse {
    @SerializedName("tareas")
    private List<Tarea> tareas;
    
    public List<Tarea> getTareas() {
        return tareas;
    }
    
    public void setTareas(List<Tarea> tareas) {
        this.tareas = tareas;
    }
}