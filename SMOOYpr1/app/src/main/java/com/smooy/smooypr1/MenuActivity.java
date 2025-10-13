package com.smooy.smooypr1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private TextView displayTextView;
    private Button btnProcesos, btnAvisos, btnAuditorias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        displayTextView = findViewById(R.id.displayTextView);
        btnProcesos = findViewById(R.id.btnProcesos);
        btnAvisos = findViewById(R.id.btnAvisos);
        btnAuditorias = findViewById(R.id.btnAuditorias);

        btnProcesos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTextView.setText("Menú de procesos");
            }
        });

        btnAvisos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTextView.setText("Menú de avisos");
            }
        });

        btnAuditorias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTextView.setText("Menú de auditorias");
            }
        });
    }
}