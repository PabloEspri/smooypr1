package com.smooy.smooypr1.establecimientos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.smooy.smooypr1.Establecimientos;
import com.smooy.smooypr1.R;
import com.smooy.smooypr1.avisos.AvisosActivity;
import com.smooy.smooypr1.procesos.ProcesosActivity;
import com.smooy.smooypr1.usuarios.UsuariosActivity;
import com.google.android.material.button.MaterialButton;

public class EstablecimientoDetalleActivity extends AppCompatActivity {

    private static final String TAG = "EstablecimientoDetalle";

    private Establecimientos establecimiento;
    private TextView tvNombre, tvDireccion, tvTipo, tvEstado;
    private MaterialButton btnProcesos, btnAvisos, btnMiembros, btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_establecimiento_detalle);

        if (getIntent().hasExtra("ESTABLECIMIENTO_ID")) {
            int establecimientoId = getIntent().getIntExtra("ESTABLECIMIENTO_ID", -1);
            String nombre = getIntent().getStringExtra("ESTABLECIMIENTO_NOMBRE");
            String direccion = getIntent().getStringExtra("ESTABLECIMIENTO_DIRECCION");
            String tipo = getIntent().getStringExtra("ESTABLECIMIENTO_TIPO");
            String estado = getIntent().getStringExtra("ESTABLECIMIENTO_ESTADO");

            establecimiento = new Establecimientos(establecimientoId, nombre, direccion, tipo, estado);
        } else {
            Toast.makeText(this, "Error: No se pudo cargar la información del establecimiento", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarVistas();
        configurarDatos();
        configurarBotones();
    }

    private void inicializarVistas() {
        tvNombre = findViewById(R.id.tvNombreEstablecimientoDetalle);
        tvDireccion = findViewById(R.id.tvDireccionEstablecimientoDetalle);
        tvTipo = findViewById(R.id.tvTipoEstablecimientoDetalle);
        tvEstado = findViewById(R.id.tvEstadoEstablecimientoDetalle);

        btnProcesos = findViewById(R.id.btnProcesos);
        btnAvisos = findViewById(R.id.btnAvisos);
        btnMiembros = findViewById(R.id.btnMiembros);
        btnVolver = findViewById(R.id.btnVolverEstablecimientoDetalle);
    }

    private void configurarDatos() {
        tvNombre.setText(establecimiento.getNombre());
        tvDireccion.setText(establecimiento.getDireccion());
        tvTipo.setText("Tipo: " + establecimiento.getTipo());
        tvEstado.setText("Estado: " + establecimiento.getEstado());
    }

    private void configurarBotones() {
        btnVolver.setOnClickListener(v -> finish());

        btnProcesos.setOnClickListener(v -> {
            Intent intent = new Intent(EstablecimientoDetalleActivity.this, ProcesosActivity.class);
            intent.putExtra("ESTABLECIMIENTO_ID", establecimiento.getId());
            intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimiento.getNombre());
            intent.putExtra("DESDE_ESTABLECIMIENTO", true); // Añadir este indicador
            startActivity(intent);
        });

        btnAvisos.setOnClickListener(v -> {
            Intent intent = new Intent(EstablecimientoDetalleActivity.this, AvisosActivity.class);
            intent.putExtra("ESTABLECIMIENTO_ID", establecimiento.getId());
            intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimiento.getNombre());
            startActivity(intent);
        });

        btnMiembros.setOnClickListener(v -> {
            Intent intent = new Intent(EstablecimientoDetalleActivity.this, UsuariosActivity.class);
            intent.putExtra("ESTABLECIMIENTO_ID", establecimiento.getId());
            intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimiento.getNombre());
            startActivity(intent);
        });
    }
}