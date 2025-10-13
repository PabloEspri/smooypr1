package com.smooy.smooypr1.tareas;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Add these imports if they're not already present
import android.app.Dialog;
import android.widget.ProgressBar;
import com.bumptech.glide.Glide;
import com.smooy.smooypr1.R;

public class TareaDetalleActivity extends AppCompatActivity {

    private static final String TAG = "TareaDetalleActivity";
    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int REQUEST_CREATE_AVISO = 300; // New request code for creating notifications

    private TextView tvNombreTarea, tvDescripcionTarea, tvEstado;
    private CheckBox cbCompletada;
    private Button btnVolver, btnSubmitComment, btnUploadPhoto;
    private Button btnCrearAviso; // New button for creating notifications
    private EditText etNewComment;
    private ImageView imgPhotoPreview;
    private RecyclerView comentariosRecyclerView, imagenesRecyclerView;
    private LinearLayout commentInputLayout, photoInputLayout;
    private TextView emptyCommentsView, emptyPhotosView;
    private FloatingActionButton fabAddContent;

    private TareaComentariosAdapter comentariosAdapter;
    private TareaImagenesAdapter imagenesAdapter;

    private Tarea tarea;
    private int tareaId;
    private int usuarioId;
    private String nombreUsuario;
    private Uri selectedImageUri;
    private File photoFile;
    private final List<TareaComentario> comentarios = new ArrayList<>();
    private final List<TareaImagen> imagenes = new ArrayList<>();
    private boolean esStaff = false;
    private ApiService apiService;

