package com.smooy.smooypr1.usuarios;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.Establecimientos;
import com.smooy.smooypr1.R;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrearUsuariosActivity extends AppCompatActivity {
    private static final String TAG = "CrearUsuariosActivity";

    private EditText etNombre, etApellido, etNombreUsuario, etContraseña;
    private Spinner spRol;
    private Button btnCrear, btnVolver, btnAddEstablecimiento;
    private LinearLayout containerEstablecimientos;
    private TextView textViewTitulo; // Add this for changing the title
    private final List<Establecimientos> establecimientos = new ArrayList<>();
    private final List<Establecimientos> establecimientosSeleccionados = new ArrayList<>();
    private final List<Establecimientos> todosEstablecimientos = new ArrayList<>(); // Added missing list
    private boolean modoEdicion = false;
    private int usuarioIdEdicion = -1;
    private ArrayList<Integer> establecimientoIdsEdicion = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_usuarios);

        modoEdicion = getIntent().getBooleanExtra("MODO_EDICION", false);
        if (modoEdicion) {
            usuarioIdEdicion = getIntent().getIntExtra("USUARIO_ID", -1);
            establecimientoIdsEdicion = getIntent().getIntegerArrayListExtra("USUARIO_ESTABLECIMIENTOS");
        }

        inicializarVistas();
        cargarRoles();
        cargarEstablecimientos();
        configurarBotones();

        if (modoEdicion) {
            textViewTitulo.setText("Editar Usuario");
            btnCrear.setText("Actualizar");

            etNombre.setText(getIntent().getStringExtra("USUARIO_NOMBRE"));
            etApellido.setText(getIntent().getStringExtra("USUARIO_APELLIDO"));
            etNombreUsuario.setText(getIntent().getStringExtra("USUARIO_USERNAME"));

            etContraseña.setHint("Dejar vacío para mantener la misma contraseña");

            String rol = getIntent().getStringExtra("USUARIO_ROL");
            if (rol != null) {
                ArrayAdapter adapter = (ArrayAdapter) spRol.getAdapter();
                int position = adapter.getPosition(rol);
                if (position >= 0) {
                    spRol.setSelection(position);
                }
            }
        }
    }

    private void inicializarVistas() {
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etNombreUsuario = findViewById(R.id.etNombreUsuario);
        etContraseña = findViewById(R.id.etContraseña);
        spRol = findViewById(R.id.spRol);
        btnCrear = findViewById(R.id.btnCrear);
        btnVolver = findViewById(R.id.btnVolver);
        btnAddEstablecimiento = findViewById(R.id.btnAddEstablecimiento);
        containerEstablecimientos = findViewById(R.id.containerEstablecimientos);
        textViewTitulo = findViewById(R.id.textViewTitulo); // Initialize the title TextView
    }

    private void cargarRoles() {
        String rolUsuarioActual = obtenerRolUsuarioActual();
        String rolUsuarioEditado = getIntent().getStringExtra("USUARIO_ROL"); // Rol del usuario que editas

        ArrayAdapter<String> adapter;

        List<String> rolesDisponibles = new ArrayList<>();
        rolesDisponibles.add("Admin");
        rolesDisponibles.add("Area Manager");
        rolesDisponibles.add("Store Manager");
        rolesDisponibles.add("Staff");

        if (rolUsuarioActual == null) {
            rolUsuarioActual = "";
        }
        rolUsuarioActual = rolUsuarioActual.trim().toLowerCase();

        if (modoEdicion) {

            if (!"admin".equals(rolUsuarioActual)) {
                rolesDisponibles = obtenerRolesPermitidos(rolUsuarioActual);
            }
        } else {

            if ("area manager".equals(rolUsuarioActual)) {
                rolesDisponibles.clear();
                rolesDisponibles.add("Store Manager");
                rolesDisponibles.add("Staff");
            } else if ("store manager".equals(rolUsuarioActual)) {
                rolesDisponibles.clear();
                rolesDisponibles.add("Staff");
            }

        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rolesDisponibles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRol.setAdapter(adapter);

        if (modoEdicion && rolUsuarioEditado != null) {
            int position = adapter.getPosition(rolUsuarioEditado);
            if (position >= 0) {
                spRol.setSelection(position);
            }
        }
    }

    private List<String> obtenerRolesPermitidos(String rolUsuarioActual) {
        List<String> rolesPermitidos = new ArrayList<>();
        switch (rolUsuarioActual) {
            case "admin":
                rolesPermitidos.add("Admin");
                rolesPermitidos.add("Area Manager");
                rolesPermitidos.add("Store Manager");
                rolesPermitidos.add("Staff");
                break;
            case "area manager":
                rolesPermitidos.add("Store Manager");
                rolesPermitidos.add("Staff");
                break;
            case "store manager":
                rolesPermitidos.add("Staff");
                break;
            default:
                break;
        }
        return rolesPermitidos;
    }

    private String obtenerRolUsuarioActual() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("ROL_USUARIO", "");
    }


    private void cargarEstablecimientos() {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, List<Establecimientos>>> call = apiService.obtenerEstablecimientos();
        
        call.enqueue(new Callback<Map<String, List<Establecimientos>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Establecimientos>>> call, Response<Map<String, List<Establecimientos>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Establecimientos> listaEstablecimientos = response.body().get("establecimientos");
                    
                    if (listaEstablecimientos != null && !listaEstablecimientos.isEmpty()) {
                        todosEstablecimientos.clear();
                        todosEstablecimientos.addAll(listaEstablecimientos);
                        actualizarListaEstablecimientosSeleccionados();
                    } else {
                        Log.e(TAG, "No hay establecimientos disponibles");
                    }
                } else {
                    Log.e(TAG, "Error al cargar establecimientos: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, List<Establecimientos>>> call, Throwable t) {
                Log.e(TAG, "Error de conexión al cargar establecimientos: " + t.getMessage());
            }
        });
    }

    private void configurarBotones() {
        btnVolver.setOnClickListener(v -> finish());
        
        btnCrear.setOnClickListener(v -> validarYCrearUsuario());
        
        btnAddEstablecimiento.setOnClickListener(v -> mostrarDialogoEstablecimientos());
    }
    
    private void mostrarDialogoEstablecimientos() {
        if (establecimientos.isEmpty()) {
            Log.e(TAG, "No hay establecimientos disponibles");
            return;
        }

        String[] nombresEstablecimientos = new String[establecimientos.size()];
        for (int i = 0; i < establecimientos.size(); i++) {
            nombresEstablecimientos[i] = establecimientos.get(i).getNombre();
        }

        new AlertDialog.Builder(this)
            .setTitle("Seleccionar Establecimiento")
            .setItems(nombresEstablecimientos, (dialog, which) -> {
                Establecimientos seleccionado = establecimientos.get(which);

                boolean yaSeleccionado = false;
                for (Establecimientos e : establecimientosSeleccionados) {
                    if (e.getId() == seleccionado.getId()) {
                        yaSeleccionado = true;
                        break;
                    }
                }
                
                if (yaSeleccionado) {
                    Log.e(TAG, "Este establecimiento ya está seleccionado");
                } else {
                    // Añadir a la lista de seleccionados
                    establecimientosSeleccionados.add(seleccionado);
                    
                    // Añadir a la UI
                    agregarEstablecimientoAUI(seleccionado);
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void agregarEstablecimientoAUI(Establecimientos establecimiento) {
        // Inflar la vista del establecimiento seleccionado
        View view = LayoutInflater.from(this).inflate(
                R.layout.item_establecimiento_seleccionado, 
                containerEstablecimientos, 
                false);

        TextView tvNombre = view.findViewById(R.id.tvNombreEstablecimiento);
        ImageButton btnRemove = view.findViewById(R.id.btnRemoveEstablecimiento);
        
        tvNombre.setText(establecimiento.getNombre());

        view.setTag(establecimiento.getId());

        btnRemove.setOnClickListener(v -> {

            containerEstablecimientos.removeView(view);

            establecimientosSeleccionados.removeIf(e -> e.getId() == establecimiento.getId());
        });

        containerEstablecimientos.addView(view);
    }

    private void validarYCrearUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String nombreUsuario = etNombreUsuario.getText().toString().trim();
        String contraseña = etContraseña.getText().toString().trim();
        String rol = spRol.getSelectedItem().toString();

        if (nombre.isEmpty() || apellido.isEmpty() || nombreUsuario.isEmpty() || 
            (!modoEdicion && contraseña.isEmpty())) {
            Log.e(TAG, "Todos los campos son obligatorios");
            return;
        }

        Map<String, Object> usuarioMap = new HashMap<>();
        usuarioMap.put("nombre", nombre);
        usuarioMap.put("apellido", apellido);
        usuarioMap.put("usuario", nombreUsuario);

        usuarioMap.put("rol", rol);
        Log.d(TAG, "Rol enviado al backend: " + usuarioMap.get("rol"));

        if (!contraseña.isEmpty() || !modoEdicion) {
            usuarioMap.put("contraseña", contraseña);
        }

        List<Integer> establecimientoIds = new ArrayList<>();
        for (Establecimientos est : establecimientosSeleccionados) {
            establecimientoIds.add(est.getId());
        }
        usuarioMap.put("establecimientos", establecimientoIds);
        Log.d(TAG, "Establecimientos enviados: " + establecimientoIds);


        if (modoEdicion) {
            actualizarUsuario(usuarioIdEdicion, usuarioMap);
        } else {
            crearUsuario(usuarioMap);
        }
    }
    
    private void crearUsuario(Map<String, Object> usuarioMap) {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.crearUsuario(usuarioMap);
        
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resultado = response.body();

                    boolean success = (boolean) resultado.getOrDefault("success", false);
                    String mensaje = (String) resultado.getOrDefault("message", "");
                    
                    if (success) {
                        Log.d(TAG, "Usuario creado correctamente");

                        new Handler().postDelayed(() -> {
                            finish();
                        }, 1500);
                    } else {
                        Log.e(TAG, mensaje);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "Error: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar respuesta de error", e);
                        Log.e(TAG, "Error al crear usuario: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error de conexión", t);
                Log.e(TAG, "Error de conexión: " + t.getMessage());
            }
        });
    }

    private void actualizarUsuario(int usuarioId, Map<String, Object> usuarioMap) {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.actualizarUsuario(usuarioId, usuarioMap);
        
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Usuario actualizado correctamente");

                    new Handler().postDelayed(() -> finish(), 1000);
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? 
                                response.errorBody().string() : "Error desconocido";
                        Log.e(TAG, "Error al actualizar usuario: " + errorMsg);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer el mensaje de error", e);
                        Log.e(TAG, "Error al actualizar usuario");
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error de red al actualizar usuario", t);
                Log.e(TAG, "Error de red al actualizar usuario");
            }
        });
    }

    private void actualizarListaEstablecimientosSeleccionados() {

        establecimientos.clear();
        establecimientos.addAll(todosEstablecimientos);

        if (modoEdicion && establecimientoIdsEdicion != null && !establecimientoIdsEdicion.isEmpty()) {

            containerEstablecimientos.removeAllViews();

            establecimientosSeleccionados.clear();

            for (Integer estId : establecimientoIdsEdicion) {
                for (Establecimientos est : todosEstablecimientos) {
                    if (est.getId() == estId) {

                        establecimientosSeleccionados.add(est);

                        agregarEstablecimientoAUI(est);
                        break;
                    }
                }
            }
        }
    }

}