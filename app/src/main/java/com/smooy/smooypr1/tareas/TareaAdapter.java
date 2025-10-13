package com.smooy.smooypr1.tareas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.smooy.smooypr1.R;

import java.util.List;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.ViewHolder> {
    private final Context context;
    private final List<Tarea> tareas;
    private OnTareaCheckedListener tareaCheckedListener;
    private OnTareaClickListener tareaClickListener;
    private final boolean esStaff;
    private boolean verificationEnabled = false;
    public interface OnTareaCheckedListener {
        void onTareaChecked(Tarea tarea, boolean isChecked);
    }

    public interface OnTareaClickListener {
        void onTareaClick(Tarea tarea);
    }

    public TareaAdapter(Context context, List<Tarea> tareas, boolean esStaff) {
        this.context = context;
        this.tareas = tareas;
        this.esStaff = esStaff;
    }

    public void setOnTareaCheckedListener(OnTareaCheckedListener listener) {
        this.tareaCheckedListener = listener;
    }
    
    public void setOnTareaClickListener(OnTareaClickListener listener) {
        this.tareaClickListener = listener;
    }

    public void setVerificationEnabled(boolean enabled) { // Add this method
        this.verificationEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tarea, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tarea tarea = tareas.get(position);

        holder.tvNombreTarea.setText(tarea.getNombre());
        holder.tvDescripcionTarea.setText(tarea.getDescripcion());

        holder.tvOrdenTarea.setText(String.valueOf(tarea.getOrden()));

        boolean estaCompletada = "Completada".equals(tarea.getEstado());
        holder.cbCompletada.setChecked(estaCompletada);

        // Permitir que CUALQUIER usuario pueda marcar/desmarcar tareas
        holder.cbCompletada.setEnabled(true);

        if (estaCompletada && tarea.getFechaCompletado() != null) {
            holder.tvFechaCompletado.setVisibility(View.VISIBLE);
            holder.tvFechaCompletado.setText("Completada: " + tarea.getFechaCompletado());
        } else {
            holder.tvFechaCompletado.setVisibility(View.GONE);
        }

        holder.cbCompletada.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed() && tareaCheckedListener != null) {
                tareaCheckedListener.onTareaChecked(tarea, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (tareaClickListener != null) {
                tareaClickListener.onTareaClick(tarea);
            }
        });

        // Eliminar la restricción para que cualquier usuario pueda marcar tareas
        // No hay necesidad de mostrar mensaje de restricción
    }

    @Override
    public int getItemCount() {
        return tareas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreTarea;
        TextView tvDescripcionTarea;
        TextView tvOrdenTarea;
        TextView tvFechaCompletado;
        CheckBox cbCompletada;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreTarea = itemView.findViewById(R.id.tvNombreTarea);
            tvDescripcionTarea = itemView.findViewById(R.id.tvDescripcionTarea);
            tvOrdenTarea = itemView.findViewById(R.id.tvOrdenTarea);
            tvFechaCompletado = itemView.findViewById(R.id.tvFechaCompletado);
            cbCompletada = itemView.findViewById(R.id.cbCompletada);
        }
    }
}