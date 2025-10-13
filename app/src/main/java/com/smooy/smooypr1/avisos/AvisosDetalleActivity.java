package com.smooy.smooypr1.avisos;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smooy.smooypr1.R;

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

public class AvisosDetalleActivity extends AppCompatActivity {

    private static final String TAG = "AvisosDetalleActivity";
    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private TextView tvTitulo, tvDescripcion, tvCategoria, tvEstablecimiento, tvUsuario, tvFecha, tvEstado;
    private Button btnVolver, btnAddPhoto, btnSelectPhoto, btnUploadPhoto, btnAgregarComentario, btnSubmitComment;
    private EditText etNewComment;
    private ImageView imgPhotoPreview;
    private RecyclerView photosList, comentariosRecyclerView;
    private CardView photoInputLayout, commentInputLayout;
    private TextView emptyPhotosView, emptyCommentsView;
    private FloatingActionButton fabAddContent;
    private AvisoImagenesAdapter imagenesAdapter;
    private AvisoComentariosAdapter comentariosAdapter;
    private Aviso aviso;
    private int avisoId;
    private int usuarioId;
    private Uri selectedImageUri;
    private File photoFile;
    private final List<AvisoImagen> imagenes = new ArrayList<>();
    private final List<AvisoComentario> comentarios = new ArrayList<>();
    private boolean esStaff = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aviso_detalle);

        initViews();

        Intent intent = getIntent();
        if (intent != null) {
            avisoId = intent.getIntExtra("AVISO_ID", -1);
            if (avisoId == -1) {
                Toast.makeText(this, "Error: No se pudo obtener el ID del aviso", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (intent.hasExtra("AVISO")) {
                aviso = (Aviso) intent.getSerializableExtra("AVISO");
                if (aviso != null) {
                    mostrarDatosAviso(aviso);
                }
            }
        } else {
            Toast.makeText(this, "Error: No se pudieron recibir los datos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        usuarioId = sharedPreferences.getInt("USER_ID", -1);

        String rol = sharedPreferences.getString("ROL_USUARIO", "");
        esStaff = rol.equalsIgnoreCase("Staff") || rol.equalsIgnoreCase("Admin");

        FloatingActionButton fabAddContent = findViewById(R.id.fabAddContent);
        if (fabAddContent != null) {
            fabAddContent.setVisibility(View.GONE);
        }

        configureViewsByRole(esStaff);

        setupListeners();

        if (aviso == null) {
            cargarDatosAviso(avisoId);
        }

        // Cargar imágenes del aviso
        cargarImagenes(avisoId);
        
        // Cargar comentarios del aviso
        cargarComentarios();
    }

    private void initViews() {
        // TextViews para información del aviso
        tvTitulo = findViewById(R.id.tvTitulo);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvCategoria = findViewById(R.id.tvCategoria);
        tvEstablecimiento = findViewById(R.id.tvEstablecimiento);
        tvUsuario = findViewById(R.id.tvUsuario);
        tvFecha = findViewById(R.id.tvFecha);
        tvEstado = findViewById(R.id.tvEstado);

        // Botones
        btnVolver = findViewById(R.id.btnVolver);
        btnAddPhoto = findViewById(R.id.btnAgregarFoto); // Cambiado
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto); // Este botón no existe en el XML
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnAgregarComentario = findViewById(R.id.btnAgregarComentario);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);

        // EditText para nuevo comentario
        etNewComment = findViewById(R.id.etNewComment);

        // Vista previa de imagen
        imgPhotoPreview = findViewById(R.id.imgPhotoPreview);

        // RecyclerView para fotos
        photosList = findViewById(R.id.photosList);
        photosList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // RecyclerView para comentarios
        comentariosRecyclerView = findViewById(R.id.comentariosRecyclerView);
        comentariosRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Layout para input de foto
        photoInputLayout = findViewById(R.id.photoInputLayout);

        // Layout para input de comentario
        commentInputLayout = findViewById(R.id.commentInputLayout);

        // Vista para cuando no hay fotos
        emptyPhotosView = findViewById(R.id.emptyPhotosView);

        // Vista para cuando no hay comentarios
        emptyCommentsView = findViewById(R.id.emptyCommentsView);

        // Botón flotante
        fabAddContent = findViewById(R.id.fabAddContent);

        // Inicializar el adaptador de imágenes
        imagenesAdapter = new AvisoImagenesAdapter(this, imagenes, ApiClient.BASE_URL);

        // Inicializar el adaptador de comentarios
        comentariosAdapter = new AvisoComentariosAdapter(this, comentarios, avisoId);

        // Set click listener for images
        imagenesAdapter.setOnImageClickListener(this::showFullscreenImage);

        // Set delete click listener
        imagenesAdapter.setOnDeleteClickListener(this::handleImageDelete);

        photosList.setAdapter(imagenesAdapter);
        comentariosRecyclerView.setAdapter(comentariosAdapter);
    }

    private void configureViewsByRole(boolean isStaff) {
        // Mantener el botón flotante "+" oculto
        if (fabAddContent != null) {
            fabAddContent.setVisibility(View.GONE);
        }

        // Mostrar el botón "Añadir imagen", independientemente del rol
        if (btnAddPhoto != null) {
            btnAddPhoto.setVisibility(View.VISIBLE);
        }

        // Asegurarse de que los layouts de entrada estén ocultos inicialmente
        if (photoInputLayout != null) {
            photoInputLayout.setVisibility(View.GONE);
        }

        // Activar botones de eliminación si el usuario es staff o admin
        imagenesAdapter.setMostrarBotonesEliminar(isStaff);
        comentariosAdapter.setMostrarBotonesBorrar(isStaff);
    }

    private void setupListeners() {
        // Botón volver
        btnVolver.setOnClickListener(v -> finish());

        // Botón para añadir foto
        btnAddPhoto.setOnClickListener(v -> {
            photoInputLayout.setVisibility(View.VISIBLE);
            btnAddPhoto.setVisibility(View.GONE);
        });

        btnAgregarComentario.setOnClickListener(v -> {
            commentInputLayout.setVisibility(View.VISIBLE);
            btnAgregarComentario.setVisibility(View.GONE);
            etNewComment.requestFocus();
        });

        btnSubmitComment.setOnClickListener(v -> {
            String comentario = etNewComment.getText().toString().trim();
            if (!comentario.isEmpty()) {
                enviarComentario(comentario);
                commentInputLayout.setVisibility(View.GONE);
                btnAgregarComentario.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Por favor, escribe un comentario", Toast.LENGTH_SHORT).show();
            }
        });

        btnSelectPhoto.setOnClickListener(v -> {
            if (checkPermissions()) {
                openCamera();
            } else {
                requestPermissions();
            }
        });

        btnUploadPhoto.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadPhoto(selectedImageUri);
            } else {
                Toast.makeText(this, "No hay imagen seleccionada", Toast.LENGTH_SHORT).show();
            }
        });

        fabAddContent.setOnClickListener(v -> showAddContentDialog());
    }

    private void handleImageDelete(AvisoImagen imagen) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar imagen")
                .setMessage("¿Estás seguro de que deseas eliminar esta imagen?")
                .setPositiveButton("Sí", (dialog, which) -> {

                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage("Eliminando imagen...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    deleteImage(imagen.getId(), progressDialog);
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Método para eliminar la imagen del servidor
    private void deleteImage(int imagenId, ProgressDialog progressDialog) {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.eliminarImagenAviso(avisoId, imagenId);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {

                    imagenesAdapter.eliminarImagen(imagenId);
                    Toast.makeText(AvisosDetalleActivity.this, "Imagen eliminada correctamente", Toast.LENGTH_SHORT).show();

                    actualizarVistaVacia();
                } else {
                    Log.e(TAG, "Error al eliminar imagen: " + response.code());
                    Toast.makeText(AvisosDetalleActivity.this, "Error al eliminar la imagen: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Fallo en la conexión al eliminar imagen", t);
                Toast.makeText(AvisosDetalleActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para actualizar la vista vacía
    public void actualizarVistaVacia() {
        if (imagenes.isEmpty()) {
            emptyPhotosView.setVisibility(View.VISIBLE);
            photosList.setVisibility(View.GONE);
        } else {
            emptyPhotosView.setVisibility(View.GONE);
            photosList.setVisibility(View.VISIBLE);
        }
    }

    private void cargarDatosAviso(int avisoId) {
        ApiService apiService = ApiClient.getApiService();
        Call<Aviso> call = apiService.obtenerAvisoPorId(avisoId);

        call.enqueue(new Callback<Aviso>() {
            @Override
            public void onResponse(Call<Aviso> call, Response<Aviso> response) {
                if (response.isSuccessful() && response.body() != null) {
                    aviso = response.body();
                    mostrarDatosAviso(aviso);
                } else {
                    Toast.makeText(AvisosDetalleActivity.this,
                            "Error al obtener datos del aviso: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Aviso> call, Throwable t) {
                Toast.makeText(AvisosDetalleActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error de conexión", t);
            }
        });
    }

    private void mostrarDatosAviso(Aviso aviso) {
        tvTitulo.setText(aviso.getNombre());
        tvDescripcion.setText(aviso.getDescripcion());
        tvCategoria.setText("Categoría: " + aviso.getCategoria());

        if (aviso.getEstablecimientoNombre() != null) {
            tvEstablecimiento.setText("Establecimiento: " + aviso.getEstablecimientoNombre());
        } else {
            tvEstablecimiento.setText("Establecimiento: #" + aviso.getEstablecimientoId());
        }

        if (aviso.getUsuarioNombre() != null) {
            tvUsuario.setText("Creado por: " + aviso.getUsuarioNombre());
        } else {
            tvUsuario.setText("Usuario: #" + aviso.getUsuarioId());
        }

        tvFecha.setText("Fecha: " + aviso.getFechaCreacion());

        // Mostrar estado con color adecuado
        String estado = aviso.getEstado() != null ? aviso.getEstado() : "Pendiente";
        tvEstado.setText(estado);

        // Cambiar color según el estado
        int colorResId;
        switch (estado.toLowerCase()) {
            case "completado":
                colorResId = android.R.color.holo_green_light;
                break;
            case "en proceso":
                colorResId = android.R.color.holo_blue_light;
                break;
            case "verificado":
                colorResId = android.R.color.holo_green_dark;
                break;
            case "rechazado":
                colorResId = android.R.color.holo_red_light;
                break;
            default: // Pendiente u otro
                colorResId = android.R.color.holo_orange_light;
                break;
        }

        tvEstado.setBackgroundColor(ContextCompat.getColor(this, colorResId));
    }

    private void cargarImagenes(int avisoId) {
        ApiService apiService = ApiClient.getApiService();
        Call<List<AvisoImagen>> call = apiService.obtenerImagenesAviso(avisoId);

        call.enqueue(new Callback<List<AvisoImagen>>() {
            @Override
            public void onResponse(Call<List<AvisoImagen>> call, Response<List<AvisoImagen>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    imagenes.clear();
                    imagenes.addAll(response.body());
                    imagenesAdapter.actualizarImagenes(response.body());

                    // Actualizar visibilidad de la vista vacía
                    actualizarVistaVacia();
                } else {
                    Log.e(TAG, "Error al cargar imágenes: " + response.code());
                    emptyPhotosView.setVisibility(View.VISIBLE);
                    photosList.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<AvisoImagen>> call, Throwable t) {
                Log.e(TAG, "Error de conexión al cargar imágenes", t);
                emptyPhotosView.setVisibility(View.VISIBLE);
                photosList.setVisibility(View.GONE);
            }
        });
    }

    // Reemplazar el método checkPermissions() actual con este:
    private boolean checkPermissions() {
        // Como ahora pedimos los permisos al inicio de la app, simplemente verificamos si están concedidos
        boolean cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        // Para Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean mediaPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            return cameraPermission && mediaPermission;
        }
        // Para Android 10-12 (API 29-32)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            return cameraPermission && readPermission;
        }
        // Para Android 9 y anteriores (API 28-)
        else {
            boolean readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            return cameraPermission && readPermission && writePermission;
        }
    }

    // Modificar el método requestPermissions() para mostrar un diálogo informativo
    private void requestPermissions() {
        new AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage("Para usar esta función, se necesitan permisos de cámara y almacenamiento. Por favor, regresa a la pantalla principal e inicia de nuevo la aplicación.")
                .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Reemplaza el método onRequestPermissionsResult actual con este:
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "✅ Todos los permisos concedidos");
            } else {
                Log.d(TAG, "❌ Algunos permisos denegados");
                boolean showRationale = false;

                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        showRationale = true;
                        break;
                    }
                }

                if (showRationale) {
                    // El usuario rechazó permisos, pero podemos explicarle por qué los necesitamos
                    Toast.makeText(this, "Los permisos son necesarios para usar la cámara y acceder a fotos", Toast.LENGTH_LONG).show();
                } else {
                    // El usuario seleccionó "No volver a preguntar", debemos dirigirlo a ajustes
                    Toast.makeText(this, "Por favor, habilita los permisos en la configuración de la aplicación", Toast.LENGTH_LONG).show();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Permisos requeridos")
                            .setMessage("Para usar esta función, necesitamos acceso a la cámara y a tus fotos. Por favor, habilita estos permisos en la configuración.")
                            .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            }
        }
    }


    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void openCamera() {
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
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error al crear archivo para foto", ex);
                Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                selectedImageUri = data.getData();
                displaySelectedImage();
            } else if (requestCode == REQUEST_CAMERA) {
                if (photoFile != null) {
                    selectedImageUri = Uri.fromFile(photoFile);
                    displaySelectedImage();
                }
            }
        }
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            try {
                imgPhotoPreview.setImageURI(selectedImageUri);
                imgPhotoPreview.setVisibility(View.VISIBLE);
                btnUploadPhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error al mostrar la imagen", e);
                Toast.makeText(this, "Error al mostrar la imagen seleccionada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadPhoto(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "No hay imagen seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Subiendo imagen...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                File file = createFileFromInputStream(inputStream);

                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part imagenPart = MultipartBody.Part.createFormData("imagen", file.getName(), requestFile);
                RequestBody usuarioIdPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(usuarioId));

                ApiService apiService = ApiClient.getApiService();
                Call<AvisoImagen> call = apiService.subirImagenAviso(avisoId, usuarioIdPart, imagenPart);

                call.enqueue(new Callback<AvisoImagen>() {
                    @Override
                    public void onResponse(Call<AvisoImagen> call, Response<AvisoImagen> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(AvisosDetalleActivity.this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show();

                            // Resetear la vista previa
                            imgPhotoPreview.setVisibility(View.GONE);
                            btnUploadPhoto.setVisibility(View.GONE);
                            photoInputLayout.setVisibility(View.GONE);
                            btnAddPhoto.setVisibility(View.VISIBLE);

                            // Actualizar la lista de imágenes
                            cargarImagenes(avisoId);

                            // Limpiar la selección
                            selectedImageUri = null;
                        } else {
                            Log.e(TAG, "Error al subir imagen: " + response.code());
                            Toast.makeText(AvisosDetalleActivity.this, "Error al subir la imagen: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AvisoImagen> call, Throwable t) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Fallo al subir imagen", t);
                        Toast.makeText(AvisosDetalleActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error al procesar la imagen", e);
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private File createFileFromInputStream(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("image", ".jpg", getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.close();
        inputStream.close();
        return tempFile;
    }

    private void showAddContentDialog() {
        // Crear un diálogo con opciones
        String[] options = {"Añadir Imagen", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Añadir Contenido")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Añadir Imagen
                        photoInputLayout.setVisibility(View.VISIBLE);
                        btnAddPhoto.setVisibility(View.GONE);
                    }
                });

        builder.create().show();
    }

    private void showFullscreenImage(String imageUrl) {
        Dialog fullscreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        fullscreenDialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullscreenImageView = fullscreenDialog.findViewById(R.id.fullscreenImageView);
        ProgressBar progressBar = fullscreenDialog.findViewById(R.id.progressBar);

        String fullImageUrl = imageUrl;

        // Check if URL needs base URL prefixed
        if (!imageUrl.startsWith("http")) {
            if (ApiClient.BASE_URL.endsWith("/") && imageUrl.startsWith("/")) {
                fullImageUrl = ApiClient.BASE_URL + imageUrl.substring(1);
            } else if (!ApiClient.BASE_URL.endsWith("/") && !imageUrl.startsWith("/")) {
                fullImageUrl = ApiClient.BASE_URL + "/" + imageUrl;
            } else {
                fullImageUrl = ApiClient.BASE_URL + imageUrl;
            }
        }

        // Use Glide to load the image
        Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(fullscreenImageView);

        progressBar.setVisibility(View.GONE);

        // Close on tap
        fullscreenImageView.setOnClickListener(v -> fullscreenDialog.dismiss());

        fullscreenDialog.show();
        }
    
    private void cargarComentarios() {
        ApiService apiService = ApiClient.getApiService();
        Call<List<AvisoComentario>> call = apiService.obtenerComentariosAviso(avisoId);
        
        call.enqueue(new Callback<List<AvisoComentario>>() {
            @Override
            public void onResponse(Call<List<AvisoComentario>> call, Response<List<AvisoComentario>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    comentarios.clear();
                    comentarios.addAll(response.body());
                    comentariosAdapter.notifyDataSetChanged();
                    
                    // Mostrar mensaje si no hay comentarios
                    if (comentarios.isEmpty()) {
                        emptyCommentsView.setVisibility(View.VISIBLE);
                        comentariosRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyCommentsView.setVisibility(View.GONE);
                        comentariosRecyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e(TAG, "Error al cargar comentarios: " + response.code());
                    emptyCommentsView.setVisibility(View.VISIBLE);
                    comentariosRecyclerView.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onFailure(Call<List<AvisoComentario>> call, Throwable t) {
                Log.e(TAG, "Error de conexión al cargar comentarios", t);
                emptyCommentsView.setVisibility(View.VISIBLE);
                comentariosRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void enviarComentario(String texto) {
        // Mostrar progreso
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando comentario...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Preparar datos
        Map<String, Object> comentarioData = new HashMap<>();
        comentarioData.put("usuarioId", usuarioId);
        comentarioData.put("comentario", texto);
        
        // Llamar a la API - Usar el nuevo método que incluye el avisoId como parámetro de ruta
        ApiService apiService = ApiClient.getApiService();
        Call<AvisoComentario> call = apiService.agregarComentarioAviso(avisoId, comentarioData);
        
        call.enqueue(new Callback<AvisoComentario>() {
            @Override
            public void onResponse(Call<AvisoComentario> call, Response<AvisoComentario> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    // Limpiar campo de texto
                    etNewComment.setText("");
                    
                    // Añadir nuevo comentario a la lista
                    comentarios.add(0, response.body());
                    comentariosAdapter.notifyItemInserted(0);
                    
                    // Actualizar visibilidad
                    emptyCommentsView.setVisibility(View.GONE);
                    comentariosRecyclerView.setVisibility(View.VISIBLE);
                    
                    // Hacer scroll al primer comentario
                    comentariosRecyclerView.smoothScrollToPosition(0);
                    
                    Toast.makeText(AvisosDetalleActivity.this, "Comentario añadido", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Error al añadir comentario: " + response.code());
                    Toast.makeText(AvisosDetalleActivity.this, "Error al enviar el comentario", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<AvisoComentario> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Error de conexión al enviar comentario", t);
                Toast.makeText(AvisosDetalleActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para actualizar la visibilidad de la vista de comentarios vacíos
    public void actualizarVistaComentariosVacia() {
        if (comentarios.isEmpty()) {
            emptyCommentsView.setVisibility(View.VISIBLE);
            comentariosRecyclerView.setVisibility(View.GONE);
        } else {
            emptyCommentsView.setVisibility(View.GONE);
            comentariosRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}