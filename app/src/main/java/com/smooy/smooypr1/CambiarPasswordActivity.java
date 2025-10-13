package com.smooy.smooypr1;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CambiarPasswordActivity extends AppCompatActivity {

    private static final String TAG = "CambiarPasswordActivity";

    private TextInputEditText etUsuarioActual;
    private TextInputEditText etPasswordActual;
    private TextInputEditText etPasswordNueva;
    private TextInputEditText etPasswordConfirmar;
    private MaterialButton btnCambiarPassword;
    private MaterialButton btnCancelar;
    private TextView mensajeResultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_layout);

        inicializarVistas();
        configurarBotones();
    }

    private void inicializarVistas() {
        etUsuarioActual = findViewById(R.id.etUsuarioActual);
        etPasswordActual = findViewById(R.id.etPasswordActual);
        etPasswordNueva = findViewById(R.id.etPasswordNueva);
        etPasswordConfirmar = findViewById(R.id.etPasswordConfirmar);
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword);
        btnCancelar = findViewById(R.id.btnCancelar);
        mensajeResultado = findViewById(R.id.mensajeResultado);
    }

    private void configurarBotones() {
        btnCancelar.setOnClickListener(v -> {
            finish();
        });

        btnCambiarPassword.setOnClickListener(v -> {
            if (validarCampos()) {
                cambiarPassword();
            }
        });
    }

    private boolean validarCampos() {
        String usuario = etUsuarioActual.getText().toString().trim();
        String passwordActual = etPasswordActual.getText().toString().trim();
        String passwordNueva = etPasswordNueva.getText().toString().trim();
        String passwordConfirmar = etPasswordConfirmar.getText().toString().trim();

        // Validar que todos los campos estén llenos
        if (TextUtils.isEmpty(usuario)) {
            mostrarMensaje("Por favor ingresa tu usuario actual", true);
            etUsuarioActual.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(passwordActual)) {
            mostrarMensaje("Por favor ingresa tu contraseña actual", true);
            etPasswordActual.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(passwordNueva)) {
            mostrarMensaje("Por favor ingresa la nueva contraseña", true);
            etPasswordNueva.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(passwordConfirmar)) {
            mostrarMensaje("Por favor confirma la nueva contraseña", true);
            etPasswordConfirmar.requestFocus();
            return false;
        }

        // Validar que la nueva contraseña tenga al menos 6 caracteres
        if (passwordNueva.length() < 6) {
            mostrarMensaje("La nueva contraseña debe tener al menos 6 caracteres", true);
            etPasswordNueva.requestFocus();
            return false;
        }

        // Validar que las contraseñas nuevas coincidan
        if (!passwordNueva.equals(passwordConfirmar)) {
            mostrarMensaje("Las contraseñas nuevas no coinciden", true);
            etPasswordConfirmar.requestFocus();
            return false;
        }

        // Validar que la contraseña nueva sea diferente a la actual
        if (passwordActual.equals(passwordNueva)) {
            mostrarMensaje("La nueva contraseña debe ser diferente a la actual", true);
            etPasswordNueva.requestFocus();
            return false;
        }

        return true;
    }

    private void cambiarPassword() {
        String usuario = etUsuarioActual.getText().toString().trim();
        String passwordActual = etPasswordActual.getText().toString().trim();
        String passwordNueva = etPasswordNueva.getText().toString().trim();

        // Obtener el USER_ID desde SharedPreferences
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = userPrefs.getInt("USER_ID", -1);

        if (userId == -1) {
            mostrarMensaje("Error: No se pudo obtener el ID del usuario. Por favor inicia sesión nuevamente.", true);
            return;
        }

        // Deshabilitar el botón mientras se procesa
        btnCambiarPassword.setEnabled(false);
        btnCambiarPassword.setText("Cambiando...");

        // Crear el mapa de datos para enviar
        Map<String, Object> datos = new HashMap<>();
        datos.put("usuario_id", userId);
        datos.put("password_actual", passwordActual);
        datos.put("password_nueva", passwordNueva);

        Log.d(TAG, "Enviando datos: usuario_id=" + userId + ", usuario_ingresado=" + usuario);

        ApiService apiService = ApiClient.getApiService();
        Call<Map<String, Object>> call = apiService.cambiarPassword(datos);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Rehabilitar el botón
                btnCambiarPassword.setEnabled(true);
                btnCambiarPassword.setText("Cambiar");

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> resultado = response.body();
                    boolean success = (boolean) resultado.getOrDefault("success", false);
                    String mensaje = (String) resultado.getOrDefault("message", "");

                    if (success) {
                        Log.d(TAG, "Contraseña cambiada exitosamente");
                        mostrarMensaje("Contraseña cambiada exitosamente", false);

                        // Limpiar los campos después de un cambio exitoso
                        limpiarCampos();

                        // Opcional: cerrar la actividad después de un delay
                        mensajeResultado.postDelayed(() -> {
                            Toast.makeText(CambiarPasswordActivity.this,
                                    "Contraseña actualizada. Por favor inicia sesión nuevamente.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }, 2000);

                    } else {
                        Log.e(TAG, "Error al cambiar contraseña: " + mensaje);
                        mostrarMensaje(mensaje.isEmpty() ? "Error al cambiar la contraseña" : mensaje, true);
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta del servidor: " + response.code());
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
                            mensajeError = "Usuario o contraseña actual incorrectos";
                            break;
                        case 404:
                            mensajeError = "Usuario no encontrado";
                            break;
                        case 422:
                            mensajeError = "Datos inválidos. Verifica que el usuario y contraseña actual sean correctos.";
                            break;
                        case 500:
                            mensajeError = "Error interno del servidor";
                            break;
                        default:
                            mensajeError = "Error al procesar la solicitud (Código: " + response.code() + ")";
                    }

                    mostrarMensaje(mensajeError, true);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Rehabilitar el botón
                btnCambiarPassword.setEnabled(true);
                btnCambiarPassword.setText("Cambiar");

                Log.e(TAG, "Error de conexión", t);
                String mensajeError = "Error de conexión: " +
                        (t.getMessage() != null ? t.getMessage() : "Revisa tu conexión a internet");
                mostrarMensaje(mensajeError, true);
            }
        });
    }

    private void mostrarMensaje(String mensaje, boolean esError) {
        mensajeResultado.setVisibility(View.VISIBLE);
        mensajeResultado.setText(mensaje);

        if (esError) {
            mensajeResultado.setTextColor(ContextCompat.getColor(this, R.color.errorColor));
            mensajeResultado.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert),
                    null, null, null
            );
        } else {
            mensajeResultado.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            mensajeResultado.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_info),
                    null, null, null
            );
        }

        // Ocultar el mensaje después de 5 segundos si es de éxito
        if (!esError) {
            mensajeResultado.postDelayed(() -> {
                if (mensajeResultado != null) {
                    mensajeResultado.setVisibility(View.GONE);
                }
            }, 5000);
        }
    }

    private void limpiarCampos() {
        etUsuarioActual.setText("");
        etPasswordActual.setText("");
        etPasswordNueva.setText("");
        etPasswordConfirmar.setText("");
    }
}