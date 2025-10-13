package com.smooy.smooypr1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.API.LoginRequest;
import com.smooy.smooypr1.API.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText edtUsuario, edtContraseña;
    private TextView mensajeError;
    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtUsuario = findViewById(R.id.usuario);
        edtContraseña = findViewById(R.id.contraseña);
        mensajeError = findViewById(R.id.mensajeError);
        requestAllPermissions();
    }

    private void requestAllPermissions() {
        Log.d(TAG, "Iniciando solicitud de permisos");
        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
        }

        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Solicitando " + permissionsToRequest.size() + " permisos");
            requestPermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            Log.d(TAG, "Todos los permisos ya están concedidos");
            checkExistingSession();
        }
    }

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> permissions) {
                    boolean allGranted = true;
                    for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                        String permission = entry.getKey();
                        boolean granted = entry.getValue();
                        Log.d(TAG, "Permiso " + permission + ": " + (granted ? "CONCEDIDO" : "DENEGADO"));
                        if (!granted) {
                            allGranted = false;
                        }
                    }

                    if (allGranted) {
                        Log.d(TAG, "✅ Todos los permisos concedidos");
                        checkExistingSession();
                    } else {
                        Log.d(TAG, "❌ Algunos permisos denegados");
                        showPermissionExplanationDialog();
                    }
                }
            });

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage("Esta aplicación necesita acceso a la cámara y almacenamiento para funcionar correctamente. Por favor, concede todos los permisos solicitados.")
                .setPositiveButton("Reintentar", (dialog, which) -> requestAllPermissions())
                .setNegativeButton("Continuar con limitaciones", (dialog, which) -> {
                    checkExistingSession();
                })
                .setCancelable(false)
                .show();
    }

    private void checkExistingSession() {
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = preferences.getInt("USER_ID", -1);
        String rolUsuario = preferences.getString("ROL_USUARIO", "");

        if (userId != -1 && rolUsuario != null && !rolUsuario.isEmpty()) {
            Intent intent;

            if ("Staff".equalsIgnoreCase(rolUsuario) ||
                    rolUsuario.toLowerCase().contains("staff") ||
                    rolUsuario.equalsIgnoreCase("Store Manager")) {
                intent = new Intent(this, StaffMenuActivity.class);

                int establecimientoId = preferences.getInt("ESTABLECIMIENTO_ID", -1);
                String establecimientoNombre = preferences.getString("ESTABLECIMIENTO_NOMBRE", "No especificado");
                intent.putExtra("ESTABLECIMIENTO_ID", establecimientoId);
                intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimientoNombre);
            } else {

                intent = new Intent(this, MenuActivity.class);
            }
            intent.putExtra("ROL_USUARIO", rolUsuario);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        }
    }

    public void btnLoginClick(View view) {

        String usuario = edtUsuario.getText().toString().trim();
        String contraseña = edtContraseña.getText().toString().trim();

        if (usuario.isEmpty() || contraseña.isEmpty()) {
            mensajeError.setVisibility(View.VISIBLE);
            mensajeError.setText("Usuario o contraseña vacíos");
            return;
        }

        LoginRequest loginRequest = new LoginRequest(usuario, contraseña);
        ApiService apiService = ApiClient.getApiService();

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    Log.d("MainActivity", "Respuesta login: success=" + loginResponse.isSuccess() +
                            ", mensaje=" + loginResponse.getMessage());

                    if (loginResponse.isSuccess()) {
                        String token = loginResponse.getToken();
                        if (token == null || token.isEmpty()) {
                            mensajeError.setVisibility(View.VISIBLE);
                            mensajeError.setText("Error: No se recibió un token válido");
                            Log.e("MainActivity", "Token recibido nulo o vacío");
                            return;
                        }

                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        prefs.edit().putString("token", token).apply();

                        String savedToken = prefs.getString("token", "");
                        Log.d("MainActivity", "Token guardado: " + (savedToken.isEmpty() ? "NO" : "SÍ"));

                        loginExitoso(loginResponse);
                    } else {
                        mensajeError.setVisibility(View.VISIBLE);
                        mensajeError.setText(loginResponse.getMessage());
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Error desconocido";
                        Log.e("MainActivity", "Error " + response.code() + ": " + errorBody);
                        mensajeError.setVisibility(View.VISIBLE);
                        mensajeError.setText("Error en la autenticación: " + response.code());
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error al leer error body", e);
                        mensajeError.setVisibility(View.VISIBLE);
                        mensajeError.setText("Error en la autenticación: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("MainActivity", "Fallo en la petición", t);
                mensajeError.setVisibility(View.VISIBLE);
                mensajeError.setText("Fallo de conexión: " + t.getMessage());
            }
        });
    }

    private void loginExitoso(LoginResponse respuesta) {
        String rol = respuesta.getRol();
        int userId = respuesta.getUserId();
        String token = respuesta.getToken();

        if (token == null || token.isEmpty()) {
            Log.e("MainActivity", "Error: Token recibido es nulo o vacío");
            mensajeError.setVisibility(View.VISIBLE);
            mensajeError.setText("Error: No se recibió un token válido");
            return;
        }

        SharedPreferences appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        appPrefs.edit()
                .putString("token", token)
                .apply();

        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userPrefs.edit()
                .putString("token", token)
                .putString("ROL_USUARIO", rol)
                .putInt("USER_ID", userId)
                .apply();

        Log.d("MainActivity", "✅ Token guardado en AppPrefs y UserPrefs");
        Log.d("MainActivity", "Rol del usuario: " + rol);

        ApiClient.resetClient();

        if (rol.equalsIgnoreCase("Admin") || rol.equalsIgnoreCase("Area Manager")) {
            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
            intent.putExtra("ROL_USUARIO", rol);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        } else if (rol.toLowerCase().contains("staff") || rol.equalsIgnoreCase("Store Manager")) {

            obtenerEstablecimientoStaff(userId, rol);
        } else {
            Log.e("MainActivity", "Rol no reconocido: " + rol);
            mensajeError.setVisibility(View.VISIBLE);
            mensajeError.setText("Error: Rol de usuario no reconocido");
        }

    }

    private void obtenerEstablecimientoStaff(int userId, String rol) {
        final int finalUserId = userId;
        final String finalRol = rol;

        ApiService apiService = ApiClient.getApiService();

        Call<Map<String, List<Establecimientos>>> call = apiService.obtenerEstablecimientosUsuario(userId);

        Log.d("MainActivity", "Obteniendo establecimientos para usuario con ID: " + userId);

        call.enqueue(new Callback<Map<String, List<Establecimientos>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Establecimientos>>> call, Response<Map<String, List<Establecimientos>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, List<Establecimientos>> data = response.body();
                    List<Establecimientos> establecimientos = data.get("establecimientos");

                    if (establecimientos != null && !establecimientos.isEmpty()) {
                        Log.d("MainActivity", "Se encontraron " + establecimientos.size() + " establecimientos para el usuario");

                        Establecimientos establecimiento = establecimientos.get(0);

                        Log.d("MainActivity", "Seleccionado establecimiento: " + establecimiento.getNombre() + " (ID: " + establecimiento.getId() + ")");

                        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        userPrefs.edit()
                                .putInt("ESTABLECIMIENTO_ID", establecimiento.getId())
                                .putString("ESTABLECIMIENTO_NOMBRE", establecimiento.getNombre())
                                .apply();

                        Intent intent = new Intent(MainActivity.this, StaffMenuActivity.class);
                        intent.putExtra("ROL_USUARIO", finalRol);
                        intent.putExtra("USER_ID", finalUserId);
                        intent.putExtra("ESTABLECIMIENTO_ID", establecimiento.getId());
                        intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimiento.getNombre());
                        startActivity(intent);
                        finish();
                    } else {

                        Log.e("MainActivity", "No se encontraron establecimientos para el usuario " + finalUserId);

                        Intent intent = new Intent(MainActivity.this, StaffMenuActivity.class);
                        intent.putExtra("ROL_USUARIO", finalRol);
                        intent.putExtra("USER_ID", finalUserId);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Log.e("MainActivity", "Error al obtener establecimientos: " + response.code());
                    Intent intent = new Intent(MainActivity.this, StaffMenuActivity.class);
                    intent.putExtra("ROL_USUARIO", finalRol);
                    intent.putExtra("USER_ID", finalUserId);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Establecimientos>>> call, Throwable t) {
                Log.e("MainActivity", "Fallo al obtener establecimientos", t);

                Intent intent = new Intent(MainActivity.this, StaffMenuActivity.class);
                intent.putExtra("ROL_USUARIO", finalRol);
                intent.putExtra("USER_ID", finalUserId);
                startActivity(intent);
                finish();
            }
        });
    }
}