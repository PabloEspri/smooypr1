package com.smooy.smooypr1.avisos; //HOLA

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvisosAdapter extends RecyclerView.Adapter<AvisosAdapter.ViewHolder> {
    private static final String TAG = "AvisosAdapter";
    private List<Aviso> avisos;
    private final boolean esStaff;

    public AvisosAdapter(List<Aviso> avisos, boolean esStaff) {
        this.avisos = avisos != null ? avisos : new ArrayList<>();
        this.esStaff = esStaff;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_aviso, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= avisos.size()) {
            Log.e(TAG, "Invalid position in onBindViewHolder: " + position);
            return;
        }
        Aviso aviso = avisos.get(position);
        holder.bind(aviso);
    }

    @Override
    public int getItemCount() {
        return avisos != null ? avisos.size() : 0;
    }

    public void actualizarLista(List<Aviso> nuevosAvisos) {
        this.avisos = nuevosAvisos != null ? nuevosAvisos : new ArrayList<>();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Usar los IDs que realmente existen en el XML
        private final TextView tvNombre;
        private final TextView tvCategoria;
        private final TextView tvDescripcion;
        private final TextView tvEstablecimiento;
        private final TextView tvUsuario;
        private final TextView tvFecha;
        private final TextView btnBorrar;
        private final TextView btnVerificacion;
        private final ImageView imgAvisoThumbnail; // Nuevo ImageView para la imagen del aviso
        private final Map<Integer, List<AvisoImagen>> imagenesCache = new HashMap<>();

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Encontrar las vistas por ID
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvCategoria = itemView.findViewById(R.id.tvCategoria);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvEstablecimiento = itemView.findViewById(R.id.tvEstablecimiento);
            tvUsuario = itemView.findViewById(R.id.tvUsuario);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            imgAvisoThumbnail = itemView.findViewById(R.id.imgAvisoThumbnail); // Inicializar el ImageView
            
            btnBorrar = itemView.findViewById(R.id.btnBorrar);
            btnVerificacion = itemView.findViewById(R.id.btnVerificacion);

            // Actualizar con lógica específica para rol
            SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String rolActual = sharedPreferences.getString("ROL_USUARIO", "");
            boolean esAdmin = "Admin".equals(rolActual);
            
            // Mostrar botones según rol
            btnBorrar.setVisibility(esStaff ? View.VISIBLE : View.GONE);
            btnVerificacion.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
            
            // Implementar el listener para borrar
            btnBorrar.setOnClickListener(v -> {
                try {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && avisos != null && position < avisos.size()) {
                        borrarAviso(avisos.get(position), position);
                    } else {
                        Log.e(TAG, "No se puede borrar: posición inválida o lista vacía");
                        Toast.makeText(itemView.getContext(), "No se puede realizar esta acción ahora", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al borrar: " + e.getMessage());
                    Toast.makeText(itemView.getContext(), "Error al realizar esta acción", Toast.LENGTH_SHORT).show();
                }
            });
            
            // Añadir listener al botón de verificación
            btnVerificacion.setOnClickListener(v -> {
                try {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && avisos != null && position < avisos.size()) {
                        mostrarDialogoVerificacion(avisos.get(position), position);
                    } else {
                        Log.e(TAG, "No se puede verificar: posición inválida o lista vacía");
                        Toast.makeText(itemView.getContext(), "No se puede realizar esta acción ahora", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al verificar: " + e.getMessage());
                    Toast.makeText(itemView.getContext(), "Error al realizar esta acción", Toast.LENGTH_SHORT).show();
                }
            });

            // Añadir este listener para abrir la vista detallada
            itemView.setOnClickListener(v -> {
                try {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && avisos != null && position < avisos.size()) {
                        Aviso aviso = avisos.get(position);
                        Intent intent = new Intent(itemView.getContext(), AvisosDetalleActivity.class);
                        intent.putExtra("AVISO_ID", aviso.getId());
                        intent.putExtra("AVISO", aviso); // Pasar el objeto completo
                        itemView.getContext().startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al abrir detalle: " + e.getMessage());
                    Toast.makeText(itemView.getContext(), "Error al abrir detalles", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void bind(Aviso aviso) {
            if (tvNombre != null) tvNombre.setText(aviso.getNombre());
            if (tvCategoria != null) tvCategoria.setText("Categoría: " + aviso.getCategoria());
            if (tvDescripcion != null) tvDescripcion.setText(aviso.getDescripcion());
            
            // Establecimiento (si está disponible)
            if (tvEstablecimiento != null) {
                if (aviso.getEstablecimientoNombre() != null) {
                    tvEstablecimiento.setText("Establecimiento: " + aviso.getEstablecimientoNombre());
                } else {
                    tvEstablecimiento.setText("Establecimiento: #" + aviso.getEstablecimientoId());
                }
            }
            
            // Usuario (si está disponible)
            if (tvUsuario != null) {
                if (aviso.getUsuarioNombre() != null) {
                    tvUsuario.setText("Por: " + aviso.getUsuarioNombre());
                } else {
                    tvUsuario.setText("Usuario: #" + aviso.getUsuarioId());
                }
            }
            
            // Fecha
            if (tvFecha != null && aviso.getFechaCreacion() != null) {
                tvFecha.setText("Fecha: " + aviso.getFechaCreacion());
            }
            
            // Estado (mostrado como parte del texto, no como un campo separado)
            if (aviso.getEstado() != null) {
                String textoCategoria = "Categoría: " + aviso.getCategoria() + " (" + aviso.getEstado() + ")";
                if (tvCategoria != null) {
                    tvCategoria.setText(textoCategoria);
                }
                
                // Establecer apariencia del botón de verificación según el estado actual
                if (btnVerificacion != null) {
                    Context context = itemView.getContext();
                    String estado = aviso.getEstado();
                    btnVerificacion.setText(estado);
                    
                    if ("Completado".equals(estado)) {
                        btnVerificacion.setBackgroundTintList(ColorStateList.valueOf(
                                ContextCompat.getColor(context, android.R.color.holo_green_light)));
                    } else if ("Pendiente".equals(estado)) {
                        btnVerificacion.setBackgroundTintList(ColorStateList.valueOf(
                                ContextCompat.getColor(context, android.R.color.holo_orange_light)));
                    } else if ("En Proceso".equals(estado)) {
                        btnVerificacion.setBackgroundTintList(ColorStateList.valueOf(
                                ContextCompat.getColor(context, android.R.color.holo_blue_light)));
                    } else if ("Verificado".equals(estado)) {
                        btnVerificacion.setBackgroundTintList(ColorStateList.valueOf(
                                ContextCompat.getColor(context, android.R.color.holo_green_dark)));
                    } else {
                        // Estado predeterminado (desconocido o no especificado)
                        btnVerificacion.setBackgroundTintList(ColorStateList.valueOf(
                                ContextCompat.getColor(context, android.R.color.darker_gray)));
                    }
                }
            }
            
            // Ocultar siempre la imagen del aviso
            if (imgAvisoThumbnail != null) {
                imgAvisoThumbnail.setVisibility(View.GONE);
            }
            
            // Ya no llamamos a cargarImagenAviso(aviso.getId());
        }
        
        // Método para mostrar un diálogo de opciones para verificar el aviso
        private void mostrarDialogoVerificacion(Aviso aviso, int position) {
            Context context = itemView.getContext();
            String[] opciones = {"Completado", "Pendiente", "En Proceso", "Verificado", "Cancelar"};
            
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Actualizar estado del aviso")
                   .setItems(opciones, (dialog, which) -> {
                       if (which < opciones.length - 1) {  // Si no es "Cancelar"
                           actualizarEstadoAviso(aviso, position, opciones[which]);
                       }
                   })
                   .show();
        }
        
        // Método para actualizar el estado del aviso en la API
        private void actualizarEstadoAviso(Aviso aviso, int position, String nuevoEstado) {
            Context context = itemView.getContext();
            Toast.makeText(context, "Actualizando estado a " + nuevoEstado + "...", Toast.LENGTH_SHORT).show();
            
            // En lugar de crear un aviso mínimo, crear una copia completa del aviso existente
            Aviso avisoActualizado = new Aviso(
                aviso.getNombre(),
                aviso.getCategoria(), 
                aviso.getDescripcion(),
                aviso.getEstablecimientoId(),
                aviso.getUsuarioId()
            );
            
            // Copiar todos los demás campos
            avisoActualizado.setId(aviso.getId());
            avisoActualizado.setFechaCreacion(aviso.getFechaCreacion());
            avisoActualizado.setEstablecimientoNombre(aviso.getEstablecimientoNombre());
            avisoActualizado.setUsuarioNombre(aviso.getUsuarioNombre());
            
            // Solo modificar el estado
            avisoActualizado.setEstado(nuevoEstado);
            
            // Resto del código sin cambios...
            ApiService apiService = ApiClient.getApiService();
            Call<Aviso> call = apiService.actualizarAviso(aviso.getId(), avisoActualizado);
            
            call.enqueue(new Callback<Aviso>() {
                @Override
                public void onResponse(Call<Aviso> call, Response<Aviso> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Actualizar el objeto en la lista
                        avisos.get(position).setEstado(nuevoEstado);
                        notifyItemChanged(position);
                        
                        // Cambiar color del botón según estado
                        if ("Completado".equals(nuevoEstado)) {
                            btnVerificacion.setText("Completado");
                            btnVerificacion.setBackgroundTintList(ColorStateList.valueOf(
                                    ContextCompat.getColor(context, android.R.color.holo_green_light)));
                        } else if ("Pendiente".equals(nuevoEstado)) {
                            btnVerificacion.setText("Pendiente");
                            btnVerificacion.setBackgroundTintList(ColorStateList.valueOf(
                                    ContextCompat.getColor(context, android.R.color.holo_orange_light)));
                        } else {
                            btnVerificacion.setText(nuevoEstado);
                        }
                        
                        Toast.makeText(context, "Estado actualizado a " + nuevoEstado, Toast.LENGTH_SHORT).show();
                    } else {
                        int code = response.code();
                        Log.e(TAG, "Error al actualizar estado: " + code);
                        Toast.makeText(context, "Error al actualizar: " + code, Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<Aviso> call, Throwable t) {
                    Log.e(TAG, "Fallo en la conexión: " + t.getMessage(), t);
                    Toast.makeText(context, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Método para borrar un aviso (sin cambios)
        private void borrarAviso(Aviso aviso, int position) {
            Context context = itemView.getContext();
            
            // Mostrar confirmación o feedback visual
            Toast.makeText(context, "Eliminando aviso...", Toast.LENGTH_SHORT).show();
            
            // Llamar a la API para borrar
            ApiService apiService = ApiClient.getApiService();
            Call<Void> call = apiService.borrarAviso(aviso.getId());
            
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        // Eliminar de la lista local
                        avisos.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, avisos.size());
                        
                        Log.d(TAG, "Aviso eliminado con éxito: ID " + aviso.getId());
                        Toast.makeText(context, "Aviso eliminado correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Error al eliminar aviso: " + response.code());
                        Toast.makeText(context, "Error al eliminar: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "Fallo en la conexión al eliminar aviso", t);
                    Toast.makeText(context, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Modificar el método para usar la caché
        private void cargarImagenAviso(int avisoId) {
            // Si ya tenemos las imágenes en caché, usarlas
            if (imagenesCache.containsKey(avisoId)) {
                List<AvisoImagen> imagenes = imagenesCache.get(avisoId);
                if (!imagenes.isEmpty()) {
                    // Mostrar la primera imagen
                    AvisoImagen imagen = imagenes.get(0);
                    String imageUrl = ApiClient.BASE_URL + imagen.getRutaImagen();
                    mostrarImagen(imageUrl);
                } else {
                    imgAvisoThumbnail.setVisibility(View.GONE);
                }
                return;
            }
            
            // Si no están en caché, hacer la llamada a la API
            ApiService apiService = ApiClient.getApiService();
            Call<List<AvisoImagen>> call = apiService.obtenerImagenesAviso(avisoId);
            
            call.enqueue(new Callback<List<AvisoImagen>>() {
                @Override
                public void onResponse(Call<List<AvisoImagen>> call, Response<List<AvisoImagen>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<AvisoImagen> imagenes = response.body();
                        // Guardar en caché
                        imagenesCache.put(avisoId, imagenes);
                        
                        if (!imagenes.isEmpty()) {
                            // Mostrar la primera imagen
                            AvisoImagen imagen = imagenes.get(0);
                            String imageUrl = ApiClient.BASE_URL + imagen.getRutaImagen();
                            mostrarImagen(imageUrl);
                        } else {
                            imgAvisoThumbnail.setVisibility(View.GONE);
                        }
                    } else {
                        imgAvisoThumbnail.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Call<List<AvisoImagen>> call, Throwable t) {
                    imgAvisoThumbnail.setVisibility(View.GONE);
                    Log.e("AvisosAdapter", "Error al cargar imágenes: " + t.getMessage());
                }
            });
        }

        private void mostrarImagen(String imageUrl) {
            imgAvisoThumbnail.setVisibility(View.VISIBLE);
            
            RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL);
            
            Glide.with(itemView.getContext())
                .load(imageUrl)
                .apply(options)
                .into(imgAvisoThumbnail);
            
            Log.d("AvisosAdapter", "Cargando imagen: " + imageUrl);
        }
    }
}