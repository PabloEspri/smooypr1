package com.smooy.smooypr1.tareas;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;


import com.smooy.smooypr1.R;
import com.smooy.smooypr1.util.GlideApp;
import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TareaImagenesAdapter extends RecyclerView.Adapter<TareaImagenesAdapter.ViewHolder> {
    private static final String TAG = "TareaImagenesAdapter";
    private final Context context;
    private final List<TareaImagen> imagenes;
    private final String baseUrl;
    private final int tareaId;
    private OnImageClickListener onImageClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private boolean mostrarBotonesEliminar = false;

    public interface OnImageClickListener {
        void onImageClick(String imageUrl);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(TareaImagen imagen);
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setMostrarBotonesEliminar(boolean mostrar) {
        this.mostrarBotonesEliminar = mostrar;
    }
    
    public TareaImagenesAdapter(Context context, List<TareaImagen> imagenes, String baseUrl, int tareaId) {
        this.context = context;
        this.imagenes = imagenes != null ? imagenes : new ArrayList<>();
        this.baseUrl = baseUrl;
        this.tareaId = tareaId;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea_imagen, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TareaImagen imagen = imagenes.get(position);

        // Construir la URL completa de la imagen
        String imageUrl = imagen.getRutaImagen();
        if (!imageUrl.startsWith("http")) {
            if (baseUrl.endsWith("/") && imageUrl.startsWith("/")) {
                imageUrl = baseUrl + imageUrl.substring(1);
            } else if (!baseUrl.endsWith("/") && !imageUrl.startsWith("/")) {
                imageUrl = baseUrl + "/" + imageUrl;
            } else {
                imageUrl = baseUrl + imageUrl;
            }
        }
        
        final String finalImageUrl = imageUrl;
        
        Log.d(TAG, "Cargando imagen desde URL FINAL: " + finalImageUrl);
        
        // Usar Glide directamente
        Glide.with(context)
            .load(finalImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.imageView);

        // Configurar la visibilidad del botón eliminar según el permiso
        holder.btnEliminar.setVisibility(mostrarBotonesEliminar ? View.VISIBLE : View.GONE);
        
        // Configurar el click listener para el botón eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(imagen);
            }
        });

        // Configurar el click listener para ver la imagen en pantalla completa
        holder.imageView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(finalImageUrl);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return imagenes.size();
    }
    
    private void eliminarImagen(int imagenId) {
        Log.d(TAG, "Inicio eliminarImagen con ID: " + imagenId + " y tarea ID: " + tareaId);
        
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.eliminarImagenTarea(tareaId, imagenId);
        
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Imagen eliminada exitosamente");
                    Toast.makeText(context, "Imagen eliminada correctamente", Toast.LENGTH_SHORT).show();

                    for (int i = 0; i < imagenes.size(); i++) {
                        if (imagenes.get(i).getId() == imagenId) {
                            imagenes.remove(i);
                            notifyItemRemoved(i);
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Error eliminando imagen: " + response.code() + " - " + response.message());
                    Toast.makeText(context, "Error al eliminar la imagen", Toast.LENGTH_SHORT).show();
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

    private void tryShowUsuarioNombre(ViewHolder holder, TareaImagen imagen) {
        try {

            String nombreUsuario = null;

            if (nombreUsuario == null) {

                holder.tvUsuario.setText("Usuario");

            } else {
                holder.tvUsuario.setText(nombreUsuario);
                holder.tvUsuario.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            // Si hay algún error, simplemente ocultamos el campo
            holder.tvUsuario.setVisibility(View.GONE);
            Log.e(TAG, "Error al mostrar nombre de usuario", e);
        }
    }

    private void tryShowFechaCreacion(ViewHolder holder, TareaImagen imagen) {
        try {
            String fecha = null;

            if (fecha == null) {
                holder.tvFecha.setText("Fecha");
            } else {
                holder.tvFecha.setText(fecha);
                holder.tvFecha.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            holder.tvFecha.setVisibility(View.GONE);
            Log.e(TAG, "Error al mostrar fecha", e);
        }
    }

    // Método nuevo para construir la URL de la imagen
    private String construirUrlCompleta(String rutaImagen) {
        String url;
        
        if (!rutaImagen.startsWith("http")) {
            if (baseUrl.endsWith("/") && rutaImagen.startsWith("/")) {
                url = baseUrl + rutaImagen.substring(1);
            } else if (!baseUrl.endsWith("/") && !rutaImagen.startsWith("/")) {
                url = baseUrl + "/" + rutaImagen;
            } else {
                url = baseUrl + rutaImagen;
            }
        } else {
            url = rutaImagen;
        }
        
        Log.d(TAG, "URL FINAL DE IMAGEN: " + url);
        return url;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnEliminar;
        TextView tvUsuario, tvFecha;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgTareaFoto);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            tvUsuario = itemView.findViewById(R.id.tvUsuarioFoto);
            tvFecha = itemView.findViewById(R.id.tvFechaFoto);
        }
    }
}