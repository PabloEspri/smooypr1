package com.smooy.smooypr1.usuarios;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.button.MaterialButton;
import com.smooy.smooypr1.R;

import java.util.List;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.ViewHolder> {
    private static final String TAG = "UsuarioAdapter";
    private final Context context;
    private List<Usuario> usuarios;
    private OnDeleteClickListener deleteListener;
    private OnEditClickListener editListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Usuario usuario, int position);
    }

    public interface OnEditClickListener {
        void onEditClick(Usuario usuario, int position);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editListener = listener;
    }

    public UsuarioAdapter(Context context, List<Usuario> usuarios) {
        this.context = context;
        this.usuarios = usuarios;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        
        holder.tvNombreUsuario.setText(usuario.getNombreUsuario());
        holder.tvNombreCompleto.setText(usuario.getNombreCompleto());
        holder.tvRol.setText("Rol: " + usuario.getRol());

        String establecimientos = usuario.getEstablecimientosFormateados();
        holder.tvEstablecimiento.setText("Establecimientos: " + establecimientos);
        holder.tvEstablecimiento.setVisibility(View.VISIBLE);

        holder.btnEditar.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditClick(usuario, position);
            }
        });

        holder.btnEliminar.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(usuario, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usuarios != null ? usuarios.size() : 0;
    }

    public void actualizarLista(List<Usuario> nuevosUsuarios) {
        this.usuarios = nuevosUsuarios;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreUsuario;
        TextView tvNombreCompleto;
        TextView tvRol;
        TextView tvEstablecimiento;
        MaterialButton btnEliminar;
        MaterialButton btnEditar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreUsuario = itemView.findViewById(R.id.tvNombreUsuario);
            tvNombreCompleto = itemView.findViewById(R.id.tvNombreCompleto);
            tvRol = itemView.findViewById(R.id.tvRol);
            tvEstablecimiento = itemView.findViewById(R.id.tvEstablecimiento);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            btnEditar = itemView.findViewById(R.id.btnEditar);
        }
    }
}