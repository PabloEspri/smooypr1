package com.smooy.smooypr1.establecimientos;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.google.android.material.button.MaterialButton;
import com.smooy.smooypr1.R;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrearEstablecimientoActivity extends AppCompatActivity {
    private static final String TAG = "CrearEstablecimientoAct";

    private EditText etNombre, etDireccion;
    private Spinner spEstado;

    private boolean[] tiposSeleccionados;
    private String[] tiposArray;
    private MaterialButton btnSeleccionarTipos;
    private MaterialButton btnCrear, btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_establecimientos);

        inicializarVistas();
        cargarTiposEstablecimientos();
        cargarEstados();
        configurarBotones();
    }

    private void inicializarVistas() {
        etNombre = findViewById(R.id.etNombre);
        etDireccion = findViewById(R.id.etDireccion);
        spEstado = findViewById(R.id.spEstado);
        btnSeleccionarTipos = findViewById(R.id.btnSeleccionarTipos);
        btnCrear = findViewById(R.id.btnCrear);
        btnVolver = findViewById(R.id.btnVolver);
    }

    private void cargarTiposEstablecimientos() {
        tiposArray = getResources().getStringArray(R.array.tipos_establecimientos_array);
        tiposSeleccionados = new boolean[tiposArray.length];

        btnSeleccionarTipos.setOnClickListener(v -> mostrarDialogoTipos());
    }


    private void mostrarDialogoTipos() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Selecciona tipos de establecimiento")
                .setMultiChoiceItems(tiposArray, tiposSeleccionados, (dialog, which, isChecked) -> {
                    tiposSeleccionados[which] = isChecked;
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    StringBuilder tiposSeleccionadosTexto = new StringBuilder();
                    for (int i = 0; i < tiposArray.length; i++) {
                        if (tiposSeleccionados[i]) {
                            if (tiposSeleccionadosTexto.length() > 0) tiposSeleccionadosTexto.append(", ");
                            tiposSeleccionadosTexto.append(tiposArray[i]);
                        }
                    }
                    btnSeleccionarTipos.setText(tiposSeleccionadosTexto.toString()); // 👈 mostrar los seleccionados en el botón
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



    private void cargarEstados() {
        // Configurar el Spinner de estados
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.estados_establecimientos_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEstado.setAdapter(adapter);
    }

    private void configurarBotones() {
        btnVolver.setOnClickListener(v -> finish());
        btnCrear.setOnClickListener(v -> validarYCrearEstablecimiento());
    }

    private void validarYCrearEstablecimiento() {
        String nombre = etNombre.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        StringBuilder tiposSeleccionadosTexto = new StringBuilder();
        for (int i = 0; i < tiposArray.length; i++) {
            if (tiposSeleccionados[i]) {
                if (tiposSeleccionadosTexto.length() > 0) tiposSeleccionadosTexto.append(", ");
                tiposSeleccionadosTexto.append(tiposArray[i]);
            }
        }
        String tipo = tiposSeleccionadosTexto.toString();
        String estado = spEstado.getSelectedItem().toString();

        if (nombre.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> establecimientoMap = new HashMap<>();
        establecimientoMap.put("nombre", nombre);
        establecimientoMap.put("direccion", direccion);
        establecimientoMap.put("tipo", tipo);
        establecimientoMap.put("estado", estado);

        crearEstablecimiento(establecimientoMap);
    }


    private void crearEstablecimiento(Map<String, Object> establecimientoMap) {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.crearEstablecimiento(establecimientoMap);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resultado = response.body();

                    // Verificar si fue exitoso
                    boolean success = (boolean) resultado.getOrDefault("success", false);
                    String mensaje = (String) resultado.getOrDefault("message", "");

                    if (success) {
                        Toast.makeText(CrearEstablecimientoActivity.this, "Establecimiento creado correctamente", Toast.LENGTH_SHORT).show();

                        // Añadir un breve retraso antes de cerrar para mostrar el mensaje
                        new Handler().postDelayed(() -> {
                            finish(); // Esto cierra la actividad actual y vuelve a MenuActivity
                        }, 1500); // Esperar 1.5 segundos para mostrar el mensaje
                    } else {
                        Toast.makeText(CrearEstablecimientoActivity.this, mensaje, Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "Error: " + errorBody);
                        Toast.makeText(CrearEstablecimientoActivity.this, "Error: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar respuesta de error", e);
                        Toast.makeText(CrearEstablecimientoActivity.this, "Error al crear establecimiento: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error de conexión", t);
                Toast.makeText(CrearEstablecimientoActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}