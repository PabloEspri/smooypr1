package com.smooy.smooypr1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.smooy.smooypr1.MenuActivity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText usuarioEditText, contraseñaEditText;
    private Button btnIniciarSesion;
    private TextView mensajeError;

    Conexion conexion;  // Clase que maneja la conexión a la base de datos
    java.sql.Connection con;  // Conexión real de tipo java.sql.Connection
    String str;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Referencias a los elementos de la UI
        usuarioEditText = findViewById(R.id.usuario);
        contraseñaEditText = findViewById(R.id.contraseña);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        mensajeError = findViewById(R.id.mensajeError);

        // Inicializo la conexión
        conexion = new Conexion();
        connect();  // Intento conectar al inicio

        // Ajusto el padding para los insets del sistema (barras de estado y navegación)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Listener para el botón de inicio de sesión
        btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usuario = usuarioEditText.getText().toString().trim();
                String contraseña = contraseñaEditText.getText().toString().trim();

                // Validar si los campos no están vacíos
                if (usuario.isEmpty() || contraseña.isEmpty()) {
                    mensajeError.setVisibility(View.VISIBLE);
                    mensajeError.setText("Por favor, ingresa usuario y contraseña.");
                    return;
                }

                // Aquí puedes agregar la lógica para verificar el usuario y la contraseña desde la base de datos.
                // Para este ejemplo, vamos a hacer una verificación simple:

                if (usuario.equals("admin") && contraseña.equals("admin123")) {
                    // Login exitoso, puedes redirigir a otra actividad
                    Log.d(TAG, "Bienvenido, " + usuario);
                    // Start MainActivity after successful login
                    Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                    intent.putExtra("USER_NAME", usuario);
                    startActivity(intent);
                    finish(); // Close login activity
                } else {
                    // Si el usuario o la contraseña son incorrectos
                    mensajeError.setVisibility(View.VISIBLE);
                    mensajeError.setText("Usuario o contraseña incorrectos.");
                }
            }
        });
    }

    // Método para realizar la conexión a la base de datos
    public void connect() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                // Intento establecer la conexión
                con = conexion.Con();  // Llamo al método Con() de la clase Conexion
                if (con == null) {
                    str = "Error en la conexión MYSQL";  // Si la conexión es nula, muestro un error
                } else {
                    str = "Conexión completa";  // Si la conexión es exitosa, muestro un mensaje de éxito
                }
            } catch (Exception e) {
                str = "Error: " + e.getMessage();  // Capturo cualquier excepción y muestro el mensaje de error
            }

            // Actualizo la UI en el hilo principal
            runOnUiThread(() -> {
                try {
                    Thread.sleep(1000);  // Doy tiempo para que el mensaje se muestre
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Toast removed
                Log.d("MainActivity", str); // Log the connection status instead
            });
        });
    }
}
