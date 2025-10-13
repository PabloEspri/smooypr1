package com.smooy.smooypr1.tareas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;

import com.smooy.smooypr1.R;
import com.smooy.smooypr1.procesos.Proceso;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TareasProcesoActivity extends AppCompatActivity implements TareaAdapter.OnTareaCheckedListener {

    private static final String TAG = "TareasProcesoActivity";
    private static final int REQUEST_NUEVA_TAREA = 101;
    private static final int REQUEST_TAREA_DETALLE = 102;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;

    private int procesoId;
    private TextView tvNombreProceso, tvEstadoProceso, tvProgresoTareas, tvEmptyTareas;
    private ProgressBar progressBarTareas;
    private RecyclerView recyclerTareas;
    private TareaAdapter tareaAdapter;
    private final List<Tarea> tareasList = new ArrayList<>();
    private ApiService apiService;
    private boolean esStaff = false;
    private FloatingActionButton fabAgregarTarea;
    private FloatingActionButton fabAdd, fabComment, fabCamera;
    private final boolean isFabOpen = false;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tareas_proceso);

        fabAgregarTarea = findViewById(R.id.fabAgregarTarea);

        configurarRecyclerView();

        setupRoleBasedPermissions();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tareas del Proceso");
        }

        procesoId = getIntent().getIntExtra("PROCESO_ID", -1);
        Log.d(TAG, "Recibido PROCESO_ID: " + procesoId);

        if (procesoId == -1) {
            Toast.makeText(this, "Error: No se pudo identificar el proceso", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error: PROCESO_ID no encontrado o inválido en el intent");
            finish();
            return;
        } else {
            Log.d(TAG, "Cargando tareas para proceso ID: " + procesoId);
        }


        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");
        esStaff = "Staff".equalsIgnoreCase(rolUsuario) || "Admin".equalsIgnoreCase(rolUsuario);

        tvNombreProceso = findViewById(R.id.tvNombreProceso);
        tvEstadoProceso = findViewById(R.id.tvEstadoProceso);
        tvProgresoTareas = findViewById(R.id.tvProgresoTareas);
        tvEmptyTareas = findViewById(R.id.tvEmptyTareas);
        progressBarTareas = findViewById(R.id.progressBarTareas);
        recyclerTareas = findViewById(R.id.recyclerTareas);
        fabAgregarTarea = findViewById(R.id.fabAgregarTarea);

        if (esStaff) {
            fabAgregarTarea.setVisibility(View.VISIBLE);
            fabAgregarTarea.setOnClickListener(v -> abrirFormularioNuevaTarea());
        } else {
            fabAgregarTarea.setVisibility(View.GONE);
        }

        configurarRecyclerView();

        apiService = ApiClient.getApiService();

        cargarDatosProceso();
        cargarTareasProceso();

        setupRoleBasedPermissions();
    }

    private void configurarRecyclerView() {
        recyclerTareas = findViewById(R.id.recyclerTareas);
        recyclerTareas.setLayoutManager(new LinearLayoutManager(this));
        
        tvEmptyTareas = findViewById(R.id.tvEmptyTareas);
        fabAgregarTarea = findViewById(R.id.fabAgregarTarea);

        tareaAdapter = new TareaAdapter(this, tareasList, esStaff);
        recyclerTareas.setAdapter(tareaAdapter);

        tareaAdapter.setOnTareaCheckedListener(this);

        tareaAdapter.setOnTareaClickListener(tarea -> {

            Log.d(TAG, "Abriendo detalle de tarea con ID: " + tarea.getId());
            
            Intent intent = new Intent(TareasProcesoActivity.this, TareaDetalleActivity.class);
            intent.putExtra("TAREA_ID", tarea.getId());
            startActivityForResult(intent, REQUEST_TAREA_DETALLE);
        });

        if (esStaff) {
            fabAgregarTarea.setVisibility(View.VISIBLE);
            fabAgregarTarea.setOnClickListener(v -> abrirFormularioNuevaTarea());
        } else {
            fabAgregarTarea.setVisibility(View.GONE);
        }
    }

    private void setupRoleBasedPermissions() {

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");

        boolean isAdminOrManager = rolUsuario.equalsIgnoreCase("Admin") || 
                                  rolUsuario.equalsIgnoreCase("Area Manager");

        if (tareaAdapter != null) {
            tareaAdapter.setVerificationEnabled(isAdminOrManager);
        }

        if (fabAgregarTarea != null) {
            fabAgregarTarea.setVisibility(isAdminOrManager ? View.VISIBLE : View.GONE);
        }

        Log.d(TAG, "Permisos configurados: " + rolUsuario + ", puede verificar: " + isAdminOrManager);
    }

    private void cargarDatosProceso() {
        Call<Proceso> call = apiService.obtenerProcesoPorId(procesoId);
        call.enqueue(new Callback<Proceso>() {
            @Override
            public void onResponse(Call<Proceso> call, Response<Proceso> response) {
                if (response.isSuccessful() && response.body() != null) {
                    actualizarInfoProceso(response.body());
                } else {
                    Toast.makeText(TareasProcesoActivity.this, "Error al cargar información del proceso", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Proceso> call, Throwable t) {
                Toast.makeText(TareasProcesoActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarInfoProceso(Proceso proceso) {
        tvNombreProceso.setText(proceso.getTipo_proceso());
        tvEstadoProceso.setText("Estado: " + proceso.getEstado());
    }

    private void cargarTareasProceso() {
        Call<List<Tarea>> call = apiService.obtenerTareasProceso(procesoId);
        call.enqueue(new Callback<List<Tarea>>() {
            @Override
            public void onResponse(Call<List<Tarea>> call, Response<List<Tarea>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Tarea> tareas = response.body();
                    
                    if (tareas != null && !tareas.isEmpty()) {
                        tareasList.clear();
                        tareasList.addAll(tareas);
                        tareaAdapter.notifyDataSetChanged();
                        tvEmptyTareas.setVisibility(View.GONE);
                    } else {
                        tvEmptyTareas.setVisibility(View.VISIBLE);
                        if (esStaff) {
                            // Ofrecer generar tareas automáticas
                            mostrarOpcionGenerarTareas();
                        }
                    }
                    
                    actualizarProgresoTareas();
                } else {
                    tvEmptyTareas.setVisibility(View.VISIBLE);
                    Toast.makeText(TareasProcesoActivity.this, 
                        "Error al cargar tareas: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error en respuesta: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Tarea>> call, Throwable t) {
                tvEmptyTareas.setVisibility(View.VISIBLE);
                Toast.makeText(TareasProcesoActivity.this, 
                    "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error al cargar tareas", t);
            }
        });
    }

    private void mostrarOpcionGenerarTareas() {

        if (tareasList.isEmpty() && esStaff) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("No hay tareas")
                .setMessage("¿Deseas generar tareas automáticas para este proceso?")
                .setPositiveButton("Generar", (dialog, which) -> generarTareas())
                .setNegativeButton("Cancelar", null)
                .show();
        }
    }

    private void generarTareas() {
        Call<Map<String, Object>> call = apiService.generarTareasProceso(procesoId);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TareasProcesoActivity.this, "Tareas generadas exitosamente", Toast.LENGTH_SHORT).show();
                    cargarTareasProceso();
                } else {
                    Toast.makeText(TareasProcesoActivity.this, "Error al generar tareas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(TareasProcesoActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarProgresoTareas() {
        int total = tareasList.size();
        int completadas = 0;

        for (Tarea tarea : tareasList) {
            if ("Completada".equals(tarea.getEstado())) {
                completadas++;
            }
        }

        int porcentaje = total > 0 ? (completadas * 100) / total : 0;

        tvProgresoTareas.setText("Progreso: " + completadas + "/" + total + " tareas completadas");
        progressBarTareas.setProgress(porcentaje);
    }

    private void abrirFormularioNuevaTarea() {
        Intent intent = new Intent(this, NuevaTareaActivity.class);
        intent.putExtra("PROCESO_ID", procesoId);
        startActivityForResult(intent, REQUEST_NUEVA_TAREA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Recargar tareas cuando volvamos de la pantalla de detalle de tarea
        if (requestCode == REQUEST_TAREA_DETALLE && resultCode == RESULT_OK) {
            Log.d(TAG, "Regresando desde detalle de tarea. Recargando datos.");
            cargarTareasProceso();
            // También recargamos los datos del proceso para reflejar cualquier cambio en el estado
            cargarDatosProceso();
        } 
        else if (requestCode == REQUEST_NUEVA_TAREA && resultCode == RESULT_OK) {
            cargarTareasProceso();
        } 
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            uploadImage(currentPhotoPath);
        }
    }

    @Override
    public void onTareaChecked(Tarea tarea, boolean isChecked) {
        // Eliminamos la restricción para que cualquier rol pueda marcar tareas
        // if (!esStaff) {
        //     return;
        // }

        Map<String, Object> tareaDatos = new HashMap<>();
        tareaDatos.put("estado", isChecked ? "Completada" : "Pendiente");

        if (isChecked) {
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            int userId = sharedPreferences.getInt("USER_ID", -1);
            if (userId != -1) {
                tareaDatos.put("usuario_completado_id", userId);
            }
        }

        Call<Map<String, Object>> call = apiService.actualizarTarea(tarea.getId(), tareaDatos);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    // Actualizar el estado de la tarea localmente
                    tarea.setEstado(isChecked ? "Completada" : "Pendiente");
                    actualizarProgresoTareas();
                    tareaAdapter.notifyDataSetChanged();

                    // Verificar y actualizar el estado del proceso
                    verificarEstadoProceso();
                    
                    // Actualizar los datos del proceso para refrescar la interfaz
                    cargarDatosProceso();
                } else {
                    // Revertir cambio visual si hay error
                    tareaAdapter.notifyDataSetChanged();
                    Toast.makeText(TareasProcesoActivity.this, "Error al actualizar tarea", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Revertir cambio visual si hay error
                tareaAdapter.notifyDataSetChanged();
                Toast.makeText(TareasProcesoActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verificarEstadoProceso() {
        Call<Map<String, Object>> call = apiService.verificarCompletado(procesoId);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean success = (boolean) response.body().get("success");
                    if (success) {
                        // El proceso ha sido marcado como completado
                        Toast.makeText(TareasProcesoActivity.this, "El proceso ha sido marcado como completado.", Toast.LENGTH_SHORT).show();
                        tvEstadoProceso.setText("Estado: Completado");
                        
                        // Opcional: recargar datos del proceso para asegurarse que el cambio se refleje
                        cargarDatosProceso();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(TareasProcesoActivity.this, "Error al verificar el estado del proceso", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private void uploadImage(String imagePath) {
        Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show();
    }

}