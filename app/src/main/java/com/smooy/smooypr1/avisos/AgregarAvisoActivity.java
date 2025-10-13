package com.smooy.smooypr1.avisos;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.Establecimientos;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smooy.smooypr1.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgregarAvisoActivity extends AppCompatActivity {
    private static final String TAG = "AgregarAvisoActivity";
    private static final int MAX_NOMBRE_LENGTH = 100;
    private static final int MAX_DESCRIPCION_LENGTH = 500;
    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private Spinner spCategoria, spEstablecimiento;
    private EditText etNombre, etDescripcion;
    private Button btnGuardar, btnCancelar;
    private CheckBox cbEnviarATodos; // Nuevo checkbox para admins
    private FloatingActionButton fabTakePhoto;
    private ImageView imgPhotoPreview;
    private List<Establecimientos> establecimientos;
    private int usuarioId;
    private String rolUsuario;
    private boolean esAdmin = false;
    private Uri selectedImageUri;
    private File photoFile;

    // Variables para datos precargados desde Tareas
    private boolean desdeTarea = false;
    private int tareaId = -1;
    private String tareaNombre = "";
    private String tareaDescripcion = "";
    private int preselectedEstablecimientoId = -1;
    private int procesoId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_aviso);

        procesarExtrasIntent();

        obtenerUsuario();
        inicializarVistas();
        configurarVisibilidadAdmin();
        setupCategoriaSpinner();
        cargarEstablecimientos();
        setupListeners();
    }


    private void procesarExtrasIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            desdeTarea = intent.getBooleanExtra("DESDE_TAREA", false);

            if (desdeTarea) {
                tareaId = intent.getIntExtra("TAREA_ID", -1);
                tareaNombre = intent.getStringExtra("TAREA_NOMBRE");
                tareaDescripcion = intent.getStringExtra("TAREA_DESCRIPCION");
                preselectedEstablecimientoId = intent.getIntExtra("ESTABLECIMIENTO_ID", -1);
                procesoId = intent.getIntExtra("PROCESO_ID", -1);

                Log.d(TAG, "Creando aviso desde tarea ID: " + tareaId +
                        ", establecimiento: " + preselectedEstablecimientoId +
                        ", proceso ID: " + procesoId);
            }
        }
    }

    private void obtenerUsuario() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        usuarioId = sharedPreferences.getInt("USER_ID", -1);
        rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");
        esAdmin = "Admin".equals(rolUsuario);

        if (usuarioId == -1) {
            mostrarError("Error: Usuario no identificado");
            finish();
        }

        Log.d(TAG, "Usuario ID: " + usuarioId + ", Rol: " + rolUsuario + ", Es Admin: " + esAdmin);
    }

    private void inicializarVistas() {
        spCategoria = findViewById(R.id.spCategoria);
        spEstablecimiento = findViewById(R.id.spEstablecimiento);
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        cbEnviarATodos = findViewById(R.id.cbEnviarATodos); // Nuevo checkbox

        fabTakePhoto = findViewById(R.id.fabTakePhoto);
        imgPhotoPreview = findViewById(R.id.imgPhotoPreview);
    }

    private void configurarVisibilidadAdmin() {
        CardView cardEnviarTodos = findViewById(R.id.cardEnviarTodos);
        TextView tvEstablecimiento = findViewById(R.id.tvEstablecimiento);
        CardView cardEstablecimiento = findViewById(R.id.cardEstablecimiento);

        if (cbEnviarATodos != null && cardEnviarTodos != null) {
            cardEnviarTodos.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

            if (esAdmin) {
                cbEnviarATodos.setOnCheckedChangeListener((buttonView, isChecked) -> {

                    spEstablecimiento.setEnabled(!isChecked);

                    float alpha = isChecked ? 0.5f : 1.0f;
                    if (tvEstablecimiento != null) tvEstablecimiento.setAlpha(alpha);
                    if (cardEstablecimiento != null) cardEstablecimiento.setAlpha(alpha);

                    if (isChecked) {
                        Toast.makeText(this, "El aviso se enviará a todos los establecimientos", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Seleccione un establecimiento específico", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void setupListeners() {
        btnGuardar.setOnClickListener(v -> validarYGuardarAviso());
        btnCancelar.setOnClickListener(v -> finish());

        fabTakePhoto.setOnClickListener(v -> {
            if (checkPermissions()) {
                openCamera();
            } else {
                requestPermissions();
            }
        });
    }

    private void setupCategoriaSpinner() {
        try {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.categoria_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCategoria.setAdapter(adapter);
        } catch (Exception e) {
            mostrarError("Error al cargar categorías: " + e.getMessage());
        }
    }

    private void cargarEstablecimientos() {
        ApiService apiService = ApiClient.getApiService();

        Call<Map<String, List<Establecimientos>>> call;

        // Si es admin, cargar todos los establecimientos, si no, solo los del usuario
        if (esAdmin) {
            call = apiService.obtenerTodosLosEstablecimientos();
        } else {
            call = apiService.obtenerEstablecimientosPorUsuario(usuarioId);
        }

        call.enqueue(new Callback<Map<String, List<Establecimientos>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Establecimientos>>> call, Response<Map<String, List<Establecimientos>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    establecimientos = response.body().get("establecimientos");
                    if (establecimientos != null && !establecimientos.isEmpty()) {
                        ArrayAdapter<Establecimientos> adapter = new ArrayAdapter<>(AgregarAvisoActivity.this,
                                android.R.layout.simple_spinner_item, establecimientos);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spEstablecimiento.setAdapter(adapter);

                        // Precargar datos si viene de una tarea
                        if (desdeTarea) {
                            // Preseleccionar el establecimiento correcto
                            seleccionarEstablecimientoPorId();
                            // Precargar el resto de datos
                            precargarDatosTarea();
                        }
                    } else {
                        mostrarError("No hay establecimientos disponibles");
                    }
                } else {
                    mostrarError("Error al cargar establecimientos");
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Establecimientos>>> call, Throwable t) {
                mostrarError("Error de conexión: " + t.getMessage());
            }
        });
    }

    private void validarYGuardarAviso() {
        String nombre = etNombre.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        if (!validarCampos(nombre, descripcion)) return;

        String categoria = spCategoria.getSelectedItem().toString();

        // Verificar si el admin quiere enviar a todos los establecimientos
        boolean enviarATodos = esAdmin && cbEnviarATodos != null && cbEnviarATodos.isChecked();

        if (enviarATodos) {
            // Enviar aviso a todos los establecimientos
            enviarAvisoATodosLosEstablecimientos(nombre, categoria, descripcion);
        } else {
            // Comportamiento normal - enviar a un establecimiento específico
            Establecimientos selectedEstablecimiento = (Establecimientos) spEstablecimiento.getSelectedItem();

            Aviso aviso;
            // Si viene de una tarea y tenemos un ID de proceso válido, lo incluimos en el aviso
            if (desdeTarea && procesoId > 0) {
                aviso = new Aviso(nombre, categoria, descripcion, selectedEstablecimiento.getId(), usuarioId, procesoId);
                Log.d(TAG, "Creando aviso con proceso ID: " + procesoId);
            } else {
                aviso = new Aviso(nombre, categoria, descripcion, selectedEstablecimiento.getId(), usuarioId);
            }

            guardarAviso(aviso);
        }
    }

    /**
     * Método para enviar avisos a todos los establecimientos (solo para admins)
     */
    private void enviarAvisoATodosLosEstablecimientos(String nombre, String categoria, String descripcion) {
        if (!esAdmin || establecimientos == null || establecimientos.isEmpty()) {
            mostrarError("No se puede enviar a todos los establecimientos");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando aviso a todos los establecimientos...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ApiService apiService = ApiClient.getApiService();

        // CREAR LISTA DE IDs DE ESTABLECIMIENTOS
        List<Integer> establecimientosIds = new ArrayList<>();
        for (Establecimientos est : establecimientos) {
            establecimientosIds.add(est.getId());
        }

        Log.d(TAG, "Enviando aviso masivo a " + establecimientosIds.size() + " establecimientos");
        Log.d(TAG, "IDs de establecimientos: " + establecimientosIds.toString());

        // Crear el aviso masivo CON LA LISTA DE ESTABLECIMIENTOS
        AvisoMasivo avisoMasivo = new AvisoMasivo(nombre, categoria, descripcion, usuarioId, establecimientosIds);

        // Si viene de una tarea, incluir el proceso ID
        if (desdeTarea && procesoId > 0) {
            avisoMasivo.setProcesoId(procesoId);
            Log.d(TAG, "Incluyendo proceso ID: " + procesoId);
        }

        // Log del objeto que se va a enviar
        Log.d(TAG, "Objeto AvisoMasivo: " + avisoMasivo.toString());

        // Llamar al endpoint para envío masivo
        Call<Map<String, Object>> call = apiService.enviarAvisoMasivo(avisoMasivo);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressDialog.dismiss();

                Log.d(TAG, "Respuesta del servidor - Código: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    Log.d(TAG, "Respuesta exitosa: " + result.toString());

                    Boolean success = (Boolean) result.get("success");
                    String message = (String) result.get("message");

                    if (success != null && success) {
                        Toast.makeText(AgregarAvisoActivity.this,
                                message != null ? message : "Aviso enviado exitosamente",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        mostrarError("Error: " + (message != null ? message : "Respuesta inesperada del servidor"));
                    }
                } else {
                    // Log del error detallado
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Sin detalles";
                        Log.e(TAG, "Error del servidor: " + response.code() + " - " + errorBody);
                        mostrarError("Error del servidor: " + response.code() + " - " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer respuesta de error", e);
                        mostrarError("Error del servidor: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Error de conexión", t);
                mostrarError("Error de conexión: " + t.getMessage());
            }
        });
    }

    private boolean validarCampos(String nombre, String descripcion) {
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("El nombre no puede estar vacío");
            return false;
        }
        if (nombre.length() > MAX_NOMBRE_LENGTH) {
            etNombre.setError("Máx. " + MAX_NOMBRE_LENGTH + " caracteres");
            return false;
        }
        if (TextUtils.isEmpty(descripcion)) {
            etDescripcion.setError("La descripción no puede estar vacía");
            return false;
        }
        if (descripcion.length() > MAX_DESCRIPCION_LENGTH) {
            etDescripcion.setError("Máx. " + MAX_DESCRIPCION_LENGTH + " caracteres");
            return false;
        }
        if (spCategoria.getSelectedItem() == null) {
            mostrarError("Seleccione una categoría");
            return false;
        }

        // Solo validar establecimiento si no es admin o si admin no marcó "enviar a todos"
        boolean enviarATodos = esAdmin && cbEnviarATodos != null && cbEnviarATodos.isChecked();
        if (!enviarATodos && spEstablecimiento.getSelectedItem() == null) {
            mostrarError("Seleccione un establecimiento");
            return false;
        }

        return true;
    }

    private void guardarAviso(Aviso aviso) {
        ApiService apiService = ApiClient.getApiService();
        Call<Aviso> call = apiService.agregarAviso(aviso);
        call.enqueue(new Callback<Aviso>() {
            @Override
            public void onResponse(Call<Aviso> call, Response<Aviso> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Aviso avisoCreado = response.body();

                    // Si hay imagen seleccionada, subirla después de crear el aviso
                    if (selectedImageUri != null) {
                        uploadPhoto(selectedImageUri, avisoCreado.getId());
                    } else {
                        finish();
                    }
                } else {
                    manejarErrorRespuesta(response);
                }
            }

            @Override
            public void onFailure(Call<Aviso> call, Throwable t) {
                mostrarError("Error de conexión: " + t.getMessage());
            }
        });
    }

    private void manejarErrorRespuesta(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Sin detalles";
            Log.e(TAG, "Error: " + response.code() + " - " + errorBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mostrarError("Error al guardar el aviso");
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        Log.e(TAG, mensaje);
    }

    // Resto de métodos para manejo de fotos (sin cambios)
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
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
                mostrarError("No se pudo crear el archivo para la foto");
            }
        } else {
            mostrarError("No hay aplicación de cámara disponible");
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
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imgPhotoPreview.setImageBitmap(bitmap);
                imgPhotoPreview.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Log.e(TAG, "Error al mostrar la imagen", e);
                mostrarError("Error al mostrar la imagen seleccionada");
            }
        }
    }

    private void uploadPhoto(Uri imageUri) {
        mostrarError("Primero guarde el aviso antes de subir una imagen");
    }

    private void uploadPhoto(Uri imageUri, int avisoId) {
        if (imageUri == null) {
            mostrarError("No hay imagen seleccionada");
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
                        if (response.isSuccessful()) {
                            finish();
                        } else {
                            // El aviso ya se guardó, solo falló la imagen
                            Log.e(TAG, "Error al subir la imagen: " + response.code());
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<AvisoImagen> call, Throwable t) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Fallo al subir imagen", t);
                        finish();
                    }
                });
            }
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error al procesar la imagen", e);
            mostrarError("Error al procesar la imagen");
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Log.w(TAG, "No se concedieron todos los permisos necesarios");

                // Mostrar diálogo para ir a configuración si los permisos son necesarios
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        }
    }

    private void precargarDatosTarea() {
        if (!desdeTarea) return;

        // Pre-cargar título sugerido para el aviso
        String tituloSugerido = getIntent().getStringExtra("AVISO_TITULO_SUGERIDO");
        if (tituloSugerido != null) {
            etNombre.setText(tituloSugerido);
        }

        // Pre-cargar descripción sugerida para el aviso
        String descripcionSugerida = getIntent().getStringExtra("AVISO_DESCRIPCION_SUGERIDA");
        if (descripcionSugerida != null) {
            etDescripcion.setText(descripcionSugerida);
        }

        // Pre-seleccionar categoría si está sugerida
        String categoriaSugerida = getIntent().getStringExtra("AVISO_CATEGORIA_SUGERIDA");
        if (categoriaSugerida != null && spCategoria.getAdapter() != null) {
            for (int i = 0; i < spCategoria.getAdapter().getCount(); i++) {
                if (spCategoria.getAdapter().getItem(i).toString().equals(categoriaSugerida)) {
                    spCategoria.setSelection(i);
                    break;
                }
            }
        }
    }

    private void seleccionarEstablecimientoPorId() {
        if (preselectedEstablecimientoId == -1 || establecimientos == null || establecimientos.isEmpty() || spEstablecimiento == null) {
            return;
        }

        for (int i = 0; i < establecimientos.size(); i++) {
            if (establecimientos.get(i).getId() == preselectedEstablecimientoId) {
                spEstablecimiento.setSelection(i);
                break;
            }
        }
    }
}