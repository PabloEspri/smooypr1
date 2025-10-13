package com.smooy.smooypr1;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.avisos.AvisosActivity;
import com.smooy.smooypr1.procesos.ProcesosActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffMenuActivity extends AppCompatActivity {
    private static final String TAG = "StaffMenuActivity";

    private TextView tvTitulo, tvEstablecimiento, tvRol;
    private Button btnVolver, btnVerProcesos, btnVerAvisos, btnCambiarPassword;
    private int establecimientoId;
    private String establecimientoNombre;
    private int userId;
    private String rol;
    private String usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_menu);

        // Obtener datos del intent
        rol = getIntent().getStringExtra("ROL_USUARIO");
        userId = getIntent().getIntExtra("USER_ID", -1);
        establecimientoId = getIntent().getIntExtra("ESTABLECIMIENTO_ID", -1);
        establecimientoNombre = getIntent().getStringExtra("ESTABLECIMIENTO_NOMBRE");

        // Obtener el usuario actual de SharedPreferences
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        usuarioActual = userPrefs.getString("USUARIO", "");

        Log.d(TAG, "Usuario actual en sesión: " + usuarioActual);
        Log.d(TAG, "UserID: " + userId);

        // Verificar token al iniciar
        verificarToken();

        tvTitulo = findViewById(R.id.tvTituloStaff);
        tvEstablecimiento = findViewById(R.id.tvEstablecimiento);
        btnVolver = findViewById(R.id.btnVolver);
        btnVerProcesos = findViewById(R.id.btnVerProcesos);
        tvRol = findViewById(R.id.tvRol);
        btnVerAvisos = findViewById(R.id.btnVerAvisos);
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword);

        if (rol != null && !rol.isEmpty()) {
            tvRol.setText("Rol: " + rol);
        } else {
            tvRol.setText("Rol: No especificado");
        }

        // Configurar textos
        tvTitulo.setText("Bienvenido Staff");
        if (establecimientoNombre != null && !establecimientoNombre.isEmpty()) {
            tvEstablecimiento.setText("Establecimiento: " + establecimientoNombre);
        } else {
            tvEstablecimiento.setText("Sin establecimiento asignado");
        }

        // Configurar listeners de botones
        configurarBotones();
    }

    private void verificarToken() {
        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = appPrefs.getString("token", "");

        if (token.isEmpty()) {
            Log.e(TAG, "❌ ERROR: No hay token disponible");
        } else {
            Log.d(TAG, "✅ TOKEN disponible: " + token.substring(0, Math.min(15, token.length())) + "...");
        }
    }

    private void configurarBotones() {

        btnVerProcesos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StaffMenuActivity.this, ProcesosActivity.class);

                if (establecimientoId > 0) {
                    intent.putExtra("ESTABLECIMIENTO_ID", establecimientoId);
                }
                startActivity(intent);
            }
        });

        btnVerAvisos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StaffMenuActivity.this, AvisosActivity.class);

                if (establecimientoId > 0) {
                    intent.putExtra("ESTABLECIMIENTO_ID", establecimientoId);
                }
                startActivity(intent);
            }
        });

        btnCambiarPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogCambiarPassword();
            }
        });

        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                appPrefs.edit().clear().apply();

                SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                userPrefs.edit().clear().apply();

                ApiClient.resetClient();

                Intent intent = new Intent(StaffMenuActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void mostrarDialogCambiarPassword() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_cambiar_password, null);

        TextInputEditText etUsuarioDialog = dialogView.findViewById(R.id.etUsuarioDialog);
        TextInputEditText etPasswordActualDialog = dialogView.findViewById(R.id.etPasswordActualDialog);
        TextInputEditText etPasswordNuevaDialog = dialogView.findViewById(R.id.etPasswordNuevaDialog);
        TextInputEditText etPasswordConfirmarDialog = dialogView.findViewById(R.id.etPasswordConfirmarDialog);

        // Pre-rellenar el campo de usuario con el usuario actual
        if (!TextUtils.isEmpty(usuarioActual)) {
            etUsuarioDialog.setText(usuarioActual);
            etUsuarioDialog.setEnabled(false); // Deshabilitar edición para evitar errores
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Cambiar Contraseña")
                .setView(dialogView)
                .setPositiveButton("Cambiar", null) // Se configurará después
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            positiveButton.setOnClickListener(v -> {
                String usuario = etUsuarioDialog.getText().toString().trim();
                String passwordActual = etPasswordActualDialog.getText().toString().trim();
                String passwordNueva = etPasswordNuevaDialog.getText().toString().trim();
                String passwordConfirmar = etPasswordConfirmarDialog.getText().toString().trim();

                // Validar que el usuario coincida con el usuario actual
                if (!TextUtils.isEmpty(usuarioActual) && !usuario.equals(usuarioActual)) {
                    Toast.makeText(this, "El usuario debe coincidir con tu usuario actual: " + usuarioActual,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (validarCamposDialog(usuario, passwordActual, passwordNueva, passwordConfirmar,
                        etUsuarioDialog, etPasswordActualDialog, etPasswordNuevaDialog, etPasswordConfirmarDialog)) {
                    // Deshabilitar botón mientras se procesa
                    positiveButton.setEnabled(false);
                    positiveButton.setText("Cambiando...");

                    cambiarPasswordDialog(passwordActual, passwordNueva, dialog, positiveButton);
                }
            });
        });

        dialog.show();
    }

    private boolean validarCamposDialog(String usuario, String passwordActual, String passwordNueva,
                                        String passwordConfirmar, TextInputEditText etUsuario,
                                        TextInputEditText etPasswordActual, TextInputEditText etPasswordNueva,
                                        TextInputEditText etPasswordConfirmar) {

        // Limpiar errores previos
        etUsuario.setError(null);
        etPasswordActual.setError(null);
        etPasswordNueva.setError(null);
        etPasswordConfirmar.setError(null);

        // Validar que todos los campos estén llenos
        if (TextUtils.isEmpty(usuario)) {
            etUsuario.setError("Por favor ingresa tu usuario actual");
            etUsuario.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(passwordActual)) {
            etPasswordActual.setError("Por favor ingresa tu contraseña actual");
            etPasswordActual.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(passwordNueva)) {
            etPasswordNueva.setError("Por favor ingresa la nueva contraseña");
            etPasswordNueva.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(passwordConfirmar)) {
            etPasswordConfirmar.setError("Por favor confirma la nueva contraseña");
            etPasswordConfirmar.requestFocus();
            return false;
        }

        // Validar que la nueva contraseña tenga al menos 6 caracteres
        if (passwordNueva.length() < 6) {
            etPasswordNueva.setError("La nueva contraseña debe tener al menos 6 caracteres");
            etPasswordNueva.requestFocus();
            return false;
        }

        // Validar que las contraseñas nuevas coincidan
        if (!passwordNueva.equals(passwordConfirmar)) {
            etPasswordConfirmar.setError("Las contraseñas nuevas no coinciden");
            etPasswordConfirmar.requestFocus();
            return false;
        }

        // Validar que la contraseña nueva sea diferente a la actual
        if (passwordActual.equals(passwordNueva)) {
            etPasswordNueva.setError("La nueva contraseña debe ser diferente a la actual");
            etPasswordNueva.requestFocus();
            return false;
        }

        return true;
    }

    // CAMBIO PRINCIPAL: Simplificar los datos enviados
    private void cambiarPasswordDialog(String passwordActual, String passwordNueva,
                                       AlertDialog dialog, Button positiveButton) {

        // Obtener el USER_ID desde SharedPreferences
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = userPrefs.getInt("USER_ID", -1);

        if (userId == -1) {
            Toast.makeText(this, "Error: No se pudo obtener el ID del usuario. Por favor inicia sesión nuevamente.",
                    Toast.LENGTH_LONG).show();
            // Rehabilitar botón
            positiveButton.setEnabled(true);
            positiveButton.setText("Cambiar");
            return;
        }

        // SIMPLIFICADO: Crear el mapa de datos con solo los campos necesarios
        Map<String, Object> datos = new HashMap<>();
        datos.put("usuario_id", userId);
        datos.put("password_actual", passwordActual);
        datos.put("password_nueva", passwordNueva);

        // ELIMINADO: No enviar el nombre de usuario para evitar conflictos
        Log.d(TAG, "=== DATOS ENVIADOS (SIMPLIFICADOS) ===");
        Log.d(TAG, "Usuario ID: " + userId);
        Log.d(TAG, "Password actual length: " + passwordActual.length());
        Log.d(TAG, "Password nueva length: " + passwordNueva.length());

        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.cambiarPassword(datos);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Rehabilitar el botón
                positiveButton.setEnabled(true);
                positiveButton.setText("Cambiar");

                Log.d(TAG, "=== RESPUESTA DEL SERVIDOR ===");
                Log.d(TAG, "Código de respuesta: " + response.code());
                Log.d(TAG, "Es exitosa: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resultado = response.body();
                    boolean success = (boolean) resultado.getOrDefault("success", false);
                    String mensaje = (String) resultado.getOrDefault("message", "");

                    Log.d(TAG, "Success: " + success);
                    Log.d(TAG, "Mensaje: " + mensaje);

                    if (success) {
                        Log.d(TAG, "✅ Contraseña cambiada exitosamente");
                        Toast.makeText(StaffMenuActivity.this, "Contraseña cambiada exitosamente",
                                Toast.LENGTH_LONG).show();

                        dialog.dismiss();

                        // Mostrar mensaje de que debe iniciar sesión nuevamente
                        new AlertDialog.Builder(StaffMenuActivity.this)
                                .setTitle("Contraseña Actualizada")
                                .setMessage("Tu contraseña ha sido actualizada exitosamente. Por seguridad, debes iniciar sesión nuevamente.")
                                .setPositiveButton("Aceptar", (dialogInterface, which) -> {
                                    // Cerrar sesión y volver al login
                                    cerrarSesionYVolver();
                                })
                                .setCancelable(false)
                                .show();

                    } else {
                        Log.e(TAG, "❌ Error al cambiar contraseña: " + mensaje);

                        // MEJORADO: Mensaje de error más específico
                        String mensajeError = mensaje;
                        if (mensaje.toLowerCase().contains("password") ||
                                mensaje.toLowerCase().contains("contraseña")) {
                            mensajeError = "La contraseña actual es incorrecta";
                        } else if (mensaje.isEmpty()) {
                            mensajeError = "Error al cambiar la contraseña";
                        }

                        Toast.makeText(StaffMenuActivity.this, mensajeError, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "❌ Error en la respuesta del servidor: " + response.code());
                    String mensajeError;

                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Cuerpo del error: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al leer cuerpo de error", e);
                    }

                    // Personalizar mensajes según el código de error
                    switch (response.code()) {
                        case 401:
                            mensajeError = "La contraseña actual es incorrecta";
                            break;
                        case 404:
                            mensajeError = "Usuario no encontrado";
                            break;
                        case 422:
                            mensajeError = "La contraseña actual no es válida";
                            break;
                        case 500:
                            mensajeError = "Error interno del servidor";
                            break;
                        default:
                            mensajeError = "Error al procesar la solicitud (Código: " + response.code() + ")";
                    }

                    Toast.makeText(StaffMenuActivity.this, mensajeError, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Rehabilitar el botón
                positiveButton.setEnabled(true);
                positiveButton.setText("Cambiar");

                Log.e(TAG, "❌ Error de conexión", t);
                String mensajeError = "Error de conexión: " +
                        (t.getMessage() != null ? t.getMessage() : "Revisa tu conexión a internet");
                Toast.makeText(StaffMenuActivity.this, mensajeError, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cerrarSesionYVolver() {
        Log.d(TAG, "🧹 Cerrando sesión y limpiando datos");

        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        appPrefs.edit().clear().apply();

        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userPrefs.edit().clear().apply();

        ApiClient.resetClient();

        Intent intent = new Intent(StaffMenuActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarToken();
    }
}