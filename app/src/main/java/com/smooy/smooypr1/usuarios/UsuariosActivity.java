package com.smooy.smooypr1.usuarios;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.android.material.textfield.TextInputEditText;
import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;

import com.google.android.material.button.MaterialButton;
import com.smooy.smooypr1.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuariosActivity extends AppCompatActivity {

    private static final String TAG = "UsuariosActivity";

    private RecyclerView recyclerViewUsuarios;
    private UsuarioAdapter usuarioAdapter;
    private Button btnVolver;
    private MaterialButton btnAgregarUsuario;
    private MaterialButton btnFiltrarUsuario;
    private TextView tvEstablecimientoNombre;
    private TextInputEditText etBuscarUsuario;

    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Usuario> usuariosFiltrados = new ArrayList<>();
    private int usuarioId;
    private String filtroRolActual = "Todos";
    private String textoBusqueda = "";

    private int establecimientoSeleccionadoId = -1;
    private String establecimientoSeleccionadoNombre = "";
    private boolean filtrarPorEstablecimiento = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        if (getIntent().hasExtra("ESTABLECIMIENTO_ID")) {
            establecimientoSeleccionadoId = getIntent().getIntExtra("ESTABLECIMIENTO_ID", -1);
            establecimientoSeleccionadoNombre = getIntent().getStringExtra("ESTABLECIMIENTO_NOMBRE");
            filtrarPorEstablecimiento = true;
            Log.d(TAG, "Mostrando usuarios del establecimiento: " + establecimientoSeleccionadoNombre + " (ID: " + establecimientoSeleccionadoId + ")");
        }

        inicializarVistas();
        configurarRecyclerView();
        configurarBotones();
        configurarBusqueda();

        if (filtrarPorEstablecimiento && tvEstablecimientoNombre != null) {
            tvEstablecimientoNombre.setText("Usuarios de: " + establecimientoSeleccionadoNombre);
            tvEstablecimientoNombre.setVisibility(View.VISIBLE);
        }

        iniciarCarga();
    }

    private void inicializarVistas() {
        recyclerViewUsuarios = findViewById(R.id.recyclerViewUsuarios);
        btnVolver = findViewById(R.id.btnVolver);
        btnAgregarUsuario = findViewById(R.id.btnAgregarUsuario);
        btnFiltrarUsuario = findViewById(R.id.btnFiltrarUsuarioBottom);
        etBuscarUsuario = findViewById(R.id.etBuscarUsuario);

        tvEstablecimientoNombre = findViewById(R.id.tvEstablecimientoNombre);
        if (tvEstablecimientoNombre != null) {
            tvEstablecimientoNombre.setVisibility(filtrarPorEstablecimiento ? View.VISIBLE : View.GONE);
        }
    }

    private void configurarBusqueda() {
        if (etBuscarUsuario != null) {
            etBuscarUsuario.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No necesario
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    textoBusqueda = s.toString().trim();
                    aplicarFiltros();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No necesario
                }
            });
        }
    }

    private void configurarRecyclerView() {
        recyclerViewUsuarios.setLayoutManager(new LinearLayoutManager(this));
        usuarioAdapter = new UsuarioAdapter(this, usuariosFiltrados);
        recyclerViewUsuarios.setAdapter(usuarioAdapter);

        usuarioAdapter.setOnDeleteClickListener((usuario, position) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar Usuario")
                    .setMessage("¿Estás seguro de que deseas eliminar a " + usuario.getNombreCompleto() + "?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        eliminarUsuario(usuario.getId(), position);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        usuarioAdapter.setOnEditClickListener((usuario, position) -> {
            Intent intent = new Intent(this, CrearUsuariosActivity.class);
            intent.putExtra("MODO_EDICION", true);
            intent.putExtra("USUARIO_ID", usuario.getId());
            intent.putExtra("USUARIO_NOMBRE", usuario.getNombre());
            intent.putExtra("USUARIO_APELLIDO", usuario.getApellido());
            intent.putExtra("USUARIO_USERNAME", usuario.getNombreUsuario());
            intent.putExtra("USUARIO_ROL", usuario.getRol());

            if (filtrarPorEstablecimiento) {
                intent.putExtra("ESTABLECIMIENTO_ID", establecimientoSeleccionadoId);
                intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimientoSeleccionadoNombre);
            }

            List<Map<String, Object>> establecimientos = usuario.getEstablecimientos();
            if (establecimientos != null && !establecimientos.isEmpty()) {
                ArrayList<Integer> establecimientoIds = new ArrayList<>();
                for (Map<String, Object> est : establecimientos) {
                    if (est.containsKey("id")) {
                        Object idObj = est.get("id");
                        if (idObj instanceof Number) {
                            establecimientoIds.add(((Number) idObj).intValue());
                        }
                    }
                }
                intent.putIntegerArrayListExtra("USUARIO_ESTABLECIMIENTOS", establecimientoIds);
            }

            startActivity(intent);
        });
    }

    private void configurarBotones() {
        btnVolver.setOnClickListener(v -> finish());

        btnAgregarUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(UsuariosActivity.this, CrearUsuariosActivity.class);
            intent.putExtra("ROL_USUARIO", getIntent().getStringExtra("ROL_USUARIO"));

            if (filtrarPorEstablecimiento) {
                intent.putExtra("ESTABLECIMIENTO_ID", establecimientoSeleccionadoId);
                intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimientoSeleccionadoNombre);
            }

            startActivity(intent);
        });

        btnFiltrarUsuario.setOnClickListener(v -> mostrarDialogoFiltrar());
    }

    private int getNivelRol(String rol) {
        if (rol == null) return Integer.MAX_VALUE;

        rol = rol.trim().toLowerCase(java.util.Locale.ROOT);
        switch (rol) {
            case "admin": return 1;
            case "area manager": return 2;
            case "store manager": return 3;
            case "staff": return 4;
            default: return Integer.MAX_VALUE;
        }
    }

    private void mostrarDialogoFiltrar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filtrar_usuarios, null);
        builder.setView(dialogView);

        RadioGroup radioGroupFiltro = dialogView.findViewById(R.id.radioGroupFiltro);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarFiltro);
        Button btnAplicar = dialogView.findViewById(R.id.btnAplicarFiltro);

        Set<String> rolesUnicos = new HashSet<>();
        rolesUnicos.add("Todos");

        for (Usuario usuario : usuarios) {
            if (usuario.getRol() != null && !usuario.getRol().isEmpty()) {
                rolesUnicos.add(usuario.getRol());
            }
        }

        for (String rol : rolesUnicos) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(rol);
            radioButton.setId(View.generateViewId());
            radioGroupFiltro.addView(radioButton);

            if (rol.equals(filtroRolActual)) {
                radioButton.setChecked(true);
            }
        }

        AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnAplicar.setOnClickListener(v -> {
            int selectedId = radioGroupFiltro.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadio = dialogView.findViewById(selectedId);
                filtroRolActual = selectedRadio.getText().toString();
                aplicarFiltros();
                dialog.dismiss();
            } else {
                Log.e(TAG, "Por favor seleccione un rol");
            }
        });

        dialog.show();
    }

    private void aplicarFiltros() {
        usuariosFiltrados.clear();

        List<Usuario> usuariosPreFiltrados = new ArrayList<>();

        if (filtrarPorEstablecimiento) {
            for (Usuario usuario : usuarios) {
                boolean perteneceAlEstablecimiento = false;
                List<Map<String, Object>> establecimientos = usuario.getEstablecimientos();

                if (establecimientos != null) {
                    for (Map<String, Object> est : establecimientos) {
                        if (est.containsKey("id")) {
                            Object idObj = est.get("id");
                            if (idObj instanceof Number && ((Number) idObj).intValue() == establecimientoSeleccionadoId) {
                                perteneceAlEstablecimiento = true;
                                break;
                            }
                        }
                    }
                }

                if (perteneceAlEstablecimiento) {
                    usuariosPreFiltrados.add(usuario);
                }
            }
        } else {
            usuariosPreFiltrados.addAll(usuarios);
        }

        // Filtro por rol
        List<Usuario> usuariosPorRol = new ArrayList<>();
        if ("Todos".equals(filtroRolActual)) {
            usuariosPorRol.addAll(usuariosPreFiltrados);
        } else {
            for (Usuario usuario : usuariosPreFiltrados) {
                if (filtroRolActual.equals(usuario.getRol())) {
                    usuariosPorRol.add(usuario);
                }
            }
        }

        if (textoBusqueda.isEmpty()) {
            usuariosFiltrados.addAll(usuariosPorRol);
        } else {
            String busquedaLower = textoBusqueda.toLowerCase();

            for (Usuario usuario : usuariosPorRol) {
                boolean coincide = false;

                // Buscar en nombre completo
                if (usuario.getNombreCompleto() != null &&
                        usuario.getNombreCompleto().toLowerCase().contains(busquedaLower)) {
                    coincide = true;
                }

                // Buscar en nombre de usuario
                if (!coincide && usuario.getNombreUsuario() != null &&
                        usuario.getNombreUsuario().toLowerCase().contains(busquedaLower)) {
                    coincide = true;
                }

                // Buscar en establecimientos
                if (!coincide && usuario.getEstablecimientos() != null) {
                    for (Map<String, Object> est : usuario.getEstablecimientos()) {
                        if (est.containsKey("nombre")) {
                            String nombreEst = (String) est.get("nombre");
                            if (nombreEst != null && nombreEst.toLowerCase().contains(busquedaLower)) {
                                coincide = true;
                                break;
                            }
                        }
                    }
                }

                if (coincide) {
                    usuariosFiltrados.add(usuario);
                }
            }
        }

        usuarioAdapter.actualizarLista(usuariosFiltrados);

        // Log de información
        String mensaje = "Mostrando " + usuariosFiltrados.size() + " usuarios";
        if (!textoBusqueda.isEmpty()) {
            mensaje += " que coinciden con: '" + textoBusqueda + "'";
        }
        if (!"Todos".equals(filtroRolActual)) {
            mensaje += " con rol: " + filtroRolActual;
        }
        if (filtrarPorEstablecimiento) {
            mensaje += " en " + establecimientoSeleccionadoNombre;
        }
        Log.d(TAG, mensaje);
    }

    // Mantener el método anterior por compatibilidad, pero que llame al nuevo
    private void aplicarFiltro() {
        aplicarFiltros();
    }

    private void cargarUsuarios() {
        Log.d(TAG, "Iniciando carga de usuarios...");

        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, List<Usuario>>> call = apiService.obtenerUsuarios();

        call.enqueue(new Callback<Map<String, List<Usuario>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Usuario>>> call, Response<Map<String, List<Usuario>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, List<Usuario>> data = response.body();
                    List<Usuario> listaUsuarios = data.get("usuarios");

                    Log.d(TAG, "Usuarios recibidos del servidor: " + (listaUsuarios != null ? listaUsuarios.size() : 0));

                    if (listaUsuarios != null && !listaUsuarios.isEmpty()) {
                        Map<Integer, Usuario> usuariosMap = new HashMap<>();

                        for (Usuario usuario : listaUsuarios) {
                            int id = usuario.getId();

                            if (usuariosMap.containsKey(id)) {
                                Usuario existente = usuariosMap.get(id);
                                List<Map<String, Object>> establecimientos = existente.getEstablecimientos();

                                if (usuario.getEstablecimientoId() > 0 && usuario.getEstablecimientoNombre() != null) {
                                    Map<String, Object> est = new HashMap<>();
                                    est.put("id", usuario.getEstablecimientoId());
                                    est.put("nombre", usuario.getEstablecimientoNombre());

                                    boolean yaExiste = false;
                                    for (Map<String, Object> e : establecimientos) {
                                        if (e.get("id").equals(usuario.getEstablecimientoId())) {
                                            yaExiste = true;
                                            break;
                                        }
                                    }

                                    if (!yaExiste) {
                                        establecimientos.add(est);
                                    }
                                }
                            } else {
                                List<Map<String, Object>> establecimientos = new ArrayList<>();

                                if (usuario.getEstablecimientoId() > 0 && usuario.getEstablecimientoNombre() != null) {
                                    Map<String, Object> est = new HashMap<>();
                                    est.put("id", usuario.getEstablecimientoId());
                                    est.put("nombre", usuario.getEstablecimientoNombre());
                                    establecimientos.add(est);
                                }

                                usuario.setEstablecimientos(establecimientos);
                                usuariosMap.put(id, usuario);
                            }
                        }

                        List<Usuario> usuariosUnicos = new ArrayList<>(usuariosMap.values());
                        Log.d(TAG, "Usuarios únicos después de agrupar: " + usuariosUnicos.size());

                        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        String rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");
                        int nivelActual = getNivelRol(rolUsuario.trim());
                        Log.d("DEBUG_NIVEL", "Mi rol actual: " + rolUsuario + " (nivel " + nivelActual + ")");

                        List<Usuario> visibles = new ArrayList<>();

                        for (Usuario u : usuariosUnicos) {
                            String rol = u.getRol();
                            int nivelUsuario = getNivelRol(rol);
                            Log.d("DEBUG_FILTER", u.getNombreCompleto() + " → " + rol + " (nivel " + nivelUsuario + ")");

                            if (nivelUsuario > nivelActual) {
                                visibles.add(u);
                            }
                        }

                        Log.d("DEBUG_RESULTADO", "Usuarios visibles: " + visibles.size());

                        usuarios.clear();
                        usuarios.addAll(visibles);

                        aplicarFiltros();
                    } else {
                        Log.d(TAG, "No hay usuarios disponibles");
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e(TAG, "Cuerpo del error: " + errorBody);
                        Log.e(TAG, "Error al cargar usuarios: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer cuerpo de error", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Usuario>>> call, Throwable t) {
                Log.e(TAG, "Error en la solicitud", t);
                Log.e(TAG, "Error de conexión: " + t.getMessage());
            }
        });
    }

    private void eliminarUsuario(int usuarioId, int position) {
        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.eliminarUsuario(usuarioId);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resultado = response.body();
                    boolean success = (boolean) resultado.getOrDefault("success", false);

                    if (success) {
                        Log.d(TAG, "Usuario eliminado con éxito");

                        int posicionOriginal = -1;
                        for (int i = 0; i < usuarios.size(); i++) {
                            if (usuarios.get(i).getId() == usuarioId) {
                                posicionOriginal = i;
                                break;
                            }
                        }

                        if (posicionOriginal != -1) {
                            usuarios.remove(posicionOriginal);
                        }

                        if (position >= 0 && position < usuariosFiltrados.size()) {
                            usuariosFiltrados.remove(position);
                            usuarioAdapter.notifyItemRemoved(position);
                            usuarioAdapter.notifyItemRangeChanged(position, usuariosFiltrados.size());
                        }
                    } else {
                        String mensaje = (String) resultado.getOrDefault("message", "Error desconocido");
                        Log.e(TAG, mensaje);
                    }
                } else {
                    Log.e(TAG, "Error al eliminar usuario: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Error en la solicitud", t);
                Log.e(TAG, "Error de conexión: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarios();
    }

    private void iniciarCarga() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        usuarioId = sharedPreferences.getInt("USER_ID", -1);
        String rolUsuario = sharedPreferences.getString("ROL_USUARIO", "");
        int establecimientoId = sharedPreferences.getInt("ESTABLECIMIENTO_ID", -1);

        Log.d(TAG, "Datos cargados: Usuario ID=" + usuarioId +
                ", Rol=" + rolUsuario +
                ", Establecimiento ID=" + establecimientoId);

        if (usuarioId == -1 || rolUsuario.isEmpty()) {
            Log.e(TAG, "Error: No se pudo obtener información del usuario");
            finish();
            return;
        }

        cargarUsuarios();
    }
}