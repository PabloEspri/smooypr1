package com.smooy.smooypr1.establecimientos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smooy.smooypr1.R;
import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.Establecimientos;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstablecimientosActivity extends AppCompatActivity {

    private static final String TAG = "EstablecimientosActivity";
    private RecyclerView recyclerViewEstablecimientos;
    private EstablecimientoAdapter establecimientoAdapter;
    private Button btnVolver;
    private MaterialButton btnAgregarEstablecimiento, btnFiltrarEstablecimiento;
    private final List<Establecimientos> establecimientos = new ArrayList<>();
    private final List<Establecimientos> establecimientosOriginales = new ArrayList<>();
    private int usuarioId;
    private String rolUsuario;
    private String filtroActual = "Todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_establecimientos);

        inicializarVistas();
        configurarRecyclerView();
        configurarBotones();

        iniciarCarga();
    }

    private void inicializarVistas() {
        recyclerViewEstablecimientos = findViewById(R.id.recyclerViewEstablecimientos);
        btnVolver = findViewById(R.id.btnVolver);
        btnAgregarEstablecimiento = findViewById(R.id.btnAgregarEstablecimiento);
        btnFiltrarEstablecimiento = findViewById(R.id.btnFiltrarEstablecimiento);
    }

    private void configurarRecyclerView() {
        recyclerViewEstablecimientos.setLayoutManager(new LinearLayoutManager(this));
        establecimientoAdapter = new EstablecimientoAdapter(this, establecimientos);
        recyclerViewEstablecimientos.setAdapter(establecimientoAdapter);

        establecimientoAdapter.setOnDeleteClickListener((establecimiento, position) -> {

            new AlertDialog.Builder(this)
                    .setTitle("Eliminar establecimiento")
                    .setMessage("¿Está seguro que desea eliminar el establecimiento " + establecimiento.getNombre() + "?")
                    .setPositiveButton("Eliminar", (dialog, which) -> eliminarEstablecimiento(establecimiento.getId(), position))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        establecimientoAdapter.setOnItemClickListener((establecimiento, position) -> {

            Intent intent = new Intent(EstablecimientosActivity.this, EstablecimientoDetalleActivity.class);
            intent.putExtra("ESTABLECIMIENTO_ID", establecimiento.getId());
            intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimiento.getNombre());
            intent.putExtra("ESTABLECIMIENTO_DIRECCION", establecimiento.getDireccion());
            intent.putExtra("ESTABLECIMIENTO_TIPO", establecimiento.getTipo());
            intent.putExtra("ESTABLECIMIENTO_ESTADO", establecimiento.getEstado());
            startActivity(intent);
        });
    }

    private void configurarBotones() {
        btnVolver.setOnClickListener(v -> finish());

        btnAgregarEstablecimiento.setOnClickListener(v -> {
            Intent intent = new Intent(EstablecimientosActivity.this, CrearEstablecimientoActivity.class);
            startActivity(intent);
        });

        btnFiltrarEstablecimiento.setOnClickListener(v -> mostrarDialogoFiltros());
    }

    private void mostrarDialogoFiltros() {
        // Crear un AlertDialog con opciones de filtro
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_filtrar_establecimientos, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        RadioGroup radioGroupFiltro = dialogView.findViewById(R.id.radioGroupFiltro);
        Button btnAplicarFiltro = dialogView.findViewById(R.id.btnAplicarFiltro);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarFiltro);

        List<String> tiposUnicos = obtenerTiposUnicos();

        RadioButton rbTodos = new RadioButton(this);
        rbTodos.setText("Todos los establecimientos");
        rbTodos.setId(View.generateViewId());
        radioGroupFiltro.addView(rbTodos);

        if (filtroActual.equals("Todos")) {
            rbTodos.setChecked(true);
        }

        Map<Integer, String> mapaIdTipo = new HashMap<>();
        for (String tipo : tiposUnicos) {
            RadioButton rb = new RadioButton(this);
            rb.setText(tipo);
            int id = View.generateViewId();
            rb.setId(id);
            mapaIdTipo.put(id, tipo);
            radioGroupFiltro.addView(rb);

            if (filtroActual.equals(tipo)) {
                rb.setChecked(true);
            }
        }

        btnAplicarFiltro.setOnClickListener(view -> {
            int selectedId = radioGroupFiltro.getCheckedRadioButtonId();
            if (selectedId == -1) {
                filtroActual = "Todos";
            } else if (selectedId == rbTodos.getId()) {
                filtroActual = "Todos";
            } else {
                filtroActual = mapaIdTipo.get(selectedId);
            }

            aplicarFiltro();
            dialog.dismiss();
        });

        btnCancelar.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private List<String> obtenerTiposUnicos() {
        List<String> tipos = new ArrayList<>();
        for (Establecimientos establecimiento : establecimientosOriginales) {
            String tipo = establecimiento.getTipo();
            if (!tipos.contains(tipo)) {
                tipos.add(tipo);
            }
        }
        Collections.sort(tipos);
        return tipos;
    }

    private void aplicarFiltro() {
        establecimientos.clear();

        if (filtroActual.equals("Todos")) {
            establecimientos.addAll(establecimientosOriginales);
        } else {
            for (Establecimientos est : establecimientosOriginales) {
                if (est.getTipo().equals(filtroActual)) {
                    establecimientos.add(est);
                }
            }
        }

        establecimientoAdapter.actualizarLista(establecimientos);
    }

    private void cargarEstablecimientos() {
        Log.d(TAG, "Iniciando carga de establecimientos...");

        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, List<Establecimientos>>> call;
        if ("Admin".equalsIgnoreCase(rolUsuario) || "Area Manager".equalsIgnoreCase(rolUsuario)) {
            Log.d(TAG, "Usuario con rol elevado (" + rolUsuario + "), cargando TODOS los establecimientos");
            call = apiService.obtenerEstablecimientos();
        } else {
            Log.d(TAG, "Usuario con rol limitado, cargando establecimientos del usuario ID: " + usuarioId);
            call = apiService.obtenerEstablecimientosUsuario(usuarioId);
        }

        call.enqueue(new Callback<Map<String, List<Establecimientos>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Establecimientos>>> call, Response<Map<String, List<Establecimientos>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, List<Establecimientos>> data = response.body();
                    List<Establecimientos> listaEstablecimientos = data.get("establecimientos");

                    Log.d(TAG, "Establecimientos recibidos del servidor: " + (listaEstablecimientos != null ? listaEstablecimientos.size() : 0));

                    if (listaEstablecimientos != null && !listaEstablecimientos.isEmpty()) {
                        establecimientos.clear();
                        establecimientosOriginales.clear(); // Limpiar la lista original

                        establecimientos.addAll(listaEstablecimientos);
                        establecimientosOriginales.addAll(listaEstablecimientos); // Guardar una copia de la lista original

                        establecimientoAdapter.actualizarLista(establecimientos);

                        // Aplicar el filtro actual (si hay alguno)
                        if (!filtroActual.equals("Todos")) {
                            aplicarFiltro();
                        }
                    } else {
                        Log.d(TAG, "No hay establecimientos para mostrar");
                    }
                } else {
                    // Manejar error en la respuesta
                    Log.e(TAG, "Error en la respuesta: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e(TAG, "Cuerpo del error: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer cuerpo de error", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Establecimientos>>> call, Throwable t) {
                Log.e(TAG, "Error en la solicitud", t);
            }
        });
    }

    private void eliminarEstablecimiento(int establecimientoId, int position) {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.eliminarEstablecimiento(establecimientoId);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resultado = response.body();
                    boolean success = (boolean) resultado.getOrDefault("success", false);

                    if (success) {
                        if (position >= 0 && position < establecimientos.size()) {
                            int idEliminar = establecimientos.get(position).getId();
                            establecimientoAdapter.eliminarEstablecimiento(position);
                            establecimientosOriginales.removeIf(est -> est.getId() == idEliminar);
                        }
                    } else {
                        String mensaje = (String) resultado.getOrDefault("message", "Error desconocido");
                        Log.e(TAG, "Error al eliminar establecimiento: " + mensaje);
                    }
                } else {
                    Log.e(TAG, "Error al eliminar establecimiento: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error en la solicitud", t);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEstablecimientos();
    }

    private void iniciarCarga() {

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        usuarioId = sharedPreferences.getInt("USER_ID", -1);
        rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");

        Log.d(TAG, "Datos cargados: Usuario ID=" + usuarioId + ", Rol=" + rolUsuario);

        if (usuarioId == -1 || rolUsuario.isEmpty()) {
            Log.e(TAG, "Error: No se pudo obtener información del usuario");
            finish();
            return;
        }

        TextView tvTitulo = findViewById(R.id.tvTituloEstablecimientos);
        if (tvTitulo != null) {
            if ("Admin".equalsIgnoreCase(rolUsuario)) {
                tvTitulo.setText("Gestión de todos los establecimientos");
            } else {
                tvTitulo.setText("Mis establecimientos asignados");
            }
        }

        cargarEstablecimientos();
    }
}