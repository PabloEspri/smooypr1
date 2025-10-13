package com.smooy.smooypr1.tareas;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.R;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TareaComentariosAdapter extends RecyclerView.Adapter<TareaComentariosAdapter.ViewHolder> {

    private static final String TAG = "TareaComentariosAdapter";
    private final Context context;
    private final List<TareaComentario> comentarios;
    private final int tareaId;

    public TareaComentariosAdapter(Context context, List<TareaComentario> comentarios, int tareaId) {
        this.context = context;
        this.comentarios = comentarios;
        this.tareaId = tareaId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comentario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TareaComentario comentario = comentarios.get(position);

        holder.tvUsuario.setText(comentario.getNombreUsuario());
        holder.tvFecha.setText(formatearFecha(comentario.getFechaCreacion()));
        holder.tvComentario.setText(comentario.getComentario());

        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userRole = prefs.getString("rol", "");
        int usuarioId = prefs.getInt("usuario_id", 0);
        Log.d(TAG, "Rol del usuario: " + userRole + ", ID: " + usuarioId);

        if (holder.btnEliminar != null) {

            holder.btnEliminar.setVisibility(View.VISIBLE);
            holder.btnEliminar.setOnClickListener(v -> {
                Log.d(TAG, "Botón eliminar comentario clickado para comentario ID: " + comentario.getId());

                new AlertDialog.Builder(context)
                    .setTitle("Eliminar comentario")
                    .setMessage("¿Estás seguro de que deseas eliminar este comentario?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        Log.d(TAG, "Confirmado: Eliminando comentario ID: " + comentario.getId());
                        eliminarComentario(comentario.getId());
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            });
        } else {
            Log.e(TAG, "El botón de eliminar es null");
        }
    }

    @Override
    public int getItemCount() {
        return comentarios.size();
    }

    private void eliminarComentario(int comentarioId) {
        Log.d(TAG, "Inicio eliminarComentario con ID: " + comentarioId + " y tarea ID: " + tareaId);
        
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.eliminarComentarioTarea(tareaId, comentarioId);
        
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Comentario eliminado exitosamente");
                    Toast.makeText(context, "Comentario eliminado correctamente", Toast.LENGTH_SHORT).show();
                    
                    // Eliminar el comentario de la lista y actualizar el adaptador
                    for (int i = 0; i < comentarios.size(); i++) {
                        if (comentarios.get(i).getId() == comentarioId) {
                            comentarios.remove(i);
                            notifyItemRemoved(i);
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Error eliminando comentario: " + response.code() + " - " + response.message());
                    Toast.makeText(context, "Error al eliminar el comentario", Toast.LENGTH_SHORT).show();
                    try {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error de conexión: " + t.getMessage());
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatearFecha(String fechaStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date fecha = inputFormat.parse(fechaStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            return outputFormat.format(fecha);
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fecha: " + e.getMessage());
            return fechaStr;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsuario;
        TextView tvFecha;
        TextView tvComentario;
        ImageButton btnEliminar;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsuario = itemView.findViewById(R.id.tvUsuario);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvComentario = itemView.findViewById(R.id.tvComentario);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}