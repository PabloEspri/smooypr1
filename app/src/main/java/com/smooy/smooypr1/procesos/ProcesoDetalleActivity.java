package com.smooy.smooypr1.procesos;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;

import com.smooy.smooypr1.R;
import com.smooy.smooypr1.tareas.TareasProcesoActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.Gson;

public class ProcesoDetalleActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String TAG = "ProcesoDetalleActivity";
    private static final int REQUEST_CAMERA = 101;
    private File photoFile;

    private TextView tvTitulo, tvEstado, tvDescripcionBreve, tvFrecuencia,
            tvHorario, tvFechaInicio, tvFechaFin;
    private Button btnVolver, btnSubmitComment;
    private EditText etNewComment;
    private RecyclerView commentsList;
    private LinearLayout commentInputLayout, photoInputLayout;

    private ComentariosAdapter comentariosAdapter;

    private Proceso proceso;
    private int procesoId;
    private int usuarioId;
    private final List<ProcesoComentario> comentarios = new ArrayList<>();

    private TextView tvDescripcion;
    private View emptyCommentsView;

    private String rolUsuario = "";
    private String tipoProceso = "";

    private Button btnVerTareas;

    private FloatingActionButton fabAddContent, fabAgregarComentario;
    private TextView tvLabelComentario;
    private boolean isFabOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proceso_detalle);

        initViews();

        Intent intent = getIntent();

        Log.d("ProcesoDetalle", "Intent recibido: " + (intent != null));
        if (intent != null && intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                Log.d("ProcesoDetalle", "Extra clave=" + key + ", valor=" + intent.getExtras().get(key));
            }
        }

        if (intent != null) {

            procesoId = intent.getIntExtra("PROCESO_ID", -1);
            if (procesoId == -1) {
                procesoId = intent.getIntExtra("ID", -1);
            }

            usuarioId = intent.getIntExtra("USUARIO_ID", -1);
            if (usuarioId == -1) {
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                usuarioId = sharedPreferences.getInt("USER_ID", -1);
            }

            boolean esStaff = intent.getBooleanExtra("ES_STAFF", false);

            Log.d("ProcesoDetalle", "procesoId=" + procesoId + ", usuarioId=" + usuarioId + ", esStaff=" + esStaff);

            if (fabAddContent != null) {
                fabAddContent.setVisibility(View.VISIBLE);
                Log.d("ProcesoDetalle", "FAB visibilidad forzada a VISIBLE");
            } else {
                Log.e("ProcesoDetalle", "ERROR: fabAddContent es NULL");
            }

            configureViewsByRole(esStaff ? "staff" : "admin");

            if (procesoId <= 0) {
                procesoId = 3;
                Log.w("ProcesoDetalle", "Usando ID de proceso hardcodeado: " + procesoId);
            }

            if (procesoId > 0) {
                cargarDatosProceso(procesoId);
                cargarComentarios(procesoId);
                cargarImagenes(procesoId);
            } else {
                Toast.makeText(this, "Error: ID de proceso no válido", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Error: No se recibieron datos", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupListeners();
    }

    private void initViews() {

        tvTitulo = findViewById(R.id.tvTitulo);
        tvTitulo.setTextColor(Color.WHITE);
        tvEstado = findViewById(R.id.tvEstado);
        tvEstado.setTextColor(Color.WHITE);
        tvDescripcionBreve = findViewById(R.id.tvDescripcionBreve);
        tvDescripcionBreve.setTextColor(Color.WHITE);
        tvFrecuencia = findViewById(R.id.tvFrecuencia);
        tvFrecuencia.setTextColor(Color.WHITE);
        tvHorario = findViewById(R.id.tvHorario);
        tvHorario.setTextColor(Color.WHITE);
        tvFechaInicio = findViewById(R.id.tvFechaInicio);
        tvFechaInicio.setTextColor(Color.WHITE);
        tvFechaFin = findViewById(R.id.tvFechaFin);
        tvFechaFin.setTextColor(Color.WHITE);

        btnVolver = findViewById(R.id.volver);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);

        etNewComment = findViewById(R.id.etNewComment);

        commentsList = findViewById(R.id.commentsList);

        commentInputLayout = findViewById(R.id.commentInputLayout);
        photoInputLayout = findViewById(R.id.photoInputLayout);

        fabAddContent = findViewById(R.id.fabAddContent);

        commentsList.setLayoutManager(new LinearLayoutManager(this));

        comentariosAdapter = new ComentariosAdapter(ProcesoDetalleActivity.this, comentarios, true, procesoId); // Añadir el tercer parámetro: true para admin/area manager
        commentsList.setAdapter(comentariosAdapter);

        emptyCommentsView = findViewById(R.id.emptyCommentsView);
        tvDescripcion = tvDescripcionBreve;

        fabAddContent = findViewById(R.id.fabAddContent);

        if (fabAddContent != null) {
            fabAddContent.setVisibility(View.VISIBLE);
        } else {
            Log.e("ProcesoDetalle", "ERROR: fabAddContent es NULL");
        }

        btnVerTareas = findViewById(R.id.btnVerTareas);
        btnVerTareas.setOnClickListener(v -> verTareasProceso());

        fabAgregarComentario = findViewById(R.id.fabAgregarComentario);


        fabAgregarComentario.setOnClickListener(v -> {
            photoInputLayout.setVisibility(View.GONE);
            commentInputLayout.setVisibility(View.VISIBLE);
            etNewComment.requestFocus();
        });

        fabAddContent = findViewById(R.id.fabAddContent);
        fabAgregarComentario = findViewById(R.id.fabAgregarComentario);

        tvLabelComentario = findViewById(R.id.tvLabelComentario);
    }

    private void configureViewsByRole(String role) {
        rolUsuario = role;

        boolean accesoSinRestricciones = "Admin".equalsIgnoreCase(role) || 
                                        "Area Manager".equalsIgnoreCase(role);

        if (role.equalsIgnoreCase("Admin") || role.equalsIgnoreCase("Staff")) {
            btnVerTareas.setVisibility(View.VISIBLE);
        } else {
            btnVerTareas.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {

        fabAddContent.setOnClickListener(v -> {

            commentInputLayout.setVisibility(View.VISIBLE);
            photoInputLayout.setVisibility(View.GONE);
            etNewComment.requestFocus();
        });

        fabAgregarComentario.setOnClickListener(v -> {
            toggleFabMenu();
            photoInputLayout.setVisibility(View.GONE);
            commentInputLayout.setVisibility(View.VISIBLE);
            etNewComment.requestFocus();
        });

        btnVolver.setOnClickListener(v -> finish());

        btnSubmitComment.setOnClickListener(v -> {
            String commentText = etNewComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                enviarComentario(commentText);
            }
        });
    }

    private void cargarDatosProceso(int procesoId) {
        Log.d("API_DEBUG", "Iniciando carga de datos para proceso ID: " + procesoId);

        ApiService apiService = ApiClient.getApiService();
        Call<Proceso> call = apiService.obtenerProcesoPorId(procesoId);

        call.enqueue(new Callback<Proceso>() {
            @Override
            public void onResponse(Call<Proceso> call, Response<Proceso> response) {
                Log.d("API_DEBUG", "Respuesta recibida, código: " + response.code());

                if (response.isSuccessful()) {
                    proceso = response.body();

                    if (proceso != null) {
                        Log.d("API_DEBUG", "Datos recibidos correctamente para proceso ID: " + procesoId);

                        debugProceso(proceso);
                        mostrarDatosProceso(proceso);
                    } else {
                        Log.e("API_ERROR", "Cuerpo de respuesta nulo");
                        tvTitulo.setText("Error al cargar datos");
                        tvDescripcionBreve.setText("No se pudo obtener la información del proceso");
                        Toast.makeText(ProcesoDetalleActivity.this, "Error: Datos del proceso no disponibles", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e("API_ERROR", "Error en respuesta: " + errorBody);
                    } catch (IOException e) {
                        Log.e("API_ERROR", "Error al leer errorBody", e);
                    }

                    tvTitulo.setText("Error al cargar datos");
                    tvDescripcionBreve.setText("No se pudo obtener la información del proceso");
                    Toast.makeText(ProcesoDetalleActivity.this,
                            "Error al cargar los detalles del proceso (Código: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Proceso> call, Throwable t) {
                Log.e("API_ERROR", "Fallo en la llamada API", t);
                tvTitulo.setText("Error de conexión");
                tvDescripcionBreve.setText("No se pudo conectar al servidor");
                Toast.makeText(ProcesoDetalleActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void mostrarDatosProceso(Proceso proceso) {
        if (proceso != null) {
            tipoProceso = proceso.getTipo_proceso();

            tvTitulo.setText(proceso.getTipo_proceso());
            tvDescripcion.setText(proceso.getDescripcion());
            tvFrecuencia.setText(proceso.getFrecuencia());
            tvHorario.setText(proceso.getHorario());
            tvFechaInicio.setText(proceso.getFecha_inicio());
            tvFechaFin.setText(proceso.getFecha_fin());
            tvEstado.setText(proceso.getEstado());

            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");
            configureViewsByRole(rolUsuario);
        } else {

            tvTitulo.setText("ERROR");
            tvDescripcionBreve.setText("No se pudo cargar la información");
        }
    }

    private void cargarComentarios(int procesoId) {
        ApiService apiService = ApiClient.getApiService();
        Call<ComentariosResponse> call = apiService.obtenerComentariosProceso(procesoId);

        call.enqueue(new Callback<ComentariosResponse>() {
            @Override
            public void onResponse(Call<ComentariosResponse> call, Response<ComentariosResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProcesoComentario> comentarios = response.body().getComentarios();
                    if (comentarios != null && !comentarios.isEmpty()) {
                        emptyCommentsView.setVisibility(View.GONE);
                        comentariosAdapter = new ComentariosAdapter(ProcesoDetalleActivity.this, comentarios, true, procesoId); // Añadir el tercer parámetro
                        commentsList.setAdapter(comentariosAdapter);
                    } else {
                        emptyCommentsView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e("API_ERROR", "Error al cargar comentarios: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e("API_ERROR", "Error body: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ProcesoDetalleActivity.this, "Error al cargar comentarios: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ComentariosResponse> call, Throwable t) {
                Log.e("API_ERROR", "Fallo al cargar comentarios", t);
                Toast.makeText(ProcesoDetalleActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarImagenes(int procesoId) {
        ApiService apiService = ApiClient.getApiService();
        Call<List<ProcesoImagen>> call = apiService.obtenerImagenesProceso(procesoId);

        Log.d("ProcesoDetalle", "Solicitando imágenes para el proceso: " + procesoId);
        Log.d("ProcesoDetalle", "URL completa: " + call.request().url());

        call.enqueue(new Callback<List<ProcesoImagen>>() {
            @Override
            public void onResponse(Call<List<ProcesoImagen>> call, Response<List<ProcesoImagen>> response) {
                Log.d("ProcesoDetalle", "Respuesta recibida. Status: " + response.code());

                if (response.isSuccessful() && response.body() != null) {

                    String jsonResponse = new Gson().toJson(response.body());
                    Log.d("ProcesoDetalle", "Respuesta JSON: " + jsonResponse);

                    List<ProcesoImagen> imagenes = response.body();
                    if (imagenes != null && !imagenes.isEmpty()) {
                        Log.d("ProcesoDetalle", "Imágenes recibidas: " + imagenes.size());

                        TextView emptyView = findViewById(R.id.emptyPhotosView);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.GONE);
                        }

                        RecyclerView recyclerView = findViewById(R.id.photosList);
                        if (recyclerView != null) {
                            recyclerView.setVisibility(View.VISIBLE);

                            if (recyclerView.getLayoutManager() == null) {
                                recyclerView.setLayoutManager(new LinearLayoutManager(
                                        ProcesoDetalleActivity.this,
                                        LinearLayoutManager.HORIZONTAL,
                                        false));
                            }
                        }
                    } else {
                        Log.d("ProcesoDetalle", "No hay imágenes para mostrar");

                        TextView emptyView = findViewById(R.id.emptyPhotosView);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("No hay fotos disponibles");
                        }

                        RecyclerView recyclerView = findViewById(R.id.photosList);
                        if (recyclerView != null) {
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
                } else {
                    Log.e("ProcesoDetalle", "Error en la respuesta: " + response.code());

                    String errorBody = "No disponible";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                            Log.e("ProcesoDetalle", "Error body: " + errorBody);
                        }
                    } catch (IOException e) {
                        Log.e("ProcesoDetalle", "Error al leer errorBody", e);
                    }

                    TextView emptyView = findViewById(R.id.emptyPhotosView);
                    if (emptyView != null) {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("Error " + response.code() + ": " + errorBody);
                    }

                    new AlertDialog.Builder(ProcesoDetalleActivity.this)
                            .setTitle("Error al cargar imágenes")
                            .setMessage("Código: " + response.code() + "\n" +
                                    "URL: " + call.request().url() + "\n" +
                                    "Respuesta: " + errorBody)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<List<ProcesoImagen>> call, Throwable t) {
                Log.e("ProcesoDetalle", "Fallo en la llamada a la API", t);
                TextView emptyView = findViewById(R.id.emptyPhotosView);
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("Error de conexión: " + t.getMessage());
                }

                new AlertDialog.Builder(ProcesoDetalleActivity.this)
                        .setTitle("Error de conexión")
                        .setMessage("URL: " + call.request().url() + "\n\n" +
                                "Error: " + t)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void enviarComentario(String contenido) {
        Log.d("ProcesoDetalle", "Enviando comentario: " + contenido);
        
        if (usuarioId == -1 || procesoId == -1) {
            Log.e("ProcesoDetalle", "Error: ID de usuario (" + usuarioId + ") o proceso (" + procesoId + ") no válidos");
            Toast.makeText(this, "Error: datos de usuario o proceso no válidos", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando comentario...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ProcesoComentario nuevoComentario = new ProcesoComentario(procesoId, usuarioId, contenido);
        
        Log.d("ProcesoDetalle", "Datos del comentario - procesoId: " + procesoId + ", usuarioId: " + usuarioId);

        ApiService apiService = ApiClient.getApiService();
        Call<ProcesoComentario> call = apiService.agregarComentario(procesoId, nuevoComentario);

        call.enqueue(new Callback<ProcesoComentario>() {
            @Override
            public void onResponse(Call<ProcesoComentario> call, Response<ProcesoComentario> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    ProcesoComentario comentarioRespuesta = response.body();
                    Log.d("ProcesoDetalle", "Comentario agregado con éxito, ID: " + comentarioRespuesta.getId());

                    etNewComment.setText("");

                    commentInputLayout.setVisibility(View.GONE);

                    Toast.makeText(ProcesoDetalleActivity.this, "Comentario agregado con éxito", Toast.LENGTH_SHORT).show();

                    cargarComentarios(procesoId);
                } else {

                    String errorMsg = "Error al agregar comentario";
                    
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = "Error: " + response.errorBody().string();
                        }
                        Log.e("ProcesoDetalle", "Error al agregar comentario: " + response.code() + " - " + errorMsg);
                    } catch (IOException e) {
                        Log.e("ProcesoDetalle", "Error al leer errorBody", e);
                    }
                    
                    Toast.makeText(ProcesoDetalleActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProcesoComentario> call, Throwable t) {
                progressDialog.dismiss();
                
                Log.e("ProcesoDetalle", "Fallo al enviar comentario", t);
                Toast.makeText(ProcesoDetalleActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean checkPermissions() {

    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
           PackageManager.PERMISSION_GRANTED;
}

private void requestPermissions() {

    ActivityCompat.requestPermissions(this, 
                                      new String[]{Manifest.permission.CAMERA}, 
                                      PERMISSION_REQUEST_CODE);
}

    private void openImagePicker() {
        Log.d(TAG, "Abriendo cámara");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

                Log.e(TAG, "Error creando archivo para imagen", ex);
                Toast.makeText(this, "Error al crear archivo para imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.smooypr1.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        } else {
            Toast.makeText(this, "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {

            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d("ProcesoDetalle", "Todos los permisos fueron concedidos");
                openImagePicker();
            } else {
                Log.d("ProcesoDetalle", "Algunos permisos fueron denegados");
                // Mostrar mensaje al usuario
                Toast.makeText(this, "Se necesitan permisos para seleccionar imágenes", Toast.LENGTH_SHORT).show();

                boolean showRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        showRationale = true;
                        break;
                    }
                }

                if (showRationale) {

                    new AlertDialog.Builder(this)
                            .setTitle("Permisos necesarios")
                            .setMessage("Esta aplicación necesita acceso a la cámara y almacenamiento para poder subir fotos. Por favor, concede los permisos solicitados.")
                            .setPositiveButton("Aceptar", (dialog, which) -> requestPermissions())
                            .setNegativeButton("Cancelar", null)
                            .create()
                            .show();
                } else {

                    new AlertDialog.Builder(this)
                            .setTitle("Permisos denegados")
                            .setMessage("Has denegado permanentemente algunos permisos necesarios. Ve a Ajustes para habilitarlos manualmente.")
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

    private void handleApiError(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Desconocido";
            Log.e("ProcesoDetalle", "Error en respuesta API: Código " + response.code() + " - " + errorBody);

            String errorMsg = "Error al subir imagen (Código: " + response.code() + ")";
            Toast.makeText(ProcesoDetalleActivity.this, errorMsg, Toast.LENGTH_SHORT).show();

            new AlertDialog.Builder(ProcesoDetalleActivity.this)
                    .setTitle("Error al subir imagen")
                    .setMessage("Código: " + response.code() + "\nDetalle: " + errorBody)
                    .setPositiveButton("Aceptar", null)
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleApiFailure(Throwable t) {
        Log.e("ProcesoDetalle", "Fallo en API", t);

        String errorMsg = "Error de conexión: " + t.getClass().getSimpleName();
        if (t.getMessage() != null) {
            errorMsg += " - " + t.getMessage();
        }

        Toast.makeText(ProcesoDetalleActivity.this, errorMsg, Toast.LENGTH_SHORT).show();

        new AlertDialog.Builder(ProcesoDetalleActivity.this)
                .setTitle("Error de conexión")
                .setMessage("Detalles del error:\n" + t)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private String getRealPathFromURI(Uri contentUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                InputStream inputStream = getContentResolver().openInputStream(contentUri);
                if (inputStream == null) {
                    return null;
                }

                // Crear un archivo temporal
                File tempFile = createImageFile();

                // Copiar el contenido al archivo temporal
                FileOutputStream fos = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                fos.close();

                return tempFile.getAbsolutePath();
            } else {
                // Para versiones anteriores a Android 10
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
                if (cursor == null) return contentUri.getPath();

                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                cursor.close();
                return path;
            }
        } catch (Exception e) {
            Log.e("ProcesoDetalle", "Error al obtener la ruta real del archivo", e);
            return contentUri.getPath(); // Como fallback, devolver la ruta directa de la URI
        }
    }

    private void debugProceso(Proceso proceso) {
        if (proceso == null) {
            Log.e("DEBUG", "Proceso es NULL");
            return;
        }

        Log.d("DEBUG", "Datos del proceso:");
        Log.d("DEBUG", "ID: " + proceso.getId());
        Log.d("DEBUG", "Tipo: " + proceso.getTipo_proceso());
        Log.d("DEBUG", "Descripción: " + proceso.getDescripcion());
        Log.d("DEBUG", "Frecuencia: " + proceso.getFrecuencia());
        Log.d("DEBUG", "Horario: " + proceso.getHorario());
        Log.d("DEBUG", "Fecha inicio: " + proceso.getFecha_inicio());
        Log.d("DEBUG", "Fecha fin: " + proceso.getFecha_fin());
        Log.d("DEBUG", "Estado: " + proceso.getEstado());
    }

    private void showAddContentDialog() {

        String[] options = {"Añadir Comentario", "Añadir Imagen"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Añadir Contenido")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            commentInputLayout.setVisibility(View.VISIBLE);
                            photoInputLayout.setVisibility(View.GONE);
                            etNewComment.requestFocus();
                            break;
                        case 1:
                            photoInputLayout.setVisibility(View.VISIBLE);
                            commentInputLayout.setVisibility(View.GONE);
                            if (checkPermissions()) {
                                openImagePicker();
                            } else {
                                requestPermissions();
                            }
                            break;
                    }
                });

        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (fabAddContent != null) {
            fabAddContent.setVisibility(View.VISIBLE);
            fabAddContent.bringToFront();

            fabAddContent.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        fabAddContent.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300);
                    });
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fabAddContent != null) {
            fabAddContent.setVisibility(View.VISIBLE);
            fabAddContent.bringToFront();
        }
    }

    private void limpiarCacheGlide() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Glide.get(getApplicationContext()).clearDiskCache();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Glide.get(getApplicationContext()).clearMemory();
                            Toast.makeText(getApplicationContext(), "Caché de imágenes limpiada", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void verTareasProceso() {
        if (proceso != null && proceso.getId() > 0) {
            Intent intent = new Intent(this, TareasProcesoActivity.class);
            intent.putExtra("PROCESO_ID", proceso.getId());
            Log.d(TAG, "Abriendo TareasProcesoActivity con PROCESO_ID: " + proceso.getId());
            startActivityForResult(intent, 1001); // Usa un requestCode único
        } else {
            Toast.makeText(this, "Error: No se puede identificar el proceso", Toast.LENGTH_SHORT).show();
        }
    }


    private void toggleFabMenu() {
        if (isFabOpen) {
            // Cerrar menú
            fabAddContent.animate().rotation(0f);
            fabAgregarComentario.animate().translationY(0f).alpha(0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        fabAgregarComentario.setVisibility(View.INVISIBLE);
                        tvLabelComentario.setVisibility(View.INVISIBLE);
                    }
                });
        } else {

            fabAddContent.animate().rotation(45f);
            fabAgregarComentario.setVisibility(View.VISIBLE);
            tvLabelComentario.setVisibility(View.VISIBLE);
            fabAgregarComentario.animate().translationY(-70f).alpha(1f).setListener(null);
        }
        
        isFabOpen = !isFabOpen;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {

            cargarDatosProceso(proceso.getId());
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}