    private Button btnAgregarComentario, btnAgregarFoto;
    private String rolUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tarea_detalle);

        tareaId = getIntent().getIntExtra("TAREA_ID", -1);
        Log.d(TAG, "Tarea ID recibido: " + tareaId);

        if (tareaId == -1) {
            Log.e(TAG, "Error: No se pudo identificar la tarea");
            finish();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        usuarioId = sharedPreferences.getInt("USER_ID", -1);
        String rol = sharedPreferences.getString("ROL_USUARIO", "");
        esStaff = rol.equalsIgnoreCase("Staff") || rol.equalsIgnoreCase("Admin");

        apiService = ApiClient.getApiService();
        initViews();

        configureViewsByRole(esStaff);

        setupListeners();

        cargarDatosTarea(tareaId);
        

        cargarComentarios(tareaId);
        cargarImagenes(tareaId);
    }

    private void initViews() {

        tvNombreTarea = findViewById(R.id.tvNombreTarea);
        tvDescripcionTarea = findViewById(R.id.tvDescripcionTarea);
        tvEstado = findViewById(R.id.tvEstado);
        cbCompletada = findViewById(R.id.cbCompletada);
        btnVolver = findViewById(R.id.btnVolver);

        comentariosRecyclerView = findViewById(R.id.comentariosRecyclerView);
        imagenesRecyclerView = findViewById(R.id.imagenesRecyclerView);
        emptyCommentsView = findViewById(R.id.emptyCommentsView);
        emptyPhotosView = findViewById(R.id.emptyPhotosView);

        imgPhotoPreview = findViewById(R.id.imgPhotoPreview);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);

        btnAgregarComentario = findViewById(R.id.btnAgregarComentario);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);
        btnCrearAviso = findViewById(R.id.btnCrearAviso); // Initialize the new button

        comentariosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        imagenesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        comentariosAdapter = new TareaComentariosAdapter(this, comentarios, tareaId);
        imagenesAdapter = new TareaImagenesAdapter(this, imagenes, ApiClient.BASE_URL, tareaId);
        imagenesAdapter.setOnImageClickListener(this::showFullscreenImage);
        imagenesRecyclerView.setAdapter(imagenesAdapter);
        
        comentariosRecyclerView.setAdapter(comentariosAdapter);
        imagenesRecyclerView.setAdapter(imagenesAdapter);

        Log.d(TAG, "Views initialized: " +
              "imgPhotoPreview=" + (imgPhotoPreview != null) +
              ", btnUploadPhoto=" + (btnUploadPhoto != null));
    }

    private void configureViewsByRole(boolean isStaff) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String rol = sharedPreferences.getString("ROL_USUARIO", "");

        // Los roles Admin y Area Manager siempre pueden verificar tareas
        boolean isAdminOrAreaManager = rol.equalsIgnoreCase("Admin") || rol.equalsIgnoreCase("Area Manager");
        boolean isStaffOrStoreManager = rol.equalsIgnoreCase("Staff") || rol.equalsIgnoreCase("StoreManager");
        
        // Inicialmente, deshabilitar el checkbox para Staff/StoreManager
        // Se habilitará después en cargarImagenes() si hay fotos
        cbCompletada.setEnabled(isAdminOrAreaManager);

        btnAgregarComentario.setVisibility(isStaff ? View.VISIBLE : View.GONE);
        btnAgregarFoto.setVisibility(isStaff ? View.VISIBLE : View.GONE);
        
        // Mostrar el botón de crear aviso solo para Admin y Area Manager
        if (btnCrearAviso != null) {
            btnCrearAviso.setVisibility(isAdminOrAreaManager ? View.VISIBLE : View.GONE);
        }

        // Para Staff y StoreManager, mostrar mensaje específico cuando intenten marcar sin fotos
        if (isStaffOrStoreManager) {
            cbCompletada.setOnClickListener(v -> {
                if (!cbCompletada.isEnabled()) {
                    Toast.makeText(TareaDetalleActivity.this, 
                        "Debe subir al menos una foto antes de completar la tarea", 
                        Toast.LENGTH_SHORT).show();
                    cbCompletada.setChecked(false);
                }
            });
        }
        
        // Guardamos el rol para usarlo después en cargarImagenes()
        this.rolUsuario = rol;
    }

    private void setupListeners() {

        // Reemplazar el listener de volver para establecer un resultado
        btnVolver.setOnClickListener(v -> {
            // Configurar un resultado para que TareasProcesoActivity sepa que debe recargar
            Intent resultIntent = new Intent();
            resultIntent.putExtra("DATOS_ACTUALIZADOS", true);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnAgregarComentario.setOnClickListener(v -> openCommentDialog());

        btnAgregarFoto.setOnClickListener(v -> {
            if (checkPermissions()) {
                openCamera();
            } else {
                requestPermissions();
            }
        });
        
        // Listener para el botón de crear aviso
        if (btnCrearAviso != null) {
            btnCrearAviso.setOnClickListener(v -> {
                if (tarea != null) {
                    crearAvisoParaTarea();
                } else {
                    Toast.makeText(this, "Error: No se ha cargado la tarea", Toast.LENGTH_SHORT).show();
                }
            });
        }

        cbCompletada.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                actualizarEstadoTarea(isChecked);
            }
        });
    }

    private void cargarDatosTarea(int tareaId) {
        Log.d(TAG, "Iniciando carga de datos para tarea ID: " + tareaId);
        
        Call<Tarea> call = apiService.obtenerTareaPorId(tareaId);
        call.enqueue(new Callback<Tarea>() {
            @Override
            public void onResponse(Call<Tarea> call, Response<Tarea> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tarea = response.body();
                    mostrarDatosTarea(tarea);
                } else {

                    int codigo = response.code();
                    Log.e(TAG, "Error al cargar tarea: Código " + codigo);
                    
                    if (codigo == 404) {

                        intentarCargarTareaDirectamente(tareaId);
                    } else {
                        Log.e(TAG, "Error al cargar detalles de la tarea (Código: " + codigo + ")");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<Tarea> call, Throwable t) {
                Log.e(TAG, "Error de red al cargar tarea", t);
                Log.e(TAG, "Error de conexión: " + t.getMessage());
            }
        });
    }

    private void intentarCargarTareaDirectamente(int tareaId) {
        // First get the proceso_id from the intent or other source if available
        int procesoId = getIntent().getIntExtra("PROCESO_ID", -1);
        
        if (procesoId != -1) {

            Call<List<Tarea>> call = apiService.obtenerTareasProceso(procesoId);
            call.enqueue(new Callback<List<Tarea>>() {
                @Override
                public void onResponse(Call<List<Tarea>> call, Response<List<Tarea>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Tarea t : response.body()) {
                            if (t.getId() == tareaId) {
                                tarea = t;
                                mostrarDatosTarea(tarea);
                                return;
                            }
                        }
                        mostrarErrorTareaNoCargada("No se encontró la tarea en el proceso");
                    } else {
                        mostrarErrorTareaNoCargada("Error al buscar la tarea en el proceso");
                    }
                }
                
                @Override
                public void onFailure(Call<List<Tarea>> call, Throwable t) {
                    mostrarErrorTareaNoCargada("Error de conexión");
                }
            });
        } else {
            mostrarErrorTareaNoCargada("No se puede determinar el proceso de esta tarea");
        }
    }

    private void mostrarErrorTareaNoCargada(String mensaje) {
        Log.e(TAG, mensaje);

        tarea = new Tarea();
        tarea.setId(tareaId);
        tarea.setNombre("Tarea #" + tareaId);
        tarea.setDescripcion("No se pudieron cargar los detalles de esta tarea.");
        tarea.setEstado("Desconocido");
        
        mostrarDatosTarea(tarea);
    }

    private void mostrarDatosTarea(Tarea tarea) {
        // Mostrar datos de la tarea en la UI
        tvNombreTarea.setText(tarea.getNombre());
        tvDescripcionTarea.setText(tarea.getDescripcion());
        tvEstado.setText("Estado: " + tarea.getEstado());
        cbCompletada.setChecked("Completada".equals(tarea.getEstado()));
    }

    private void actualizarEstadoTarea(boolean isChecked) {

        Map<String, Object> tareaDatos = new HashMap<>();
        tareaDatos.put("estado", isChecked ? "Completada" : "Pendiente");

        if (isChecked) {
            tareaDatos.put("usuario_completado_id", usuarioId);
        }

        Call<Map<String, Object>> call = apiService.actualizarTarea(tareaId, tareaDatos);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    tarea.setEstado(isChecked ? "Completada" : "Pendiente");
                    tvEstado.setText("Estado: " + tarea.getEstado());

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("TAREA_ACTUALIZADA", true);
                    resultIntent.putExtra("TAREA_ID", tareaId);
                    resultIntent.putExtra("TAREA_ESTADO", isChecked ? "Completada" : "Pendiente");
                    setResult(RESULT_OK, resultIntent);
                } else {
                    cbCompletada.setChecked(!isChecked);

                    Log.e(TAG, "Error al actualizar tarea: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                cbCompletada.setChecked(!isChecked);

                Log.e(TAG, "Error de conexión: " + t.getMessage());
            }
        });
    }

    private void cargarComentarios(int tareaId) {
        Log.d(TAG, "Iniciando carga de comentarios para tarea ID: " + tareaId);
        
        if (comentariosRecyclerView == null) {
            Log.e(TAG, "RecyclerView de comentarios no inicializado");
            return;
        }
        
        Log.d(TAG, "Cargando comentarios para tarea ID: " + tareaId);
        
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, List<TareaComentario>>> call = apiService.obtenerComentariosTarea(tareaId);
        
        call.enqueue(new Callback<Map<String, List<TareaComentario>>>() {
            @Override
            public void onResponse(Call<Map<String, List<TareaComentario>>> call, Response<Map<String, List<TareaComentario>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TareaComentario> nuevosComentarios = response.body().get("comentarios");
                    
                    Log.d(TAG, "Respuesta recibida: " + (nuevosComentarios != null ? nuevosComentarios.size() : "null") + " comentarios");

                    if (nuevosComentarios != null && !nuevosComentarios.isEmpty()) {
                        comentarios.clear();
                        comentarios.addAll(nuevosComentarios);
                        comentariosAdapter = new TareaComentariosAdapter(
                            TareaDetalleActivity.this, 
                            comentarios,
                            tareaId
                        );
                        comentariosRecyclerView.setAdapter(comentariosAdapter);
                        

                        if (emptyCommentsView != null) emptyCommentsView.setVisibility(View.GONE);
                        comentariosRecyclerView.setVisibility(View.VISIBLE);
                        
                        Log.d(TAG, "Comentarios actualizados y visibles: " + comentarios.size());
                    } else {
                        Log.d(TAG, "Sin comentarios para mostrar");
                        comentarios.clear();
                        comentariosAdapter.notifyDataSetChanged();
                        if (emptyCommentsView != null) emptyCommentsView.setVisibility(View.VISIBLE);
                        comentariosRecyclerView.setVisibility(View.GONE);
                    }
                } else {
                    String errorMsg = "Error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer mensaje de error", e);
                    }
                    Log.e(TAG, "Error en respuesta: " + errorMsg);
                    Log.e(TAG, "Error al cargar comentarios: " + response.code());
                    if (emptyCommentsView != null) emptyCommentsView.setVisibility(View.VISIBLE);
                    comentariosRecyclerView.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, List<TareaComentario>>> call, Throwable t) {
                Log.e(TAG, "Error de red al cargar comentarios", t);
                Log.e(TAG, "Error de conexión: " + t.getMessage());
                if (emptyCommentsView != null) emptyCommentsView.setVisibility(View.VISIBLE);
                comentariosRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void enviarComentario(String contenido) {
        if (tareaId <= 0) {
            Log.e(TAG, "Error: ID de tarea inválido");
            return;
        }
        
        if (usuarioId <= 0) {

            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            usuarioId = sharedPreferences.getInt("USER_ID", -1);
            
            if (usuarioId <= 0) {
                Log.e(TAG, "Error: No se pudo identificar el usuario");
                return;
            }
        }

        Log.d(TAG, "Enviando comentario - tareaId: " + tareaId + ", usuarioId: " + usuarioId + ", contenido: " + contenido);

        TareaComentario comentario = new TareaComentario(tareaId, usuarioId, contenido);

        Call<TareaComentario> call = apiService.agregarComentarioTarea(tareaId, comentario);
        
        call.enqueue(new Callback<TareaComentario>() {
            @Override
            public void onResponse(Call<TareaComentario> call, Response<TareaComentario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Comentario agregado correctamente");
                    cargarComentarios(tareaId);
                } else {

                    String errorMsg = "Error " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    
                    Log.e(TAG, "Error response: " + errorMsg);
                    Log.e(TAG, "Error al agregar comentario: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<TareaComentario> call, Throwable t) {
                Log.e(TAG, "Failure sending comment", t);
                Log.e(TAG, "Error de conexión: " + t.getMessage());
            }
        });
    }

    private void cargarImagenes(int tareaId) {
        Log.d(TAG, "Iniciando carga de imágenes para tarea ID: " + tareaId);
        
        if (imagenesRecyclerView == null) {
            Log.e(TAG, "RecyclerView de imágenes no inicializado");
            return;
        }
        
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, List<TareaImagen>>> call = apiService.obtenerImagenesTarea(tareaId);
        
        call.enqueue(new Callback<Map<String, List<TareaImagen>>>() {
            @Override
            public void onResponse(Call<Map<String, List<TareaImagen>>> call, Response<Map<String, List<TareaImagen>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TareaImagen> nuevasImagenes = response.body().get("imagenes");
                    
                    Log.d(TAG, "Respuesta recibida: " + (nuevasImagenes != null ? nuevasImagenes.size() : "null") + " imágenes");

                    if (nuevasImagenes != null && !nuevasImagenes.isEmpty()) {
                        // Depuración para ver exactamente qué URLs llegan
                        for (TareaImagen img : nuevasImagenes) {
                            Log.d(TAG, "URL de imagen recibida: " + img.getRutaImagen());
                            // Verificar si es una ruta relativa o completa
                            if (!img.getRutaImagen().startsWith("http")) {
                                Log.d(TAG, "URL COMPLETA sería: " + ApiClient.BASE_URL + img.getRutaImagen());
                            }
                        }
                        
                        imagenes.clear();
                        imagenes.addAll(nuevasImagenes);
                        imagenesAdapter = new TareaImagenesAdapter(
                            TareaDetalleActivity.this, 
                            imagenes,
                            ApiClient.BASE_URL,
                            tareaId
                        );
                        
                        // Configurar visibilidad del botón eliminar según rol
                        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        String rol = sharedPreferences.getString("ROL_USUARIO", "");
                        boolean isAdminOrAreaManager = rol.equalsIgnoreCase("Admin") || rol.equalsIgnoreCase("Area Manager");
                        
                        // Mostrar botones de eliminar solo para roles Admin y Area Manager
                        imagenesAdapter.setMostrarBotonesEliminar(isAdminOrAreaManager);
                        
                        // Configurar listener para eliminar imágenes
                        imagenesAdapter.setOnDeleteClickListener(imagen -> {
                            // Mostrar diálogo de confirmación antes de eliminar
                            new AlertDialog.Builder(TareaDetalleActivity.this)
                                .setTitle("Eliminar imagen")
                                .setMessage("¿Está seguro que desea eliminar esta imagen?")
                                .setPositiveButton("Eliminar", (dialog, which) -> {
                                    // Llamar al método del adaptador para eliminar
                                    eliminarImagen(imagen.getId());
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                        });
                        
                        imagenesAdapter.setOnImageClickListener(TareaDetalleActivity.this::showFullscreenImage);
                        imagenesRecyclerView.setAdapter(imagenesAdapter);

                        if (emptyPhotosView != null) emptyPhotosView.setVisibility(View.GONE);
                        imagenesRecyclerView.setVisibility(View.VISIBLE);
                        
                        Log.d(TAG, "Imágenes actualizadas y visibles: " + imagenes.size());

                        // NUEVO: Habilitar checkbox para Staff/StoreManager si hay imágenes
                        habilitarCheckboxSiHayImagenes(nuevasImagenes.size() > 0);
                    } else {
                        Log.d(TAG, "Sin imágenes para mostrar");
                        imagenes.clear();
                        imagenesAdapter.notifyDataSetChanged();
                        if (emptyPhotosView != null) emptyPhotosView.setVisibility(View.VISIBLE);
                        imagenesRecyclerView.setVisibility(View.GONE);
                        
                        // NUEVO: Deshabilitar checkbox para Staff/StoreManager si no hay imágenes
                        habilitarCheckboxSiHayImagenes(false);
                    }
                } else {
                    String errorMsg = "Error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer mensaje de error", e);
                    }
                    Log.e(TAG, "Error en respuesta: " + errorMsg);
                    Log.e(TAG, "Error al cargar imágenes: " + response.code());
                    if (emptyPhotosView != null) emptyPhotosView.setVisibility(View.VISIBLE);
                    imagenesRecyclerView.setVisibility(View.GONE);
                    
                    // NUEVO: Deshabilitar checkbox para Staff/StoreManager si hay error
                    habilitarCheckboxSiHayImagenes(false);
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, List<TareaImagen>>> call, Throwable t) {
                Log.e(TAG, "Error de red al cargar imágenes", t);
                Log.e(TAG, "Error de conexión: " + t.getMessage());
                if (emptyPhotosView != null) emptyPhotosView.setVisibility(View.VISIBLE);
                imagenesRecyclerView.setVisibility(View.GONE);
                
                // NUEVO: Deshabilitar checkbox para Staff/StoreManager si hay error
                habilitarCheckboxSiHayImagenes(false);
            }
        });
    }

    // NUEVO: Método para habilitar/deshabilitar el checkbox según permisos y fotos
    private void habilitarCheckboxSiHayImagenes(boolean hayImagenes) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String rol = sharedPreferences.getString("ROL_USUARIO", "");
        
        boolean isAdminOrAreaManager = rol.equalsIgnoreCase("Admin") || rol.equalsIgnoreCase("Area Manager");
        boolean isStaffOrStoreManager = rol.equalsIgnoreCase("Staff") || rol.equalsIgnoreCase("StoreManager");
        
        // Admin/AreaManager siempre pueden marcar, Staff/StoreManager solo si hay imágenes
        boolean puedeMarcar = isAdminOrAreaManager || (isStaffOrStoreManager && hayImagenes);
        
        cbCompletada.setEnabled(puedeMarcar);
    }

    private boolean checkPermissions() {

        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {

        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.CAMERA},
            PERMISSION_REQUEST_CODE
        );
    }

    private void openCamera() {
        if (checkPermissions()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    photoFile = createImageFile();
                    
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "com.example.smooypr1.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_CAMERA);
                        Log.d(TAG, "Iniciando cámara con URI: " + photoURI);
                    } else {
                        Log.e(TAG, "Error al crear el archivo para la foto");
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Error al crear el archivo de imagen", ex);
                    Log.e(TAG, "Error al crear el archivo para la foto");
                }
            } else {
                Log.e(TAG, "No se encontró una aplicación de cámara");
            }
        } else {
            requestPermissions();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        photoFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        );
        
        Log.d(TAG, "Archivo de imagen creado: " + photoFile.getAbsolutePath());
        return photoFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {

            if (photoFile != null) {
                uploadPhoto(Uri.fromFile(photoFile));
            } else {
                Log.e(TAG, "Error: photoFile es null después de tomar la foto");
                Log.e(TAG, "Error al procesar la foto");
            }
        } 
        else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                displaySelectedImage();
                uploadPhoto(selectedImageUri);
            }
        }
        else if (requestCode == REQUEST_CREATE_AVISO) {
            if (resultCode == RESULT_OK) {
                // El aviso fue creado correctamente
                Toast.makeText(this, "Aviso creado correctamente", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displaySelectedImage() {
        photoInputLayout.setVisibility(View.VISIBLE);
        commentInputLayout.setVisibility(View.GONE);
        
        try {
            imgPhotoPreview.setImageURI(selectedImageUri);
            imgPhotoPreview.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando imagen", e);
        }
    }

    private void uploadPhoto(Uri imageUri) {
        if (tareaId <= 0 || usuarioId <= 0) {
            Log.e(TAG, "Error: ID de tarea o usuario inválido");
            return;
        }

        try {

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Subiendo imagen...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            String filePath = getPath(imageUri);
            if (filePath == null) {
                Log.e(TAG, "Error: No se pudo obtener la ruta del archivo desde URI: " + imageUri);
                File tempFile = createTempFileFromUri(imageUri);
                if (tempFile != null) {
                    filePath = tempFile.getAbsolutePath();
                } else {
                    Log.e(TAG, "Error: No se pudo procesar la imagen");
                    progressDialog.dismiss();
                    return;
                }
            }

            File file = new File(filePath);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("imagen", file.getName(), requestFile);
            RequestBody usuarioIdPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(usuarioId));

            Call<TareaImagen> call = apiService.subirImagenTarea(tareaId, usuarioIdPart, imagePart);
            
            Log.d(TAG, "Enviando imagen para tareaId=" + tareaId + ", usuarioId=" + usuarioId + ", ruta=" + filePath);

            call.enqueue(new Callback<TareaImagen>() {
                @Override
                public void onResponse(Call<TareaImagen> call, Response<TareaImagen> response) {
                    progressDialog.dismiss();
                    
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Imagen subida correctamente: " + response.body().getRutaImagen());

                        cargarImagenes(tareaId);

                        if (imgPhotoPreview != null) {
                            imgPhotoPreview.setVisibility(View.GONE);
                        }
                        
                        if (btnUploadPhoto != null) {
                            btnUploadPhoto.setVisibility(View.GONE);
                        }
                    } else {
                        String errorBody = "Unknown error";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        
                        Log.e(TAG, "Error subiendo imagen: " + response.code() + " - " + errorBody);
                        Log.e(TAG, "Error al subir imagen: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<TareaImagen> call, Throwable t) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error de conexión al subir imagen", t);
                    Log.e(TAG, "Error de conexión: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparando imagen para subir", e);
            Log.e(TAG, "Error preparando imagen: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                openCamera();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

                    Log.e(TAG, "Se necesita permiso de cámara para tomar fotos");
                } else {

                    new AlertDialog.Builder(this)
                        .setTitle("Permiso necesario")
                        .setMessage("Se requiere permiso de cámara para tomar fotos. Por favor habilítalo en la configuración.")
                        .setPositiveButton("Ir a Ajustes", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancelar", null)
                        .create()
                        .show();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Establecer resultado al volver con el botón de la barra de acciones
            Intent resultIntent = new Intent();
            resultIntent.putExtra("DATOS_ACTUALIZADOS", true);
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openCommentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_comment, null);
        EditText etComment = dialogView.findViewById(R.id.et_comment);
        
        builder.setView(dialogView)
               .setTitle("Agregar Comentario")
               .setPositiveButton("Guardar", (dialog, id) -> {
                   String comment = etComment.getText().toString().trim();
                   if (!comment.isEmpty()) {
                       enviarComentario(comment);
                   } else {
                       Log.e(TAG, "El comentario no puede estar vacío");
                   }
               })
               .setNegativeButton("Cancelar", (dialog, id) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getPath(Uri uri) {
        String filePath = null;
        try {
            if ("content".equals(uri.getScheme())) {
                // For content URIs
                String[] projection = { MediaStore.Images.Media.DATA };
                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(column_index);
                    }
                    cursor.close();
                }
            } else if ("file".equals(uri.getScheme())) {

                filePath = uri.getPath();
            }

            if (filePath == null) {
                File tempFile = createTempFileFromUri(uri);
                if (tempFile != null) {
                    filePath = tempFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file path from URI", e);
        }
        
        Log.d(TAG, "URI path resolved to: " + filePath);
        return filePath;
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            File tempFile = File.createTempFile("image", ".jpg", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[4 * 1024]; // 4k buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file from URI", e);
            return null;
        }
    }

    private void showFullscreenImage(String imageUrl) {
        Dialog fullscreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        fullscreenDialog.setContentView(R.layout.dialog_fullscreen_image);
        
        ImageView fullscreenImageView = fullscreenDialog.findViewById(R.id.fullscreenImageView);
        ProgressBar progressBar = fullscreenDialog.findViewById(R.id.progressBar);
        
        String fullImageUrl = imageUrl;

        if (!imageUrl.startsWith("http")) {
            if (ApiClient.BASE_URL.endsWith("/") && imageUrl.startsWith("/")) {
                fullImageUrl = ApiClient.BASE_URL + imageUrl.substring(1);
            } else if (!ApiClient.BASE_URL.endsWith("/") && !imageUrl.startsWith("/")) {
                fullImageUrl = ApiClient.BASE_URL + "/" + imageUrl;
            } else {
                fullImageUrl = ApiClient.BASE_URL + imageUrl;
            }
        }

        Glide.with(this)
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(fullscreenImageView);
        
        progressBar.setVisibility(View.GONE);

        fullscreenImageView.setOnClickListener(v -> fullscreenDialog.dismiss());
        
        fullscreenDialog.show();
    }

    /**
     * Método para crear un aviso (notificación) relacionado con la tarea actual
     */
    private void crearAvisoParaTarea() {
        if (tarea == null) {
            Toast.makeText(this, "Error: Tarea no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el establecimiento_id desde la tarea o proceso
        Call<Tarea> call = apiService.obtenerTareaPorId(tareaId);
        call.enqueue(new Callback<Tarea>() {
            @Override
            public void onResponse(Call<Tarea> call, Response<Tarea> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Tarea tareaActualizada = response.body();
                    
                    // Obtener el proceso para encontrar el establecimiento_id
                    Call<Map<String, Object>> procesoCall = apiService.obtenerProcesoTarea(tareaId);
                    procesoCall.enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                // Extraer el establecimiento_id de la respuesta
                                Map<String, Object> data = response.body();
                                Integer establecimientoId = null;
                                
                                if (data.containsKey("proceso")) {
                                    Map<String, Object> proceso = (Map<String, Object>) data.get("proceso");
                                    if (proceso.containsKey("establecimiento_id")) {
                                        Object value = proceso.get("establecimiento_id");
                                        if (value instanceof Integer) {
                                            establecimientoId = (Integer) value;
                                        } else if (value instanceof Double) {
                                            establecimientoId = ((Double) value).intValue();
                                        } else if (value instanceof String) {
                                            try {
                                                establecimientoId = Integer.parseInt((String) value);
                                            } catch (NumberFormatException e) {
                                                Log.e(TAG, "Error al convertir establecimiento_id", e);
                                            }
                                        }
                                    }
                                }
                                
                                if (establecimientoId != null) {
                                    // Ahora tenemos toda la información para crear el aviso
                                    abrirCrearAviso(tareaActualizada, establecimientoId);
                                } else {
                                    Toast.makeText(TareaDetalleActivity.this, 
                                        "No se pudo identificar el establecimiento de esta tarea", 
                                        Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(TareaDetalleActivity.this, 
                                    "Error al obtener datos del proceso", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            Toast.makeText(TareaDetalleActivity.this, 
                                "Error de conexión: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(TareaDetalleActivity.this, 
                        "Error al obtener detalles actualizados de la tarea", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Tarea> call, Throwable t) {
                Toast.makeText(TareaDetalleActivity.this, 
                    "Error de conexión: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirCrearAviso(Tarea tarea, int establecimientoId) {
        Intent intent = new Intent(this, com.smooy.smooypr1.avisos.AgregarAvisoActivity.class);

        // Pasamos información de la tarea para pre-cargar el formulario
        intent.putExtra("DESDE_TAREA", true);
        intent.putExtra("TAREA_ID", tarea.getId());
        intent.putExtra("TAREA_NOMBRE", tarea.getNombre());
        intent.putExtra("TAREA_DESCRIPCION", tarea.getDescripcion());
        intent.putExtra("ESTABLECIMIENTO_ID", establecimientoId);

        // Obtener el proceso para encontrar su estado y tipo
        Call<Map<String, Object>> procesoCall = apiService.obtenerProcesoTarea(tareaId);
        procesoCall.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    String tipoProceso = "";
                    String estadoProceso = "";
                    int procesoId = -1;

                    if (data.containsKey("proceso")) {
                        Map<String, Object> proceso = (Map<String, Object>) data.get("proceso");
                        if (proceso.containsKey("tipo_proceso")) {
                            tipoProceso = proceso.get("tipo_proceso").toString();
                        }
                        if (proceso.containsKey("estado")) {
                            estadoProceso = proceso.get("estado").toString();
                        }
                        if (proceso.containsKey("id")) {
                            Object idValue = proceso.get("id");
                            if (idValue instanceof Integer) {
                                procesoId = (Integer) idValue;
                            } else if (idValue instanceof Double) {
                                procesoId = ((Double) idValue).intValue();
                            } else if (idValue instanceof String) {
                                try {
                                    procesoId = Integer.parseInt((String) idValue);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error al convertir proceso_id", e);
                                }
                            }
                        }
                    }

                    // Pasar el ID del proceso para asociarlo con el aviso
                    intent.putExtra("PROCESO_ID", procesoId);

                    // Sugerimos un título para el aviso que incluya el ID del proceso
                    String tituloSugerido = "Revisión necesaria: " + tarea.getNombre() + " (ID: " + procesoId + ")";
                    intent.putExtra("AVISO_TITULO_SUGERIDO", tituloSugerido);

                    // Obtener la fecha/hora actual para indicar cuándo se verificó por última vez
                    String fechaVerificacion = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

                    // Sugerimos una descripción para el aviso que incluye el nombre de la tarea, la hora de verificación y el ID del proceso
                    String descripcionSugerida = "Problema en la tarea: " + tarea.getNombre() + " (verificado: " + fechaVerificacion + ")\n\n";
                    descripcionSugerida += "Detalles de la tarea:\n";
                    descripcionSugerida += "Estado del proceso: " + estadoProceso + "\n";
                    descripcionSugerida += "ID del proceso: " + procesoId + "\n";
                    descripcionSugerida += "Descripción: " + tarea.getDescripcion() + "\n\n";
                    descripcionSugerida += "Por favor, corrija los siguientes problemas: ";

                    intent.putExtra("AVISO_DESCRIPCION_SUGERIDA", descripcionSugerida);

                    // Solución muy específica para categorías
                    String categoriaSugerida;

                    // Primero, loguear el valor para diagnóstico
                    Log.d(TAG, "Tipo proceso original: '" + tipoProceso + "'");

                    // Simplemente asignar categoría directamente por tipo
                    if (tipoProceso == null || tipoProceso.isEmpty()) {
                        categoriaSugerida = "Trascurso de Jornada";
                    } else if (tipoProceso.toUpperCase().contains("APERTURA")) {
                        categoriaSugerida = "Apertura";
                    } else if (tipoProceso.toUpperCase().contains("CIERRE")) {
                        categoriaSugerida = "Cierre";
                    } else if (tipoProceso.toUpperCase().contains("SEMANAL")) {
                        categoriaSugerida = "Proceso Semanal";
                    } else if (tipoProceso.toUpperCase().contains("MENSUAL")) {
                        categoriaSugerida = "Proceso Mensual";
                    } else if (tipoProceso.toUpperCase().contains("JORNADA")) {
                            categoriaSugerida = "Trascurso de Jornada";
                    }
                    else categoriaSugerida="Trascurso de Jornada";

                    intent.putExtra("AVISO_CATEGORIA_SUGERIDA", categoriaSugerida);

                    Log.d(TAG, "Categoría final enviada: Transcurso de Jornada");

                    startActivityForResult(intent, REQUEST_CREATE_AVISO);

                } else {
                    intent.putExtra("PROCESO_ID", -1);

                    String tituloSugerido = "Revisión necesaria: " + tarea.getNombre();
                    intent.putExtra("AVISO_TITULO_SUGERIDO", tituloSugerido);

                    String fechaVerificacion = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    String descripcionSugerida = "Problema en la tarea: " + tarea.getNombre() + " (verificado: " + fechaVerificacion + ")\n\n";
                    descripcionSugerida += "Detalles de la tarea:\n";
                    descripcionSugerida += "Estado de la tarea: " + tarea.getEstado() + "\n";
                    descripcionSugerida += "Descripción: " + tarea.getDescripcion() + "\n\n";
                    descripcionSugerida += "Por favor, corrija los siguientes problemas: ";

                    intent.putExtra("AVISO_DESCRIPCION_SUGERIDA", descripcionSugerida);
                    intent.putExtra("AVISO_CATEGORIA_SUGERIDA", "Corrección de tarea");
                    startActivityForResult(intent, REQUEST_CREATE_AVISO);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {

                intent.putExtra("PROCESO_ID", -1);
                
                String tituloSugerido = "Revisión necesaria: " + tarea.getNombre();
                intent.putExtra("AVISO_TITULO_SUGERIDO", tituloSugerido);
                
                String fechaVerificacion = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                String descripcionSugerida = "Problema en la tarea: " + tarea.getNombre() + " (verificado: " + fechaVerificacion + ")\n\n";
                descripcionSugerida += "Detalles de la tarea:\n";
                descripcionSugerida += "Estado de la tarea: " + tarea.getEstado() + "\n";
                descripcionSugerida += "Descripción: " + tarea.getDescripcion() + "\n\n";
                descripcionSugerida += "Por favor, corrija los siguientes problemas: ";
                
                intent.putExtra("AVISO_DESCRIPCION_SUGERIDA", descripcionSugerida);
                intent.putExtra("AVISO_CATEGORIA_SUGERIDA", "Corrección de tarea");
                startActivityForResult(intent, REQUEST_CREATE_AVISO);
            }
        });
    }
    private void eliminarImagen(int imagenId) {
        Log.d(TAG, "Eliminando imagen con ID: " + imagenId);
        
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Eliminando imagen...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        Call<Map<String, Object>> call = apiService.eliminarImagenTarea(tareaId, imagenId);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful()) {
                    Toast.makeText(TareaDetalleActivity.this, "Imagen eliminada correctamente", Toast.LENGTH_SHORT).show();
                    // Recargar la lista de imágenes
                    cargarImagenes(tareaId);
                } else {
                    Toast.makeText(TareaDetalleActivity.this, "Error al eliminar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(TareaDetalleActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("DATOS_ACTUALIZADOS", true);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }
}