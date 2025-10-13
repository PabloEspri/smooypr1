package com.smooy.smooypr1.establecimientos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smooy.smooypr1.R;
import com.smooy.smooypr1.Establecimientos;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class EstablecimientoAdapter extends RecyclerView.Adapter<EstablecimientoAdapter.ViewHolder> {

    private final Context context;
    private List<Establecimientos> establecimientos;
    private OnDeleteClickListener onDeleteClickListener;
    private OnItemClickListener onItemClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Establecimientos establecimiento, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(Establecimientos establecimiento, int position);
    }

    public EstablecimientoAdapter(Context context, List<Establecimientos> establecimientos) {
        this.context = context;
        this.establecimientos = establecimientos;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_establecimiento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Establecimientos establecimiento = establecimientos.get(position);

        holder.tvNombre.setText(establecimiento.getNombre());
        holder.tvDireccion.setText(establecimiento.getDireccion());
        holder.tvTipo.setText("Tipo: " + establecimiento.getTipo());
        holder.tvEstado.setText("Estado: " + establecimiento.getEstado());

        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(establecimiento, holder.getAdapterPosition());
            }
        });

        holder.cardView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(establecimiento, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return establecimientos.size();
    }

    public void actualizarLista(List<Establecimientos> nuevaLista) {
        this.establecimientos = nuevaLista;
        notifyDataSetChanged();
    }

    public void eliminarEstablecimiento(int position) {
        if (position >= 0 && position < establecimientos.size()) {
            establecimientos.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDireccion, tvTipo, tvEstado;
        MaterialButton btnDelete;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreEstablecimiento);
            tvDireccion = itemView.findViewById(R.id.tvDireccionEstablecimiento);
            tvTipo = itemView.findViewById(R.id.tvTipoEstablecimiento);
            tvEstado = itemView.findViewById(R.id.tvEstadoEstablecimiento);
            btnDelete = itemView.findViewById(R.id.btnDeleteEstablecimiento);
            cardView = (MaterialCardView) itemView;
        }
    }
}