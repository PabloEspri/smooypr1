package com.smooy.smooypr1.procesos;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.R;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComentariosAdapter extends RecyclerView.Adapter<ComentariosAdapter.ViewHolder> {
    private final Context context;
    private final List<ProcesoComentario> comentarios;
    private final boolean esAdminOAreaManager;
    private final int procesoId;

    public ComentariosAdapter(Context context, List<ProcesoComentario> comentarios, boolean esAdminOAreaManager, int procesoId) {
        this.context = context;
        this.comentarios = comentarios;
        this.esAdminOAreaManager = esAdminOAreaManager;
        this.procesoId = procesoId;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comentario, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProcesoComentario comentario = comentarios.get(position);

        holder.tvComentario.setText(comentario.getComentario());
        holder.tvUsuario.setText(comentario.getNombreUsuario());
        holder.tvFecha.setText(comentario.getFechaCreacion());

        if (esAdminOAreaManager) {
            holder.btnEliminar.setVisibility(View.VISIBLE);
            holder.btnEliminar.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                    .setTitle("Eliminar Comentario")
                    .setMessage("¿Estás seguro de que deseas eliminar este comentario?")
                    .setPositiveButton("Sí", (dialog, which) -> eliminarComentario(comentario.getId(), position))
                    .setNegativeButton("No", null)
                    .show();
            });
        } else {
            holder.btnEliminar.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return comentarios != null ? comentarios.size() : 0;
    }

    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsuario, tvFecha, tvComentario;
        View btnEliminar;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsuario = itemView.findViewById(R.id.tvUsuario);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvComentario = itemView.findViewById(R.id.tvComentario);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
    
    private void eliminarComentario(int idComentario, int position) {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.eliminarComentarioProceso(procesoId, idComentario);
        
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d("ComentariosAdapter", "Comentario eliminado exitosamente");
                    Toast.makeText(context, "Comentario eliminado correctamente", Toast.LENGTH_SHORT).show();

                    comentarios.remove(position);
                    notifyItemRemoved(position);
                } else {
                    Log.e("ComentariosAdapter", "Error eliminando comentario: " + response.code() + " - " + response.message());
                    Toast.makeText(context, "Error al eliminar el comentario", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("ComentariosAdapter", "Fallo en la llamada para eliminar comentario", t);
                Toast.makeText(context, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}