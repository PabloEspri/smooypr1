package com.smooy.smooypr1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.smooy.smooypr1.avisos.AvisosActivity;
import com.smooy.smooypr1.procesos.ProcesosActivity;
import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.usuarios.UsuariosActivity;
import com.smooy.smooypr1.establecimientos.EstablecimientosActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends AppCompatActivity {

    private Button btnProcesos, btnAvisos, cierreSesion, btnCrearUsuarios, btnEstablecimientos, btnCambiarContrasena;
    private static final String TAG = "MenuActivity";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        String rol = getIntent().getStringExtra("ROL_USUARIO");
        if (!"Admin".equalsIgnoreCase(rol) && !"Area Manager".equalsIgnoreCase(rol)) {
            Log.e("MenuActivity", "Acceso no autorizado. Rol: " + rol);
            finish();
            return;
        }

        btnProcesos = findViewById(R.id.btnProcesos);
        btnAvisos = findViewById(R.id.btnAvisos);
        cierreSesion = findViewById(R.id.cierreSesion);
        btnCrearUsuarios = findViewById(R.id.btnCrearUsuarios);
        btnEstablecimientos = findViewById(R.id.btnEstablecimientos);
        btnCambiarContrasena = findViewById(R.id.btnCambiarContrasena);

        btnCrearUsuarios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, UsuariosActivity.class);
                intent.putExtra("ROL_USUARIO", rol);
                startActivity(intent);
            }
        });

        btnEstablecimientos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, EstablecimientosActivity.class);
                startActivity(intent);
            }
        });

        btnProcesos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, ProcesosActivity.class);
                startActivity(intent);
            }
        });

        btnAvisos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, AvisosActivity.class);
                startActivity(intent);
            }
        });

        btnCambiarContrasena.setOnClickListener(v -> mostrarDialogoCambiarPassword());

        cierreSesion.setOnClickListener(v -> logout());
    }

    private void mostrarDialogoCambiarPassword() {
        // Crear el layout del diálogo
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cambiar_password, null);

        EditText etUsuario = dialogView.findViewById(R.id.etUsuarioDialog);
        EditText etPasswordActual = dialogView.findViewById(R.id.etPasswordActualDialog);
        EditText etPasswordNueva = dialogView.findViewById(R.id.etPasswordNuevaDialog);
        EditText etPasswordConfirmar = dialogView.findViewById(R.id.etPasswordConfirmarDialog);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle("Cambiar Contraseña")
                .setView(dialogView)
                .setPositiveButton("Cambiar", null) // Establecemos null primero
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        dialog.show();

        // Configurar el botón después de mostrar el diálogo para evitar que se cierre automáticamente
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String usuario = etUsuario.getText().toString().trim();
            String passwordActual = etPasswordActual.getText().toString().trim();
            String passwordNueva = etPasswordNueva.getText().toString().trim();
            String passwordConfirmar = etPasswordConfirmar.getText().toString().trim();

            Log.d(TAG, "=== DATOS CAPTURADOS DEL DIÁLOGO ===");
            Log.d(TAG, "Usuario: '" + usuario + "'");
            Log.d(TAG, "Password actual: [CENSURADA] (longitud: " + passwordActual.length() + ")");
            Log.d(TAG, "Password nueva: [CENSURADA] (longitud: " + passwordNueva.length() + ")");
            Log.d(TAG, "Password confirmar: [CENSURADA] (longitud: " + passwordConfirmar.length() + ")");

            if (validarCamposCambioPassword(usuario, passwordActual, passwordNueva, passwordConfirmar)) {
                // CAMBIO IMPORTANTE: Primero validamos las credenciales actuales
                validarYCambiarPassword(usuario, passwordActual, passwordNueva, dialog);
            }
        });
    }

    private boolean validarCamposCambioPassword(String usuario, String passwordActual, String passwordNueva, String passwordConfirmar) {
        Log.d(TAG, "=== INICIANDO VALIDACIÓN ===");

        // Limpiar posibles caracteres invisibles
        usuario = usuario != null ? usuario.trim().replaceAll("\\p{C}", "") : "";
        passwordActual = passwordActual != null ? passwordActual.trim().replaceAll("\\p{C}", "") : "";
        passwordNueva = passwordNueva != null ? passwordNueva.trim().replaceAll("\\p{C}", "") : "";
        passwordConfirmar = passwordConfirmar != null ? passwordConfirmar.trim().replaceAll("\\p{C}", "") : "";

        // Validación de campos vacíos
        if (usuario.isEmpty()) {
            mostrarMensajeError("Por favor ingresa tu usuario");
            return false;
        }

        if (passwordActual.isEmpty()) {
            mostrarMensajeError("Por favor ingresa tu contraseña actual");
            return false;
        }

        if (passwordNueva.isEmpty()) {
            mostrarMensajeError("Por favor ingresa la nueva contraseña");
            return false;
        }

        if (passwordConfirmar.isEmpty()) {
            mostrarMensajeError("Por favor confirma la nueva contraseña");
            return false;
        }

        // Validación de longitud
        if (passwordNueva.length() < 6) {
            mostrarMensajeError("La nueva contraseña debe tener al menos 6 caracteres");
            return false;
        }

        // Validación de coincidencia
        if (!passwordNueva.equals(passwordConfirmar)) {
            mostrarMensajeError("Las contraseñas nuevas no coinciden");
            return false;
        }

        // Validación de diferencia con contraseña actual
        if (passwordActual.equals(passwordNueva)) {
            mostrarMensajeError("La nueva contraseña debe ser diferente a la actual");
            return false;
        }

        Log.d(TAG, "✅ Todas las validaciones pasaron correctamente");
        return true;
    }

    /**
     * NUEVO MÉTODO: Valida primero las credenciales actuales antes de cambiar
     */
    private void validarYCambiarPassword(String usuario, String passwordActual, String passwordNueva, AlertDialog dialog) {
        Log.d(TAG, "=== VALIDANDO CREDENCIALES ACTUALES ===");

        // Crear datos para validación
        Map<String, Object> datosValidacion = new HashMap<>();
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = userPrefs.getInt("USER_ID", -1);

        // Agregar tanto el usuario como el userId para validación robusta
        datosValidacion.put("usuario_id", userId);
        datosValidacion.put("usuario", usuario);
        datosValidacion.put("password_actual", passwordActual);
        datosValidacion.put("password_nueva", passwordNueva);
        datosValidacion.put("validar_credenciales", true); // Flag para el backend

        Log.d(TAG, "Validando credenciales para usuario ID: " + userId + ", usuario: " + usuario);

        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.cambiarPassword(datosValidacion);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resultado = response.body();
                    boolean success = (boolean) resultado.getOrDefault("success", false);
                    String mensaje = (String) resultado.getOrDefault("message", "");

                    if (success) {
                        Log.d(TAG, "✅ Contraseña cambiada exitosamente");

                        // Limpiar completamente la sesión
                        limpiarSesionCompleta();

                        // Cerrar el diálogo de cambio de contraseña
                        dialog.dismiss();

                        // Mostrar mensaje de éxito y forzar re-login
                        new AlertDialog.Builder(MenuActivity.this)
                                .setTitle("Contraseña Cambiada")
                                .setMessage("Tu contraseña ha sido cambiada exitosamente.\n\nPor seguridad, debes iniciar sesión con tu nueva contraseña.")
                                .setPositiveButton("Iniciar Sesión", (d, which) -> {
                                    Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .setCancelable(false)
                                .show();

                    } else {
                        Log.e(TAG, "❌ Error: " + mensaje);
                        // Mostrar error específico del servidor
                        if (mensaje.toLowerCase().contains("credenciales") ||
                                mensaje.toLowerCase().contains("contraseña") ||
                                mensaje.toLowerCase().contains("usuario")) {
                            mostrarMensajeError("Credenciales incorrectas: " + mensaje);
                        } else {
                            mostrarMensajeError(mensaje.isEmpty() ? "Error al cambiar la contraseña" : mensaje);
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Error HTTP: " + response.code());

                    // Log detallado del error
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer error body", e);
                    }

                    String mensajeError = obtenerMensajeError(response.code());
                    mostrarMensajeError(mensajeError);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "❌ Error de conexión al cambiar contraseña", t);
                mostrarMensajeError("Error de conexión: " +
                        (t.getMessage() != null ? t.getMessage() : "Revisa tu conexión a internet"));
            }
        });
    }

    /**
     * Limpia completamente la sesión del usuario
     */
    private void limpiarSesionCompleta() {
        Log.d(TAG, "🧹 Limpiando sesión completa del usuario");

        // Limpiar SharedPreferences de la app
        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        appPrefs.edit().clear().apply();

        // Limpiar SharedPreferences del usuario
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userPrefs.edit().clear().apply();

        // Resetear el cliente API (esto limpia el token del interceptor)
        ApiClient.resetClient();

        Log.d(TAG, "✅ Sesión limpiada completamente");
    }

    /**
     * Obtiene mensaje de error personalizado según el código HTTP
     */
    private String obtenerMensajeError(int codigoError) {
        switch (codigoError) {
            case 401:
                return "Usuario o contraseña actual incorrectos";
            case 403:
                return "No tienes permisos para cambiar la contraseña";
            case 404:
                return "Usuario no encontrado";
            case 422:
                return "Datos inválidos. Verifica que el usuario y contraseña actual sean correctos.";
            case 500:
                return "Error interno del servidor";
            default:
                return "Error al procesar la solicitud (Código: " + codigoError + ")";
        }
    }

    /**
     * LOGOUT MEJORADO
     */
    private void logout() {
        Log.d(TAG, "🚪 Iniciando cierre de sesión");

        // Limpiar sesión completa
        limpiarSesionCompleta();

        // Mostrar mensaje de confirmación
        new AlertDialog.Builder(this)
                .setTitle("Sesión Cerrada")
                .setMessage("Has cerrado sesión correctamente")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Redirigir al login
                    Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void mostrarMensajeError(String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }

    private void mostrarMensajeExito(String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle("Éxito")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }
}