package com.smooy.smooypr1.procesos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.Establecimientos;
import com.smooy.smooypr1.R;


import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgregarProcesoActivity extends AppCompatActivity {

    private Spinner spTipoProceso, spFrecuencia, spEstado, spEstablecimiento;
    private EditText etDescripcion, etHorario, etFechaInicio, etFechaFin;
    private Button btnGuardar, btnCancelar;
    private Calendar calendar;
    private SimpleDateFormat dateFormat, timeFormat;
    private List<Establecimientos> establecimientos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_proceso);

        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        spTipoProceso = findViewById(R.id.spTipoProceso);
        spFrecuencia = findViewById(R.id.spFrecuencia);
        spEstado = findViewById(R.id.spEstado);
        spEstablecimiento = findViewById(R.id.spEstablecimiento);
        etDescripcion = findViewById(R.id.etDescripcion);
        etHorario = findViewById(R.id.etHorario);
        etFechaInicio = findViewById(R.id.etFechaInicio);
        etFechaFin = findViewById(R.id.etFechaFin);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        ArrayAdapter<CharSequence> tipoProcesoAdapter = ArrayAdapter.createFromResource(this,
                R.array.tipo_proceso_array, android.R.layout.simple_spinner_item);
        tipoProcesoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipoProceso.setAdapter(tipoProcesoAdapter);

        ArrayAdapter<CharSequence> frecuenciaAdapter = ArrayAdapter.createFromResource(this,
                R.array.frecuencia_array, android.R.layout.simple_spinner_item);
        frecuenciaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrecuencia.setAdapter(frecuenciaAdapter);

        ArrayAdapter<CharSequence> estadoAdapter = ArrayAdapter.createFromResource(this,
                R.array.estado_array, android.R.layout.simple_spinner_item);
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEstado.setAdapter(estadoAdapter);

        cargarEstablecimientos();

        etFechaInicio.setText(dateFormat.format(calendar.getTime()));
        etHorario.setText(timeFormat.format(calendar.getTime()));

        btnGuardar.setOnClickListener(v -> validarYGuardarProceso());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void cargarEstablecimientos() {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, List<com.smooy.smooypr1.Establecimientos>>> call = apiService.obtenerEstablecimientos();
        call.enqueue(new Callback<Map<String, List<Establecimientos>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Establecimientos>>> call, Response<Map<String, List<Establecimientos>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    establecimientos = response.body().get("establecimientos");
                    if (establecimientos != null) {
                        ArrayAdapter<Establecimientos> adapter = new ArrayAdapter<>(AgregarProcesoActivity.this,
                                android.R.layout.simple_spinner_item, establecimientos);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spEstablecimiento.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Establecimientos>>> call, Throwable t) {
                Toast.makeText(AgregarProcesoActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validarYGuardarProceso() {
        String tipoProceso = spTipoProceso.getSelectedItem().toString();
        String frecuencia = spFrecuencia.getSelectedItem().toString();
        String estado = spEstado.getSelectedItem().toString();
        String descripcion = etDescripcion.getText().toString().trim();
        String horario = etHorario.getText().toString().trim();
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();

        if (descripcion.isEmpty()) {
            etDescripcion.setError("La descripción es obligatoria");
            return;
        }
        if (horario.isEmpty()) {
            etHorario.setError("El horario es obligatorio");
            return;
        }
        if (fechaInicio.isEmpty()) {
            etFechaInicio.setError("La fecha de inicio es obligatoria");
            return;
        }

        Map<String, Object> procesoMap = new HashMap<>();
        procesoMap.put("tipoProceso", tipoProceso);
        procesoMap.put("descripcion", descripcion);
        procesoMap.put("frecuencia", frecuencia);
        procesoMap.put("horario", horario);
        procesoMap.put("fechaInicio", fechaInicio);
        procesoMap.put("fechaFin", fechaFin);
        procesoMap.put("estado", estado);

        Establecimientos establecimientoSeleccionado = (Establecimientos) spEstablecimiento.getSelectedItem();
        int establecimientoId = establecimientoSeleccionado.getId();
        procesoMap.put("establecimientoId", establecimientoId);

        SharedPreferences prefs = getSharedPreferences("proceso_prefs", MODE_PRIVATE);
        prefs.edit().putString("ultimo_proceso", new JSONObject(procesoMap).toString()).apply();

        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.agregarProceso(procesoMap);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AgregarProcesoActivity.this, "Proceso guardado correctamente", Toast.LENGTH_SHORT).show();


                    finish();
                } else {
                    Toast.makeText(AgregarProcesoActivity.this, "Error al guardar", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AgregarProcesoActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}