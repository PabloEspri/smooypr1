package com.smooy.smooypr1.procesos.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.smooy.smooypr1.R;
import com.smooy.smooypr1.procesos.ProcesoComentario;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    
    private List<ProcesoComentario> comentarios;
    
    public CommentAdapter(List<ProcesoComentario> comentarios) {
        this.comentarios = comentarios;
    }
    
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comentario, parent, false);
        return new CommentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ProcesoComentario comentario = comentarios.get(position);
        holder.tvUsuario.setText(comentario.getNombre_usuario());
        holder.tvFecha.setText(comentario.getFecha_creacion());
        holder.tvComentario.setText(comentario.getComentario());
    }
    
    @Override
    public int getItemCount() {
        return comentarios != null ? comentarios.size() : 0;
    }
    
    public void actualizarComentarios(List<ProcesoComentario> nuevosComentarios) {
        this.comentarios = nuevosComentarios;
        notifyDataSetChanged();
    }
    
    public void agregarComentario(ProcesoComentario nuevoComentario) {
        if (this.comentarios != null) {
            this.comentarios.add(0, nuevoComentario);  // Añadir al principio
            notifyItemInserted(0);
        }
    }
    
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsuario, tvFecha, tvComentario;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsuario = itemView.findViewById(R.id.tvUsuario);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvComentario = itemView.findViewById(R.id.tvComentario);
        }
    }
}