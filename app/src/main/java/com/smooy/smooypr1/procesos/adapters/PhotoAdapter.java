package com.smooy.smooypr1.procesos.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.smooy.smooypr1.R;
import com.smooy.smooypr1.procesos.ProcesoImagen;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    
    private final Context context;
    private List<ProcesoImagen> imagenes;
    private final String baseUrl;
    
    public PhotoAdapter(Context context, List<ProcesoImagen> imagenes, String baseUrl) {
        this.context = context;
        this.imagenes = imagenes;
        this.baseUrl = baseUrl;
    }
    
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imagen, parent, false);
        return new PhotoViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        ProcesoImagen imagen = imagenes.get(position);
        
        // Cargar imagen con Glide - usar getRutaImagen() en lugar de getRuta_imagen()
        String imageUrl = imagen.getRutaImagen(); 
        // Si la ruta no es una URL completa, agregarle la URL base
        if (!imageUrl.startsWith("http")) {
            imageUrl = baseUrl + imageUrl;
        }
        
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.imgFoto);
        
        // Mostrar información de la imagen - usar getNombreUsuario() y getFechaSubida()
        holder.tvUsuarioFoto.setText(imagen.getNombreUsuario());
        holder.tvFechaFoto.setText(imagen.getFechaSubida());
    }
    
    @Override
    public int getItemCount() {
        return imagenes != null ? imagenes.size() : 0;
    }
    
    public void actualizarImagenes(List<ProcesoImagen> nuevasImagenes) {
        this.imagenes = nuevasImagenes;
        notifyDataSetChanged();
    }
    
    public void agregarImagen(ProcesoImagen nuevaImagen) {
        if (this.imagenes != null) {
            this.imagenes.add(0, nuevaImagen);  // Añadir al principio
            notifyItemInserted(0);
        }
    }
    
    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto;
        TextView tvUsuarioFoto, tvFechaFoto;
        
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            tvUsuarioFoto = itemView.findViewById(R.id.tvUsuarioFoto);
            tvFechaFoto = itemView.findViewById(R.id.tvFechaFoto);
        }
    }
}