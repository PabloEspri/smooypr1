package com.smooy.smooypr1.tareas;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;

import com.google.android.material.textfield.TextInputEditText;
import com.smooy.smooypr1.R;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevaTareaActivity extends AppCompatActivity {

    private TextInputEditText etNombreTarea, etDescripcionTarea, etOrdenTarea;
    private Button btnGuardarTarea;
    private ApiService apiService;
    private int procesoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_tarea);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Nueva Tarea");
            }
        }

        procesoId = getIntent().getIntExtra("PROCESO_ID", -1);
        if (procesoId == -1) {
            Toast.makeText(this, "Error: No se pudo identificar el proceso", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etNombreTarea = findViewById(R.id.etNombreTarea);
        etDescripcionTarea = findViewById(R.id.etDescripcionTarea);
        etOrdenTarea = findViewById(R.id.etOrdenTarea);
        btnGuardarTarea = findViewById(R.id.btnGuardarTarea);

        apiService = ApiClient.getApiService();

        btnGuardarTarea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarNuevaTarea();
            }
        });
    }

    private void guardarNuevaTarea() {
        String nombre = etNombreTarea.getText().toString().trim();
        String descripcion = etDescripcionTarea.getText().toString().trim();
        String ordenStr = etOrdenTarea.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            etNombreTarea.setError("El nombre es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(descripcion)) {
            etDescripcionTarea.setError("La descripción es obligatoria");
            return;
        }

        if (TextUtils.isEmpty(ordenStr)) {
            etOrdenTarea.setError("El orden es obligatorio");
            return;
        }

        int orden;
        try {
            orden = Integer.parseInt(ordenStr);
        } catch (NumberFormatException e) {
            etOrdenTarea.setError("Ingrese un número válido");
            return;
        }

        Map<String, Object> tareaDatos = new HashMap<>();
        tareaDatos.put("proceso_id", procesoId);
        tareaDatos.put("nombre", nombre);
        tareaDatos.put("descripcion", descripcion);
        tareaDatos.put("orden", orden);
        tareaDatos.put("estado", "Pendiente");

        Call<Map<String, Object>> call = apiService.crearTarea(tareaDatos);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(NuevaTareaActivity.this, "Tarea creada exitosamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Toast.makeText(NuevaTareaActivity.this, "Error: " + errorBody, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(NuevaTareaActivity.this, "Error al crear tarea", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(NuevaTareaActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
}