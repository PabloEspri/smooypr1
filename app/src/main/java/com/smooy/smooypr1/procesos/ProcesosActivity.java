package com.smooy.smooypr1.procesos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.Establecimientos;

import com.google.android.material.button.MaterialButton;
import com.smooy.smooypr1.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProcesosActivity extends AppCompatActivity {

    private static final String TAG = "ProcesosActivity";
    private static final int REQUEST_DETALLE_PROCESO = 100;
    private RecyclerView recyclerViewProcesos;
    private ProcesoAdapter procesoAdapter;
    private Button btnVolver;
    private Button btnAgregarProceso;
    private MaterialButton btnFiltrarProcesos;
    private final List<Proceso> procesos = new ArrayList<>();
    private final List<Proceso> procesosOriginales = new ArrayList<>();
    private boolean esStaff = false;
    private boolean esAdmin = false;
    private boolean esAreaManager = false;
    private int establecimientoId = -1;
    private String rolUsuario = "";
    private String nombreEstablecimiento = "";

    private String filtroEstablecimiento = "Todos";
    private String filtroEstado = "Todos";
    private final List<Establecimientos> listaEstablecimientos = new ArrayList<>();

    private boolean vieneDesdeLista = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_procesos);

        obtenerInformacionUsuario();
        inicializarComponentes();
        configurarAdaptador();

        cargarEstablecimientos();

        Log.d(TAG, "esAdmin=" + esAdmin + ", esAreaManager=" + esAreaManager + ", vieneDesdeLista=" + vieneDesdeLista);
        Toast.makeText(this, "Agregar visible: " + (btnAgregarProceso.getVisibility() == View.VISIBLE), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Filtrar visible: " + (btnFiltrarProcesos.getVisibility() == View.VISIBLE), Toast.LENGTH_SHORT).show();

    }

    private void obtenerInformacionUsuario() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        Log.d(TAG, "Contenido de SharedPreferences:");
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d(TAG, entry.getKey() + ": " + entry.getValue().toString());
        }

        rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");
        if (rolUsuario.isEmpty()) {
            rolUsuario = sharedPreferences.getString("USER_ROLE", "");
        }
        Log.d(TAG, "Rol de usuario obtenido: " + rolUsuario);

        establecimientoId = sharedPreferences.getInt("ESTABLECIMIENTO_ID", -1);
        if (establecimientoId == -1) {
            establecimientoId = sharedPreferences.getInt("establecimiento_id", -1);
        }
        Log.d(TAG, "ID de establecimiento obtenido: " + establecimientoId);

        // Obtener nombre de establecimiento, intentando ambas claves posibles
        nombreEstablecimiento = sharedPreferences.getString("ESTABLECIMIENTO_NOMBRE", "");
        if (nombreEstablecimiento.isEmpty()) {
            nombreEstablecimiento = sharedPreferences.getString("establecimiento_nombre", "");
        }
        Log.d(TAG, "Nombre de establecimiento obtenido: " + nombreEstablecimiento);

        // Determinar roles del usuario
        esAdmin = "Admin".equalsIgnoreCase(rolUsuario.trim());
        esAreaManager = "Area Manager".equalsIgnoreCase(rolUsuario.trim());
        esStaff = "Staff".equalsIgnoreCase(rolUsuario) || esAdmin || esAreaManager;

        Log.d(TAG, "¿Es usuario admin?: " + esAdmin);
        Log.d(TAG, "¿Es usuario area manager?: " + esAreaManager);
        Log.d(TAG, "¿Es usuario staff?: " + esStaff);

        // Comprobar si hay información adicional en el intent
        if (getIntent().hasExtra("ES_STAFF")) {
            esStaff = getIntent().getBooleanExtra("ES_STAFF", false);
            Log.d(TAG, "Valor ES_STAFF del intent: " + esStaff);
        }

        vieneDesdeLista = getIntent().hasExtra("DESDE_ESTABLECIMIENTO") &&
                getIntent().getBooleanExtra("DESDE_ESTABLECIMIENTO", false);

        if (getIntent().hasExtra("ESTABLECIMIENTO_ID")) {
            establecimientoId = getIntent().getIntExtra("ESTABLECIMIENTO_ID", -1);
            Log.d(TAG, "ESTABLECIMIENTO_ID del intent: " + establecimientoId);

            if (vieneDesdeLista && establecimientoId > 0) {
                filtroEstablecimiento = String.valueOf(establecimientoId);
                Log.d(TAG, "Filtro establecimiento forzado desde detalle al ID: " + filtroEstablecimiento);
            }
        }

        // Establecer filtro por establecimiento si es necesario
        if ((vieneDesdeLista || (!esAdmin && !esAreaManager)) && establecimientoId > 0) {
            filtroEstablecimiento = String.valueOf(establecimientoId);
            Log.d(TAG, "Filtro establecimiento forzado al ID: " + filtroEstablecimiento);
        }

        // Establecer título de la actividad según el contexto
        if (vieneDesdeLista && !nombreEstablecimiento.isEmpty()) {
            setTitle("Procesos de " + nombreEstablecimiento);
        } else if (esAreaManager) {
            setTitle("Gestión de procesos - Area Manager");
        } else if (esStaff && !esAdmin && !nombreEstablecimiento.isEmpty()) {
            setTitle("Procesos de " + nombreEstablecimiento);
        } else if (esStaff && !esAdmin) {
            setTitle("Procesos de tu establecimiento");
        } else {
            setTitle("Gestión de procesos");
        }
    }

    private void inicializarComponentes() {
        recyclerViewProcesos = findViewById(R.id.recyclerViewProcesos);
        recyclerViewProcesos.setLayoutManager(new LinearLayoutManager(this));

        btnVolver = findViewById(R.id.btnVolver);
        btnAgregarProceso = findViewById(R.id.btnAgregarProceso);
        btnFiltrarProcesos = findViewById(R.id.btnFiltrarProcesos);


        // Solo administradores pueden agregar procesos
        btnAgregarProceso.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

        // Solo administradores y area managers pueden filtrar procesos
        btnFiltrarProcesos.setVisibility(((esAdmin || esAreaManager) && !vieneDesdeLista) ? View.VISIBLE : View.GONE);

        // Configurar listeners de botones
        btnVolver.setOnClickListener(v -> {
            Log.d(TAG, "Botón volver clickeado");
            onBackPressed();
        });

        btnAgregarProceso.setOnClickListener(v -> {
            Log.d(TAG, "Botón agregar proceso clickeado");
            Intent intent = new Intent(ProcesosActivity.this, AgregarProcesoActivity.class);
            startActivity(intent);
        });

        btnFiltrarProcesos.setOnClickListener(v -> {
            Log.d(TAG, "Botón filtrar procesos clickeado");
            mostrarDialogoFiltros();
        });
    }

    private void configurarAdaptador() {
        procesoAdapter = new ProcesoAdapter(this, procesos);
        procesoAdapter.setStaffMode(esStaff);
        procesoAdapter.setAdminMode(esAdmin);

        if (esAdmin) {
            procesoAdapter.setOnDeleteClickListener(proceso -> {
                new AlertDialog.Builder(ProcesosActivity.this)
                        .setTitle("Eliminar proceso")
                        .setMessage("¿Estás seguro de que quieres eliminar este proceso?")
                        .setPositiveButton("Sí, eliminar", (dialog, which) -> eliminarProceso(proceso))
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }

        // Agregar el listener para verificar procesos SOLO para Admin y AreaManager
        if (esAdmin || esAreaManager) {
            procesoAdapter.setOnVerifyClickListener((proceso, position) -> {
                verificarProceso(proceso, position);
            });
        }

        recyclerViewProcesos.setAdapter(procesoAdapter);
        procesoAdapter.setOnItemClickListener(this::abrirDetalleProceso);
    }

    private void verificarProceso(Proceso proceso, int position) {

        Log.d(TAG, "⚠️ Iniciando verificación para proceso ID: " + proceso.getId());

        ApiService apiService = ApiClient.getApiService();

        // Primero intentamos con el endpoint específico de verificación
        Call<Map<String, Object>> call = apiService.verificarCompletado(proceso.getId());

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Verificación exitosa con el endpoint verificar-completado: " + response.body());

                    // Como el endpoint marca como "Completado" pero necesitamos "Verificado",
                    // hacemos una llamada adicional para cambiar el estado
                    actualizarEstadoAVerificado(proceso, position);
                } else {
                    Log.d(TAG, "❌ Fallo en verificar-completado con código: " + response.code() + ", intentando método alternativo");
                    // Si falla, intentamos directamente con el método de actualizar estado
                    actualizarEstadoAVerificado(proceso, position);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "⚠️ Error de conexión en verificar-completado: " + t.getMessage());
                // Si la conexión falla, intentamos con el método alternativo
                actualizarEstadoAVerificado(proceso, position);
            }
        });
    }

    private void actualizarEstadoAVerificado(Proceso proceso, int position) {
        ApiService apiService = ApiClient.getApiService();
        Map<String, String> estado = new HashMap<>();
        estado.put("estado", "Verificado");

        Log.d(TAG, "🔄 Actualizando estado a 'Verificado' para proceso ID: " + proceso.getId() + " usando endpoint /estado");

        Call<EstadoResponse> call = apiService.actualizarEstadoProceso(proceso.getId(), estado);

        call.enqueue(new Callback<EstadoResponse>() {
            @Override
            public void onResponse(Call<EstadoResponse> call, Response<EstadoResponse> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ Actualización de estado exitosa: " + response.body().getMensaje());
                    manejarVerificacionExitosa(proceso, position);
                } else {
                    String codigo = String.valueOf(response.code());
                    String mensaje = response.body() != null ? response.body().getMensaje() : "Error desconocido";
                    Log.e(TAG, "❌ Error al actualizar estado: " + codigo + " - " + mensaje);
                    manejarErrorVerificacion(response);
                }
            }

            @Override
            public void onFailure(Call<EstadoResponse> call, Throwable t) {

                Log.e(TAG, "⚠️ Error de conexión al actualizar estado: " + t.getMessage());
                Toast.makeText(ProcesosActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void intentarActualizarEstado(Proceso proceso, int position) {
        ApiService apiService = ApiClient.getApiService();
        Map<String, String> estado = new HashMap<>();
        estado.put("estado", "Verificado");

        Log.d(TAG, "🔄 Intentando actualizar estado a 'Verificado' para proceso ID: " + proceso.getId() + " usando endpoint /estado");

        Call<EstadoResponse> call = apiService.actualizarEstadoProceso(proceso.getId(), estado);

        call.enqueue(new Callback<EstadoResponse>() {
            @Override
            public void onResponse(Call<EstadoResponse> call, Response<EstadoResponse> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ Actualización de estado exitosa: " + response.body().getMensaje());
                    manejarVerificacionExitosa(proceso, position);
                } else {
                    String codigo = String.valueOf(response.code());
                    String mensaje = response.body() != null ? response.body().getMensaje() : "Error desconocido";
                    Log.e(TAG, "❌ Error al actualizar estado: " + codigo + " - " + mensaje);
                    manejarErrorVerificacion(response);
                }
            }

            @Override
            public void onFailure(Call<EstadoResponse> call, Throwable t) {

                Log.e(TAG, "⚠️ Error de conexión al actualizar estado: " + t.getMessage());
                Toast.makeText(ProcesosActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void manejarVerificacionExitosa(Proceso proceso, int position) {


        // Update the state of the process in the local list
        proceso.setEstado("Verificado");

        // Important: Update the process in the original list too to maintain persistence after filtering
        for (Proceso p : procesosOriginales) {
            if (p.getId() == proceso.getId()) {
                p.setEstado("Verificado");
                break;
            }
        }

        // Notify the adapter to update the view
        procesoAdapter.actualizarEstadoProceso(position, "Verificado");

        // Force refresh the UI to ensure button visibility is updated correctly
        procesoAdapter.notifyItemChanged(position);

        // Importante: También actualizar la UI si existe un detalle abierto de este proceso
        actualizarDetalleProcesoSiEsVisible(proceso.getId());

        Toast.makeText(ProcesosActivity.this, "Proceso verificado exitosamente", Toast.LENGTH_SHORT).show();
    }
    private void actualizarDetalleProcesoSiEsVisible(int procesoId) {
        Log.d(TAG, "📣 Enviando broadcast para actualizar detalle del proceso ID: " + procesoId);

        // Crear un intent con la acción personalizada
        Intent intent = new Intent("com.example.smooypr1.PROCESO_ACTUALIZADO");

        // Agregar datos al intent
        intent.putExtra("PROCESO_ID", procesoId);
        intent.putExtra("NUEVO_ESTADO", "Verificado");

        // Enviar el broadcast
        sendBroadcast(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DETALLE_PROCESO && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("PROCESO_ID") && data.hasExtra("NUEVO_ESTADO")) {
                int procesoId = data.getIntExtra("PROCESO_ID", -1);
                String nuevoEstado = data.getStringExtra("NUEVO_ESTADO");

                if (procesoId != -1 && nuevoEstado != null) {
                    // Buscar el proceso en la lista y actualizar su estado
                    for (int i = 0; i < procesos.size(); i++) {
                        if (procesos.get(i).getId() == procesoId) {
                            procesos.get(i).setEstado(nuevoEstado);
                            procesoAdapter.notifyItemChanged(i);
                            break;
                        }
                    }

                    // También actualizar en la lista original
                    for (Proceso p : procesosOriginales) {
                        if (p.getId() == procesoId) {
                            p.setEstado(nuevoEstado);
                            break;
                        }
                    }
                }
            } else {
                cargarProcesos();
            }
        }
    }

    private void manejarErrorVerificacion(Response<?> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
            Log.e(TAG, "Error al verificar proceso: " + response.code() + " - " + errorBody);

            String mensajeError;
            switch (response.code()) {
                case 400:
                    mensajeError = "Error en la solicitud. Datos incorrectos.";
                    break;
                case 401:
                    mensajeError = "No autorizado. Inicie sesión nuevamente.";
                    break;
                case 403:
                    mensajeError = "No tiene permisos para realizar esta acción.";
                    break;
                case 404:
                    mensajeError = "Proceso no encontrado.";
                    break;
                case 500:
                    mensajeError = "Error del servidor. Intente más tarde.";
                    break;
                default:
                    mensajeError = "Error " + response.code() + ". Contacte soporte.";
            }

            Toast.makeText(ProcesosActivity.this, mensajeError, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error al leer el error: " + e.getMessage());
            Toast.makeText(ProcesosActivity.this, "Error al verificar proceso", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarProcesos();
    }

    private void cargarEstablecimientos() {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, List<Establecimientos>>> call = apiService.obtenerEstablecimientos();

        call.enqueue(new Callback<Map<String, List<Establecimientos>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Establecimientos>>> call, Response<Map<String, List<Establecimientos>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, List<Establecimientos>> data = response.body();
                    listaEstablecimientos.clear();

                    List<Establecimientos> establecimientos = data.get("establecimientos");
                    if (establecimientos != null) {
                        listaEstablecimientos.addAll(establecimientos);
                        Log.d(TAG, "Establecimientos cargados: " + listaEstablecimientos.size());

                        if (nombreEstablecimiento.isEmpty() && establecimientoId > 0) {
                            for (Establecimientos est : listaEstablecimientos) {
                                if (est.getId() == establecimientoId) {
                                    nombreEstablecimiento = est.getNombre();

                                    if (vieneDesdeLista || (esStaff && !esAdmin && !esAreaManager)) {
                                        setTitle("Procesos de " + nombreEstablecimiento);
                                    }
                                    break;
                                }
                            }
                        }

                        procesoAdapter.cargarNombresEstablecimientos(listaEstablecimientos);

                        // Una vez que tenemos los establecimientos, cargamos los procesos
                        cargarProcesos();
                    } else {

                        Log.e(TAG, "Lista de establecimientos nula en la respuesta");
                        cargarProcesos(); // Intentamos cargar los procesos de todos modos
                    }
                } else {

                    Log.e(TAG, "Error al cargar establecimientos: " + response.code());
                    cargarProcesos(); // Intentamos cargar los procesos de todos modos
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Establecimientos>>> call, Throwable t) {

                Log.e(TAG, "Error de conexión al cargar establecimientos: " + t.getMessage());

                // Mostrar mensaje de error al usuario
                Toast.makeText(ProcesosActivity.this,
                        "Error al cargar establecimientos: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();

                // Intentamos cargar los procesos de todos modos
                cargarProcesos();
            }
        });
    }

    private void mostrarDialogoFiltros() {
        if (vieneDesdeLista) {
            Toast.makeText(this, "En esta vista solo se muestran procesos del establecimiento seleccionado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!esAdmin && !esAreaManager) {
            Toast.makeText(this, "Solo los administradores y area managers pueden filtrar procesos", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filtrar_procesos, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        RadioGroup radioGroupEstablecimiento = dialogView.findViewById(R.id.radioGroupEstablecimiento);
        RadioGroup radioGroupEstado = dialogView.findViewById(R.id.radioGroupEstado);
        Button btnLimpiar = dialogView.findViewById(R.id.btnLimpiarFiltros);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarFiltroProcesos);
        Button btnAplicar = dialogView.findViewById(R.id.btnAplicarFiltroProcesos);

        cargarOpcionesEstablecimiento(radioGroupEstablecimiento);
        cargarOpcionesEstado(radioGroupEstado);

        btnLimpiar.setOnClickListener(v -> {
            filtroEstablecimiento = "Todos";
            filtroEstado = "Todos";
            aplicarFiltros();
            dialog.dismiss();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnAplicar.setOnClickListener(v -> {
            // Obtener establecimiento seleccionado
            int idEstablecimiento = radioGroupEstablecimiento.getCheckedRadioButtonId();
            if (idEstablecimiento != -1) {
                RadioButton rbEstablecimiento = dialogView.findViewById(idEstablecimiento);
                if (rbEstablecimiento != null) {
                    String tag = (String) rbEstablecimiento.getTag();
                    filtroEstablecimiento = tag != null ? tag : "Todos";
                    Log.d(TAG, "Establecimiento seleccionado: " + rbEstablecimiento.getText() + " (ID: " + filtroEstablecimiento + ")");
                } else {
                    filtroEstablecimiento = "Todos";
                    Log.d(TAG, "No se encontró el RadioButton de establecimiento con ID: " + idEstablecimiento);
                }
            } else {
                filtroEstablecimiento = "Todos";
                Log.d(TAG, "Ningún establecimiento seleccionado, usando 'Todos'");
            }

            // Obtener estado seleccionado
            int idEstado = radioGroupEstado.getCheckedRadioButtonId();
            if (idEstado != -1) {
                RadioButton rbEstado = dialogView.findViewById(idEstado);
                if (rbEstado != null) {
                    String tag = (String) rbEstado.getTag();
                    filtroEstado = tag != null ? tag : rbEstado.getText().toString();
                    Log.d(TAG, "Estado seleccionado: " + filtroEstado);
                } else {
                    filtroEstado = "Todos";
                    Log.d(TAG, "No se encontró el RadioButton de estado con ID: " + idEstado);
                }
            } else {
                filtroEstado = "Todos";
                Log.d(TAG, "Ningún estado seleccionado, usando 'Todos'");
            }

            aplicarFiltros();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void cargarOpcionesEstablecimiento(RadioGroup radioGroup) {
        // Opción "Todos"
        RadioButton rbTodos = new RadioButton(this);
        rbTodos.setText("Todos los establecimientos");
        rbTodos.setTag("Todos");
        rbTodos.setId(View.generateViewId());
        radioGroup.addView(rbTodos);

        boolean seleccionEncontrada = false;
        if (filtroEstablecimiento.equals("Todos")) {
            rbTodos.setChecked(true);
            seleccionEncontrada = true;
        }

        // Opciones según el rol
        if (esAdmin || esAreaManager) {
            for (Establecimientos establecimiento : listaEstablecimientos) {
                RadioButton rb = new RadioButton(this);
                rb.setText(establecimiento.getNombre());
                rb.setTag(String.valueOf(establecimiento.getId()));
                rb.setId(View.generateViewId());
                radioGroup.addView(rb);

                if (filtroEstablecimiento.equals(String.valueOf(establecimiento.getId()))) {
                    rb.setChecked(true);
                    seleccionEncontrada = true;
                }
            }
        }
        else if (establecimientoId > 0) {
            RadioButton rb = new RadioButton(this);
            rb.setText("Mi establecimiento");
            rb.setTag(String.valueOf(establecimientoId));
            rb.setId(View.generateViewId());
            radioGroup.addView(rb);

            if (filtroEstablecimiento.equals(String.valueOf(establecimientoId))) {
                rb.setChecked(true);
                seleccionEncontrada = true;
            }
        }

        // Si no se encontró ninguna selección válida, seleccionar "Todos"
        if (!seleccionEncontrada) {
            rbTodos.setChecked(true);
            filtroEstablecimiento = "Todos";
        }
    }

    private void cargarOpcionesEstado(RadioGroup radioGroup) {
        // Recopilar estados únicos de todos los procesos
        Set<String> estadosUnicos = new HashSet<>();

        // Añadir estados predeterminados
        estadosUnicos.add("Pendiente");
        estadosUnicos.add("En proceso");
        estadosUnicos.add("Completado");
        estadosUnicos.add("Cancelado");
        estadosUnicos.add("Verificado");

        // Añadir estados de los procesos cargados
        for (Proceso proceso : procesosOriginales) {
            if (proceso.getEstado() != null && !proceso.getEstado().isEmpty()) {
                estadosUnicos.add(proceso.getEstado());
            }
        }

        // Opción "Todos"
        RadioButton rbTodos = new RadioButton(this);
        rbTodos.setText("Todos los estados");
        rbTodos.setTag("Todos");
        rbTodos.setId(View.generateViewId());
        radioGroup.addView(rbTodos);

        boolean seleccionEncontrada = false;
        if (filtroEstado.equals("Todos")) {
            rbTodos.setChecked(true);
            seleccionEncontrada = true;
        }

        // Opciones de estados
        List<String> listaEstados = new ArrayList<>(estadosUnicos);
        Collections.sort(listaEstados);

        for (String estado : listaEstados) {
            RadioButton rb = new RadioButton(this);
            rb.setText(estado);
            rb.setTag(estado);
            rb.setId(View.generateViewId());
            radioGroup.addView(rb);

            if (filtroEstado.equals(estado)) {
                rb.setChecked(true);
                seleccionEncontrada = true;
            }
        }

        // Si no se encontró ninguna selección válida, seleccionar "Todos"
        if (!seleccionEncontrada) {
            rbTodos.setChecked(true);
            filtroEstado = "Todos";
        }
    }

    private void aplicarFiltros() {
        procesos.clear();

        Log.d(TAG, "Aplicando filtros - Establecimiento: " + filtroEstablecimiento + ", Estado: " + filtroEstado);

        // Forzar filtro por establecimiento si es necesario
        if ((vieneDesdeLista || (!esAdmin && !esAreaManager)) && establecimientoId > 0) {
            filtroEstablecimiento = String.valueOf(establecimientoId);
            Log.d(TAG, "Forzando filtro por establecimiento ID: " + filtroEstablecimiento);
        }

        // Si no hay filtros y es Admin/Area Manager, mostrar todos los procesos
        if (filtroEstablecimiento.equals("Todos") && filtroEstado.equals("Todos") &&
                (esAdmin || esAreaManager) && !vieneDesdeLista) {
            procesos.addAll(procesosOriginales);
            Log.d(TAG, "Admin/Area Manager: Mostrando todos los procesos: " + procesos.size());
        } else {
            // Aplicar filtros
            for (Proceso proceso : procesosOriginales) {
                boolean cumpleFiltroEstablecimiento;

                if (filtroEstablecimiento.equals("Todos") && (esAdmin || esAreaManager) && !vieneDesdeLista) {
                    cumpleFiltroEstablecimiento = true;
                } else {
                    try {
                        int idEstablecimientoFiltro = Integer.parseInt(filtroEstablecimiento);
                        cumpleFiltroEstablecimiento = (proceso.getEstablecimiento_id() == idEstablecimientoFiltro);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error al convertir ID de establecimiento: " + filtroEstablecimiento, e);
                        cumpleFiltroEstablecimiento = false;
                    }
                }

                boolean cumpleFiltroEstado = filtroEstado.equals("Todos") ||
                        (proceso.getEstado() != null && proceso.getEstado().equals(filtroEstado));

                Log.d(TAG, "Proceso ID: " + proceso.getId() +
                        ", Establecimiento: " + proceso.getEstablecimiento_id() +
                        ", Estado: " + proceso.getEstado() +
                        ", Cumple filtro establecimiento: " + cumpleFiltroEstablecimiento +
                        ", Cumple filtro estado: " + cumpleFiltroEstado);

                if (cumpleFiltroEstablecimiento && cumpleFiltroEstado) {
                    procesos.add(proceso);
                }
            }
            Log.d(TAG, "Procesos filtrados: " + procesos.size());
        }

        // Notificar al adaptador
        procesoAdapter.notifyDataSetChanged();

        // Mostrar mensaje si no hay procesos
        if (procesos.isEmpty()) {
            Toast.makeText(this, "No se encontraron procesos con los filtros aplicados", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarProcesos() {

        procesos.clear();
        procesosOriginales.clear();

        ApiService apiService = ApiClient.getApiService();
        Call<ProcesosResponse> call;

        // Determinar qué endpoint usar según el contexto
        if (vieneDesdeLista && establecimientoId > 0) {
            Log.d(TAG, "Vista específica: Filtrando procesos por establecimiento ID: " + establecimientoId);
            call = apiService.obtenerProcesos(establecimientoId);
        }
        else if ((esAdmin || esAreaManager) && !vieneDesdeLista) {
            Log.d(TAG, "Admin/Area Manager: Obteniendo todos los procesos");
            call = apiService.obtenerProcesos();
        }

        else if (esStaff && establecimientoId > 0) {
            Log.d(TAG, "Staff: Filtrando procesos por establecimiento ID: " + establecimientoId);
            call = apiService.obtenerProcesos(establecimientoId);
        } else {

            Log.d(TAG, "Rol no reconocido o sin establecimiento: " + rolUsuario +
                    ", establecimientoId: " + establecimientoId);
            call = apiService.obtenerProcesos();
        }

        call.enqueue(new Callback<ProcesosResponse>() {
            @Override
            public void onResponse(Call<ProcesosResponse> call, Response<ProcesosResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getProcesos() != null) {
                        procesosOriginales.addAll(response.body().getProcesos());
                        Log.d(TAG, "Procesos recibidos: " + procesosOriginales.size());
                    } else {
                        Log.d(TAG, "Lista de procesos nula en la respuesta");
                    }

                    if (procesosOriginales.isEmpty()) {
                        if (vieneDesdeLista) {
                            Toast.makeText(ProcesosActivity.this,
                                    "No hay procesos para el establecimiento seleccionado",
                                    Toast.LENGTH_LONG).show();
                        } else if (esAreaManager) {
                            Toast.makeText(ProcesosActivity.this,
                                    "No hay procesos disponibles. Utilice los filtros para buscar procesos específicos.",
                                    Toast.LENGTH_LONG).show();
                        } else if (esStaff && !esAdmin) {
                            Toast.makeText(ProcesosActivity.this,
                                    "No hay procesos asignados a tu establecimiento" +
                                            (nombreEstablecimiento.isEmpty() ? " (ID: " + establecimientoId + ")" : " (" + nombreEstablecimiento + ")"),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProcesosActivity.this,
                                    "No hay procesos disponibles",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (!procesosOriginales.isEmpty()) {
                        Collections.sort(procesosOriginales, new Comparator<Proceso>() {
                            @Override
                            public int compare(Proceso p1, Proceso p2) {

                                if (p1.getFecha_inicio() == null && p2.getFecha_inicio() == null) {

                                    return compareHorarios(p1.getHorario(), p2.getHorario());
                                }
                                if (p1.getFecha_inicio() == null) return 1;
                                if (p2.getFecha_inicio() == null) return -1;

                                int dateCompare = p2.getFecha_inicio().compareTo(p1.getFecha_inicio());
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }

                                return compareHorarios(p1.getHorario(), p2.getHorario());
                            }
                        });
                    }

                    aplicarFiltros();

                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e(TAG, "Error al cargar procesos: " + response.code() + " - " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer error body", e);
                    }
                    Toast.makeText(ProcesosActivity.this,
                            "Error al cargar procesos: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProcesosResponse> call, Throwable t) {
                Log.e(TAG, "Error de conexión: " + t.getMessage(), t);
                Toast.makeText(ProcesosActivity.this,
                        "Error de conexión: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int compareHorarios(String horario1, String horario2) {

        if (horario1 == null && horario2 == null) return 0;
        if (horario1 == null) return 1;
        if (horario2 == null) return -1;

        try {
            String h1 = horario1.trim();
            String h2 = horario2.trim();
            return h2.compareTo(h1);
        } catch (Exception e) {
            Log.e(TAG, "Error comparando horarios: " + e.getMessage());
            return 0;
        }
    }

    private void eliminarProceso(Proceso proceso) {


        ApiService apiService = ApiClient.getApiService();
        Call<Void> call = apiService.eliminarProceso(proceso.getId());

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                if (response.isSuccessful()) {
                    procesos.remove(proceso);
                    procesosOriginales.remove(proceso);
                    procesoAdapter.notifyDataSetChanged();
                    Toast.makeText(ProcesosActivity.this, "Proceso eliminado exitosamente", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e(TAG, "Error al eliminar proceso: " + response.code() + " - " + errorBody);
                        Toast.makeText(ProcesosActivity.this, "Error al eliminar proceso: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer el error: " + e.getMessage());
                        Toast.makeText(ProcesosActivity.this, "Error al eliminar proceso", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

                Log.e(TAG, "Error de conexión: " + t.getMessage());
                Toast.makeText(ProcesosActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void abrirDetalleProceso(Proceso proceso) {
        Intent intent = new Intent(this, ProcesoDetalleActivity.class);

        intent.putExtra("ES_STAFF", esStaff);

        intent.putExtra("ID", proceso.getId());
        intent.putExtra("TIPO_PROCESO", proceso.getTipo_proceso());
        intent.putExtra("DESCRIPCION", proceso.getDescripcion());
        intent.putExtra("FRECUENCIA", proceso.getFrecuencia());
        intent.putExtra("HORARIO", proceso.getHorario());
        intent.putExtra("FECHA_INICIO", proceso.getFecha_inicio());
        intent.putExtra("FECHA_FIN", proceso.getFecha_fin());
        intent.putExtra("ESTADO", proceso.getEstado());
        intent.putExtra("ESTABLECIMIENTO_ID", proceso.getEstablecimiento_id());

        startActivity(intent);
    }
}