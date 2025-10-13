package com.smooy.smooypr1.procesos;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;



import com.smooy.smooypr1.Establecimientos;
import com.google.android.material.button.MaterialButton;
import com.smooy.smooypr1.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.Color;

public class ProcesoAdapter extends RecyclerView.Adapter<ProcesoAdapter.ViewHolder> {
    private static final String TAG = "ProcesoAdapter";
    private final Context context;
    private final List<Proceso> procesos;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private OnVerifyClickListener verifyListener;
    private boolean esStaff = false;
    private boolean esAdmin = false;
    private final Map<Integer, String> nombresEstablecimientos = new HashMap<>();

    public void setStaffMode(boolean esStaff) {
        this.esStaff = esStaff;
    }

    public void setAdminMode(boolean esAdmin) {
        this.esAdmin = esAdmin;
        Log.d(TAG, "🔄 Admin Mode establecido en: " + esAdmin);
        notifyDataSetChanged();
    }

    public void cargarNombresEstablecimientos(List<Establecimientos> establecimientos) {
        for (Establecimientos establecimiento : establecimientos) {
            nombresEstablecimientos.put(establecimiento.getId(), establecimiento.getNombre());
        }
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(Proceso proceso);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Proceso proceso);
    }

    public interface OnVerifyClickListener {
        void onVerifyClick(Proceso proceso, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setOnVerifyClickListener(OnVerifyClickListener verifyListener) {
        this.verifyListener = verifyListener;
    }

    public ProcesoAdapter(Context context, List<Proceso> procesos) {
        this.context = context;
        this.procesos = procesos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_proceso, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Proceso proceso = procesos.get(position);

        String tipoProceso = proceso.getTipo_proceso();
        holder.tvTipoProceso.setText(tipoProceso != null ? tipoProceso.toUpperCase() : "PROCESO");
        holder.tvDescripcionBreve.setText("PROCESO #" + proceso.getId());
        holder.tvDescripcion.setText(proceso.getDescripcion());
        holder.tvFechaHora.setText(proceso.getHorario() + " - " + proceso.getFecha_inicio());

        // Mostrar el estado del proceso
        if (holder.tvEstado != null) {
            String estado = proceso.getEstado();
            if (estado != null && !estado.isEmpty()) {
                holder.tvEstado.setText("Estado: " + estado);
                holder.tvEstado.setVisibility(View.VISIBLE);

                // Opcional: destacar visualmente los procesos verificados
                if ("verificado".equalsIgnoreCase(estado)) {
                    holder.tvEstado.setTextColor(Color.GREEN);
                } else {
                    holder.tvEstado.setTextColor(Color.BLACK);
                }
            } else {
                holder.tvEstado.setVisibility(View.GONE);
            }
        }

        if (holder.tvEstablecimiento != null) {
            int establecimientoId = proceso.getEstablecimiento_id();
            String nombreEstablecimiento = nombresEstablecimientos.get(establecimientoId);

            if (nombreEstablecimiento != null) {
                holder.tvEstablecimiento.setText("Establecimiento: " + nombreEstablecimiento);
                holder.tvEstablecimiento.setVisibility(View.VISIBLE);
            } else {
                holder.tvEstablecimiento.setText("Establecimiento: ID " + establecimientoId);
                holder.tvEstablecimiento.setVisibility(View.VISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(proceso);
            }
        });

        if (holder.btnEliminar != null) {
            if (deleteListener != null && esAdmin) {
                holder.btnEliminar.setVisibility(View.VISIBLE);
                holder.btnEliminar.setOnClickListener(v -> deleteListener.onDeleteClick(proceso));
            } else {
                holder.btnEliminar.setVisibility(View.GONE);
            }
        }

        // Configurar el botón de verificar
        if (holder.btnVerificar != null) {
            boolean esVerificado = "verificado".equalsIgnoreCase(proceso.getEstado());
            Log.d(TAG, "Proceso ID: " + proceso.getId() + ", Estado: " + proceso.getEstado() + ", Es verificado: " + esVerificado);

            if (esVerificado) {
                // Si ya está verificado, ocultar el botón
                holder.btnVerificar.setVisibility(View.GONE);
                Log.d(TAG, "Botón verificar OCULTO para proceso ID: " + proceso.getId());
            } else {
                // Mostrar el botón SOLO si no está verificado y el usuario es ADMIN o AREA MANAGER
                // No mostrarlo para usuarios Staff normales
                boolean esAreaManager = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        .getString("ROL_USUARIO", "").equalsIgnoreCase("Area Manager");
                boolean mostrarBoton = (esAdmin || esAreaManager);
                holder.btnVerificar.setVisibility(mostrarBoton ? View.VISIBLE : View.GONE);
                Log.d(TAG, "Botón verificar " + (mostrarBoton ? "VISIBLE" : "OCULTO") + " para proceso ID: " + proceso.getId());

                // Configurar el click listener para el botón de verificar
                if (mostrarBoton) {
                    holder.btnVerificar.setOnClickListener(v -> {
                        if (verifyListener != null) {
                            // Mostrar diálogo de confirmación
                            new AlertDialog.Builder(context)
                                    .setTitle("Verificar proceso")
                                    .setMessage("¿Estás seguro de que quieres verificar este proceso?")
                                    .setPositiveButton("Sí, verificar", (dialog, which) -> {
                                        Log.d(TAG, "Verificando proceso ID: " + proceso.getId() + " en posición: " + position);
                                        verifyListener.onVerifyClick(proceso, position);
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();
                        }
                    });
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return procesos.size();
    }

    public void actualizarEstadoProceso(int position, String nuevoEstado) {
        if (position >= 0 && position < procesos.size()) {
            Proceso proceso = procesos.get(position);
            proceso.setEstado(nuevoEstado);

            // Force the view update
            notifyItemChanged(position);

            Log.d(TAG, "Estado actualizado para proceso ID: " + proceso.getId() +
                    ", nuevo estado: " + nuevoEstado);
        } else {
            Log.e(TAG, "Error al actualizar estado: posición inválida " + position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipoProceso;
        TextView tvDescripcionBreve;
        TextView tvDescripcion;
        TextView tvFechaHora;
        TextView tvEstablecimiento;
        TextView tvEstado;
        TextView btnEliminar;
        MaterialButton btnVerificar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipoProceso = itemView.findViewById(R.id.tvTipoProceso);
            tvDescripcionBreve = itemView.findViewById(R.id.tvDescripcionBreve);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvEstablecimiento = itemView.findViewById(R.id.tvEstablecimiento);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            btnVerificar = itemView.findViewById(R.id.btnVerificar);
        }
    }
}