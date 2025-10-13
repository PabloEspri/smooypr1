package com.smooy.smooypr1.avisos;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.smooy.smooypr1.R;


import java.util.ArrayList;
import java.util.List;

public class AvisoImagenesAdapter extends RecyclerView.Adapter<AvisoImagenesAdapter.ViewHolder> {
    private final Context context;
    private final List<AvisoImagen> imagenes;
    private final String baseUrl;
    private static final String TAG = "AvisoImagenesAdapter";
    private boolean mostrarBotonesEliminar = false;

    public AvisoImagenesAdapter(Context context, List<AvisoImagen> imagenes, String baseUrl) {
        this.context = context;
        this.imagenes = imagenes != null ? imagenes : new ArrayList<>();
        this.baseUrl = baseUrl;
    }

    public interface OnImageClickListener {
        void onImageClick(String imageUrl);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(AvisoImagen imagen);
    }

    private OnImageClickListener onImageClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setMostrarBotonesEliminar(boolean mostrar) {
        this.mostrarBotonesEliminar = mostrar;
        notifyDataSetChanged();
        Log.d(TAG, "mostrarBotonesEliminar cambiado a: " + mostrar);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imagen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AvisoImagen imagen = imagenes.get(position);

        // Manejo seguro para el nombre de usuario
        String nombreUsuario = imagen.getNombreUsuario();
        if (nombreUsuario != null) {
            holder.tvUsuario.setText(nombreUsuario);
        } else {
            holder.tvUsuario.setText("Usuario desconocido");
        }

        // Manejo seguro para la fecha
        String fechaSubida = imagen.getFechaSubida();
        if (fechaSubida != null) {
            holder.tvFecha.setText(fechaSubida);
        } else {
            holder.tvFecha.setText("Fecha desconocida");
        }

        // Construir y cargar la URL de la imagen
        String rutaImagen = imagen.getRutaImagen();

        String imageUrl = rutaImagen;

        // Verificar y arreglar la URL - CORREGIDO
        if (imageUrl != null) {
            if (!imageUrl.startsWith("http")) {
                // Asegurar que no hay dobles barras al concatenar
                if (baseUrl.endsWith("/") && imageUrl.startsWith("/")) {
                    imageUrl = baseUrl + imageUrl.substring(1);
                } else if (!baseUrl.endsWith("/") && !imageUrl.startsWith("/")) {
                    imageUrl = baseUrl + "/" + imageUrl;
                } else {
                    imageUrl = baseUrl + imageUrl;
                }
            }
            Log.d(TAG, "URL de imagen final: " + imageUrl);
        }

        // Modificar las opciones de Glide - CORREGIDO
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontTransform();

        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(holder.imageView);

        // Manejar click en la imagen
        holder.imageView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(imagen.getRutaImagen());
            }
        });

        // Configurar el botón de eliminar - Asegurarnos que existe y lo configuramos correctamente
        if (holder.btnEliminar != null) {
            if (mostrarBotonesEliminar) {
                holder.btnEliminar.setVisibility(View.VISIBLE);
                holder.btnEliminar.setOnClickListener(v -> {
                    if (onDeleteClickListener != null) {
                        Log.d(TAG, "Botón eliminar pulsado para imagen id: " + imagen.getId());
                        onDeleteClickListener.onDeleteClick(imagen);
                    }
                });
            } else {
                holder.btnEliminar.setVisibility(View.GONE);
            }
        } else {
            Log.e(TAG, "Error: btnEliminar es null! Verifica que el ID en el layout coincide.");
        }
    }

    @Override
    public int getItemCount() {
        return imagenes != null ? imagenes.size() : 0;
    }

    // Método para actualizar las imágenes después de una nueva carga
    public void actualizarImagenes(List<AvisoImagen> nuevasImagenes) {
        this.imagenes.clear();
        if (nuevasImagenes != null) {
            this.imagenes.addAll(nuevasImagenes);
        }
        notifyDataSetChanged();
        Log.d(TAG, "Imágenes actualizadas: " + imagenes.size());
    }

    // Método para eliminar una imagen de la lista local
    public void eliminarImagen(int id) {
        for (int i = 0; i < imagenes.size(); i++) {
            if (imagenes.get(i).getId() == id) {
                imagenes.remove(i);
                notifyItemRemoved(i);
                Log.d(TAG, "Imagen con id: " + id + " eliminada del adaptador");
                break;
            }
        }

        // Actualizar la vista vacía si corresponde
        if (imagenes.isEmpty() && context instanceof AvisosDetalleActivity) {
            ((AvisosDetalleActivity) context).actualizarVistaVacia();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvUsuario, tvFecha;
        ImageButton btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgFoto);
            tvUsuario = itemView.findViewById(R.id.tvUsuarioFoto);
            tvFecha = itemView.findViewById(R.id.tvFechaFoto);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);

            // Debug para verificar si el botón está presente
            if (btnEliminar == null) {
                Log.e(TAG, "ADVERTENCIA: btnBorrar no encontrado en el layout");
            }
        }
    }
}