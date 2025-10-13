package com.smooy.smooypr1.avisos;

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


import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvisoComentariosAdapter extends RecyclerView.Adapter<AvisoComentariosAdapter.ViewHolder> {

    private static final String TAG = "AvisoComentariosAdapter";
    private final Context context;
    private final List<AvisoComentario> comentarios;
    private final int avisoId;
    private boolean mostrarBotonesBorrar = false;

    public AvisoComentariosAdapter(Context context, List<AvisoComentario> comentarios, int avisoId) {
        this.context = context;
        this.comentarios = comentarios;
        this.avisoId = avisoId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comentario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AvisoComentario comentario = comentarios.get(position);
        
        // Configurar nombre de usuario
        String nombreUsuario = comentario.getNombreUsuario();
        if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
            holder.tvUsuario.setText(nombreUsuario);
        } else {
            holder.tvUsuario.setText("Usuario #" + comentario.getUsuarioId());
        }

        holder.tvComentario.setText(comentario.getComentario());
        holder.tvFecha.setText(comentario.getFechaCreacion());

        if (holder.btnEliminar != null) {
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userRole = prefs.getString("ROL_USUARIO", "");
            int usuarioId = prefs.getInt("USER_ID", -1);
            
            // Mostrar botón eliminar solo para Admin, Staff o el autor del comentario
            boolean puedeEliminar = "Admin".equalsIgnoreCase(userRole) || 
                                  "Staff".equalsIgnoreCase(userRole) ||
                                  usuarioId == comentario.getUsuarioId() ||
                                  mostrarBotonesBorrar;
            
            if (puedeEliminar) {
                holder.btnEliminar.setVisibility(View.VISIBLE);
                
                // Configurar listener para eliminar comentario
                final int comentarioId = comentario.getId();
                holder.btnEliminar.setOnClickListener(v -> {
                    Log.d(TAG, "Botón eliminar comentario clickado para comentario ID: " + comentarioId);
                    // Mostrar diálogo de confirmación
                    new AlertDialog.Builder(context)
                        .setTitle("Eliminar comentario")
                        .setMessage("¿Estás seguro de que deseas eliminar este comentario?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            Log.d(TAG, "Confirmado: Eliminando comentario ID: " + comentarioId);
                            eliminarComentario(comentarioId, position);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                });
            } else {
                holder.btnEliminar.setVisibility(View.GONE);
            }
        } else {
            Log.e(TAG, "El botón de eliminar es null");
        }
    }

    @Override
    public int getItemCount() {
        return comentarios.size();
    }

    // Método para establecer si se muestran los botones de eliminar
    public void setMostrarBotonesBorrar(boolean mostrar) {
        this.mostrarBotonesBorrar = mostrar;
        notifyDataSetChanged();
    }

    private void eliminarComentario(int comentarioId, int position) {
        Log.d(TAG, "Inicio eliminarComentario con ID: " + comentarioId + " y aviso ID: " + avisoId);
        
        // Verificar que tengamos un comentario válido en la posición especificada
        if (position < 0 || position >= comentarios.size()) {
            Log.e(TAG, "Error: posición inválida: " + position);
            Toast.makeText(context, "Error: comentario no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        AvisoComentario comentario = comentarios.get(position);
        
        if (comentario.getId() != comentarioId) {
            Log.e(TAG, "Error: ID de comentario inconsistente: " + comentarioId + " vs " + comentario.getId());
            comentarioId = comentario.getId();
        }

        int avisoIdReal = comentario.getAvisoId();
        
        Log.d(TAG, "Datos del comentario a eliminar: ID=" + comentarioId + ", avisoId=" + avisoIdReal);
        
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.eliminarComentarioAviso(avisoIdReal, comentarioId);
        
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Comentario eliminado exitosamente");
                    Toast.makeText(context, "Comentario eliminado correctamente", Toast.LENGTH_SHORT).show();
                    
                    // Eliminar el comentario de la lista y actualizar el adaptador
                    comentarios.remove(position);
                    notifyItemRemoved(position);
                    
                    // Si el contexto es AvisosDetalleActivity, actualizar vista vacía si es necesario
                    if (context instanceof AvisosDetalleActivity) {
                        ((AvisosDetalleActivity) context).actualizarVistaComentariosVacia();
                    }
                } else {
                    String errorMessage = "Error al eliminar el comentario: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer mensaje de error", e);
                    }
                    
                    Log.e(TAG, errorMessage);
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Fallo en la conexión al eliminar comentario", t);
                Toast.makeText(context, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsuario, tvComentario, tvFecha;
        ImageButton btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsuario = itemView.findViewById(R.id.tvUsuario);
            tvComentario = itemView.findViewById(R.id.tvComentario);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}