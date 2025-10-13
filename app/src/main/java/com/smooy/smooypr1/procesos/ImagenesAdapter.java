package com.smooy.smooypr1.procesos;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smooy.smooypr1.R;


import java.util.ArrayList;
import java.util.List;

public class ImagenesAdapter extends RecyclerView.Adapter<ImagenesAdapter.ViewHolder> {
    private final Context context;
    private final List<ProcesoImagen> imagenes;
    private final String baseUrl;
    private static final String TAG = "ImagenesAdapter";

    public ImagenesAdapter(Context context, List<ProcesoImagen> imagenes, String baseUrl) {
        this.context = context;
        this.imagenes = imagenes != null ? imagenes : new ArrayList<>();
        this.baseUrl = baseUrl;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imagen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProcesoImagen imagen = imagenes.get(position);

        String nombreUsuario = imagen.getNombre_usuario();
        if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
            holder.tvUsuario.setText(nombreUsuario);
        } else {
            holder.tvUsuario.setText("Usuario desconocido");
        }

        String fecha = imagen.getFecha_subida();
        if (fecha != null && !fecha.isEmpty()) {
            holder.tvFecha.setText(fecha);
        } else {
            holder.tvFecha.setText("Fecha desconocida");
        }

        String rutaImagen = imagen.getRutaImagen();
        if (rutaImagen == null || rutaImagen.isEmpty()) {
            rutaImagen = imagen.getRuta_imagen();
        }
        
        String imageUrl = rutaImagen;

        if (imageUrl != null && !imageUrl.startsWith("http")) {
            imageUrl = baseUrl + (imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl);
        }
        
        Log.d(TAG, "URL de imagen final: " + imageUrl);

        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .dontTransform()
            .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imagenes != null ? imagenes.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvUsuario, tvFecha;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgFoto);
            tvUsuario = itemView.findViewById(R.id.tvUsuarioFoto);
            tvFecha = itemView.findViewById(R.id.tvFechaFoto);
        }
    }
}