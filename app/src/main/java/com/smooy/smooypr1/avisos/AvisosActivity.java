package com.smooy.smooypr1.avisos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.button.MaterialButton;
import com.smooy.smooypr1.API.ApiClient;
import com.smooy.smooypr1.API.ApiService;
import com.smooy.smooypr1.Establecimientos;
import com.smooy.smooypr1.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvisosActivity extends AppCompatActivity {
    private static final String TAG = "AvisosActivity";
    private RecyclerView recyclerView;
    private AvisosAdapter avisosAdapter;
    private MaterialButton btnVolver, btnAgregarAviso, btnFiltrarAvisos;
    private TextView tvTitulo;

    private boolean esStaff = false;
    private int establecimientoId = -1;
    private boolean esAdmin = false;
    private String establecimientoNombre = "";
    private boolean desdeEstablecimiento = false;

    // Lista para almacenar todos los avisos originales
    private final List<Aviso> avisosOriginales = new ArrayList<>();
    // Lista para mostrar avisos filtrados
    private final List<Aviso> avisos = new ArrayList<>();
    // Lista para almacenar los establecimientos disponibles
    private final List<Establecimientos> listaEstablecimientos = new ArrayList<>();

    // Variables para el filtrado
    private String filtroEstablecimiento = "Todos";
    private String filtroCategoria = "Todas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avisos_layout);

        obtenerInformacionUsuario();
        procesarIntentDatos();
        inicializarComponentes();
        configurarAdaptador();

        // Cargar establecimientos si el usuario es Admin
        if (esAdmin) {
            cargarEstablecimientos();
        }
    }

    private void obtenerInformacionUsuario() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String rol = sharedPreferences.getString("ROL_USUARIO", "");

        esAdmin = "Admin".equals(rol);
        esStaff = "Staff".equals(rol) || esAdmin;

        // Solo usamos el establecimiento de las preferencias si no viene uno específico en el intent
        if (getIntent().getIntExtra("ESTABLECIMIENTO_ID", -1) == -1) {
            establecimientoId = sharedPreferences.getInt("ESTABLECIMIENTO_ID", -1);
        }

        Log.d(TAG, "¿Es usuario admin?: " + esAdmin);
        Log.d(TAG, "¿Es usuario staff?: " + esStaff);
        Log.d(TAG, "ID de establecimiento: " + establecimientoId);
    }

    private void procesarIntentDatos() {
        Intent intent = getIntent();

        // Verificar si viene de un establecimiento específico
        if (intent.hasExtra("ESTABLECIMIENTO_ID")) {
            establecimientoId = intent.getIntExtra("ESTABLECIMIENTO_ID", -1);
            establecimientoNombre = intent.getStringExtra("ESTABLECIMIENTO_NOMBRE");
            desdeEstablecimiento = true;

            // Si viene de un establecimiento específico, establecer el filtro
            if (establecimientoId != -1) {
                filtroEstablecimiento = String.valueOf(establecimientoId);
                Log.d(TAG, "Filtrando por establecimiento: " + establecimientoNombre + " (ID: " + establecimientoId + ")");
            }
        }
    }

    private void inicializarComponentes() {
        recyclerView = findViewById(R.id.recyclerViewAvisos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnVolver = findViewById(R.id.btnVolver);
        btnAgregarAviso = findViewById(R.id.btnAgregarAviso);
        btnFiltrarAvisos = findViewById(R.id.btnFiltrarAvisos);
        MaterialButton btnExportarAvisos = findViewById(R.id.btnExportarAvisos);
        tvTitulo = findViewById(R.id.tvTituloAvisos);

        // Cambiar el título si viene de un establecimiento específico
        if (desdeEstablecimiento && establecimientoNombre != null && !establecimientoNombre.isEmpty()) {
            tvTitulo.setText("Avisos de " + establecimientoNombre);
        }

        btnVolver.setOnClickListener(v -> finish());
        btnAgregarAviso.setOnClickListener(v -> {
            Intent intent = new Intent(AvisosActivity.this, AgregarAvisoActivity.class);
            // Si viene de un establecimiento específico, pasar el ID para preseleccionarlo
            if (desdeEstablecimiento) {
                intent.putExtra("ESTABLECIMIENTO_ID", establecimientoId);
                intent.putExtra("ESTABLECIMIENTO_NOMBRE", establecimientoNombre);
            }
            startActivity(intent);
        });

        // Configurar el botón de exportar - solo visible para Admin y AreaManager
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String rol = sharedPreferences.getString("ROL_USUARIO", "");
        if ("Admin".equals(rol) || "AreaManager".equals(rol)) {
            btnExportarAvisos.setVisibility(View.VISIBLE);
            btnExportarAvisos.setOnClickListener(v -> exportarAvisosExcel());
        } else {
            btnExportarAvisos.setVisibility(View.GONE);
        }

        // Ocultar el botón de filtrar si viene de un establecimiento específico
        if (desdeEstablecimiento) {
            btnFiltrarAvisos.setVisibility(View.GONE);
        } else {
            btnFiltrarAvisos.setOnClickListener(v -> mostrarDialogoFiltros());
        }
    }

    private void configurarAdaptador() {
        avisosAdapter = new AvisosAdapter(new ArrayList<>(), esStaff);
        recyclerView.setAdapter(avisosAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        obtenerAvisos();
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

    private void obtenerAvisos() {
        avisos.clear();
        avisosOriginales.clear();

        ApiService apiService = ApiClient.getApiService();

        // Si viene de un establecimiento específico o es Staff, filtrar por establecimiento
        Integer filtroEstablecimientoApi = null;
        if (desdeEstablecimiento && establecimientoId != -1) {
            filtroEstablecimientoApi = establecimientoId;
        } else if (!esAdmin && establecimientoId != -1) {
            filtroEstablecimientoApi = establecimientoId;
        }

        Call<Map<String, List<Aviso>>> call = apiService.obtenerAvisos(filtroEstablecimientoApi);

        call.enqueue(new Callback<Map<String, List<Aviso>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Aviso>>> call, Response<Map<String, List<Aviso>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Aviso> listaAvisos = response.body().get("avisos");

                    if (listaAvisos != null && !listaAvisos.isEmpty()) {
                        // Guardar los avisos originales
                        avisosOriginales.addAll(listaAvisos);
                        Log.d(TAG, "Avisos recibidos: " + avisosOriginales.size());

                        // Aplicar los filtros actuales
                        aplicarFiltros();
                    } else {
                        Log.d(TAG, "No se encontraron avisos");
                        Toast.makeText(AvisosActivity.this, "No hay avisos disponibles", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta: " + response.code());
                    Toast.makeText(AvisosActivity.this, "Error al obtener avisos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Aviso>>> call, Throwable t) {
                Log.e(TAG, "Fallo en la conexión", t);
                Toast.makeText(AvisosActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoFiltros() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filtrar_avisos, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Inicializar componentes del diálogo
        RadioGroup radioGroupEstablecimiento = dialogView.findViewById(R.id.radioGroupEstablecimiento);
        RadioGroup radioGroupCategoria = dialogView.findViewById(R.id.radioGroupCategoria);
        Button btnLimpiar = dialogView.findViewById(R.id.btnLimpiarFiltros);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarFiltroAvisos);
        Button btnAplicar = dialogView.findViewById(R.id.btnAplicarFiltroAvisos);

        // Cargar opciones de establecimientos y categorías
        cargarOpcionesEstablecimiento(radioGroupEstablecimiento);
        cargarOpcionesCategorias(radioGroupCategoria);

        // Configurar acciones de botones
        btnLimpiar.setOnClickListener(v -> {
            filtroEstablecimiento = "Todos";
            filtroCategoria = "Todas";
            aplicarFiltros();
            dialog.dismiss();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnAplicar.setOnClickListener(v -> {
            // Obtener el establecimiento seleccionado
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

            // Obtener la categoría seleccionada
            int idCategoria = radioGroupCategoria.getCheckedRadioButtonId();
            if (idCategoria != -1) {
                RadioButton rbCategoria = dialogView.findViewById(idCategoria);
                if (rbCategoria != null) {
                    String tag = (String) rbCategoria.getTag();
                    filtroCategoria = tag != null ? tag : rbCategoria.getText().toString();
                    Log.d(TAG, "Categoría seleccionada: " + filtroCategoria);
                } else {
                    filtroCategoria = "Todas";
                    Log.d(TAG, "No se encontró el RadioButton de categoría con ID: " + idCategoria);
                }
            } else {
                filtroCategoria = "Todas";
                Log.d(TAG, "Ninguna categoría seleccionada, usando 'Todas'");
            }

            aplicarFiltros();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void cargarOpcionesEstablecimiento(RadioGroup radioGroup) {
        // Añadir opción "Todos"
        RadioButton rbTodos = new RadioButton(this);
        rbTodos.setText("Todos los establecimientos");
        rbTodos.setTag("Todos");
        rbTodos.setId(View.generateViewId());
        radioGroup.addView(rbTodos);

        // Seleccionar la opción actual
        boolean seleccionEncontrada = false;
        if (filtroEstablecimiento.equals("Todos")) {
            rbTodos.setChecked(true);
            seleccionEncontrada = true;
        }

        // Si es rol Admin, mostrar todos los establecimientos
        if (esAdmin) {
            for (Establecimientos establecimiento : listaEstablecimientos) {
                RadioButton rb = new RadioButton(this);
                rb.setText(establecimiento.getNombre());
                rb.setTag(String.valueOf(establecimiento.getId()));
                rb.setId(View.generateViewId());
                radioGroup.addView(rb);

                // Seleccionar si es el filtro actual
                if (filtroEstablecimiento.equals(String.valueOf(establecimiento.getId()))) {
                    rb.setChecked(true);
                    seleccionEncontrada = true;
                }
            }
        }
        // Si es Staff, solo mostrar su establecimiento
        else if (establecimientoId > 0) {
            RadioButton rb = new RadioButton(this);
            rb.setText("Mi establecimiento");
            rb.setTag(String.valueOf(establecimientoId));
            rb.setId(View.generateViewId());
            radioGroup.addView(rb);

            // Seleccionar si es el filtro actual
            if (filtroEstablecimiento.equals(String.valueOf(establecimientoId))) {
                rb.setChecked(true);
                seleccionEncontrada = true;
            }
        }

        // Si no se encontró ninguna selección, seleccionar "Todos" por defecto
        if (!seleccionEncontrada) {
            rbTodos.setChecked(true);
            filtroEstablecimiento = "Todos";
        }
    }

    private void cargarOpcionesCategorias(RadioGroup radioGroup) {
        // Crear conjunto de categorías únicas
        Set<String> categoriasUnicas = new HashSet<>();

        // Añadir categorías conocidas o comunes
        categoriasUnicas.add("Informativo");
        categoriasUnicas.add("Urgente");
        categoriasUnicas.add("Mantenimiento");
        categoriasUnicas.add("General");

        // Añadir categorías de avisos actuales si hay alguna no contemplada
        for (Aviso aviso : avisosOriginales) {
            if (aviso.getCategoria() != null && !aviso.getCategoria().isEmpty()) {
                categoriasUnicas.add(aviso.getCategoria());
            }
        }

        // Añadir opción "Todas"
        RadioButton rbTodas = new RadioButton(this);
        rbTodas.setText("Todas las categorías");
        rbTodas.setTag("Todas");
        rbTodas.setId(View.generateViewId());
        radioGroup.addView(rbTodas);

        // Seleccionar la opción actual
        boolean seleccionEncontrada = false;
        if (filtroCategoria.equals("Todas")) {
            rbTodas.setChecked(true);
            seleccionEncontrada = true;
        }

        // Añadir cada categoría como opción
        List<String> listaCategorias = new ArrayList<>(categoriasUnicas);
        Collections.sort(listaCategorias);

        for (String categoria : listaCategorias) {
            RadioButton rb = new RadioButton(this);
            rb.setText(categoria);
            rb.setTag(categoria);
            rb.setId(View.generateViewId());
            radioGroup.addView(rb);

            // Seleccionar si es el filtro actual
            if (filtroCategoria.equals(categoria)) {
                rb.setChecked(true);
                seleccionEncontrada = true;
            }
        }

        if (!seleccionEncontrada) {
            rbTodas.setChecked(true);
            filtroCategoria = "Todas";
        }
    }

    private void aplicarFiltros() {
        // Limpiar la lista de avisos mostrados
        avisos.clear();

        // Añadir logs para depuración
        Log.d(TAG, "Aplicando filtros - Establecimiento: " + filtroEstablecimiento + ", Categoría: " + filtroCategoria);

        // Si ambos filtros son "Todos"/"Todas", mostrar todos los avisos
        if (filtroEstablecimiento.equals("Todos") && filtroCategoria.equals("Todas")) {
            avisos.addAll(avisosOriginales);
            Log.d(TAG, "Mostrando todos los avisos: " + avisos.size());
        } else {
            // Aplicar filtros
            for (Aviso aviso : avisosOriginales) {
                boolean cumpleFiltroEstablecimiento;

                // Verificar filtro de establecimiento
                if (filtroEstablecimiento.equals("Todos")) {
                    cumpleFiltroEstablecimiento = true;
                } else {
                    try {
                        int idEstablecimientoFiltro = Integer.parseInt(filtroEstablecimiento);
                        cumpleFiltroEstablecimiento = (aviso.getEstablecimientoId() == idEstablecimientoFiltro);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error al convertir ID de establecimiento: " + filtroEstablecimiento, e);
                        cumpleFiltroEstablecimiento = false;
                    }
                }

                // Verificar filtro de categoría
                boolean cumpleFiltroCategoria = filtroCategoria.equals("Todas") ||
                        (aviso.getCategoria() != null && aviso.getCategoria().equals(filtroCategoria));

                // Log para depuración
                Log.d(TAG, "Aviso ID: " + aviso.getId() +
                        ", Establecimiento: " + aviso.getEstablecimientoId() +
                        ", Categoría: " + aviso.getCategoria() +
                        ", Cumple filtro establecimiento: " + cumpleFiltroEstablecimiento +
                        ", Cumple filtro categoría: " + cumpleFiltroCategoria);

                if (cumpleFiltroEstablecimiento && cumpleFiltroCategoria) {
                    avisos.add(aviso);
                }
            }
            Log.d(TAG, "Avisos filtrados: " + avisos.size());
        }

        // Actualizar el adaptador con la lista filtrada
        avisosAdapter.actualizarLista(avisos);
    }

    // Método para exportar los avisos actuales (filtrados) a un archivo Excel
    private void exportarAvisosExcel() {
        if (avisos.isEmpty()) {
            Toast.makeText(this, "No hay avisos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar diálogo para pedir el correo electrónico
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_export, null);
        builder.setView(dialogView);
        
        final EditText etEmail = dialogView.findViewById(R.id.etEmail);
        final Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        final Button btnEnviar = dialogView.findViewById(R.id.btnEnviar);
        
        final AlertDialog dialog = builder.create();
        dialog.show();
        
        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        
        btnEnviar.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            
            // Validar el correo electrónico
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(AvisosActivity.this, "Por favor, introduce un correo electrónico válido", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dialog.dismiss();
            generarYEnviarExcel(email);
        });
    }
    
    // Método para generar el archivo Excel y enviarlo por correo
    private void generarYEnviarExcel(String destinatario) {
        try {
            // Crear directorio si no existe
            File directorio = new File(getCacheDir(), "excel_temp");
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // Crear nombre de archivo con fecha y hora
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fechaHora = sdf.format(new Date());
            String nombreArchivo = "avisos_" + fechaHora + ".xlsx";
            File archivo = new File(directorio, nombreArchivo);

            // Ordenar los avisos por establecimiento
            List<Aviso> avisosOrdenados = new ArrayList<>(avisos);
            Collections.sort(avisosOrdenados, (a1, a2) -> {
                // Primero ordenar por establecimiento
                String establecimiento1 = a1.getEstablecimientoNombre() != null ? 
                        a1.getEstablecimientoNombre() : String.valueOf(a1.getEstablecimientoId());
                String establecimiento2 = a2.getEstablecimientoNombre() != null ? 
                        a2.getEstablecimientoNombre() : String.valueOf(a2.getEstablecimientoId());
                
                return establecimiento1.compareTo(establecimiento2);
            });

            // Crear un libro de trabajo Excel
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                // Crear una hoja
                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("Avisos");
                
                // Crear estilos para los encabezados
                org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.xssf.usermodel.XSSFFont font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                
                // Crear estilo para el grupo de establecimiento
                org.apache.poi.xssf.usermodel.XSSFCellStyle groupStyle = workbook.createCellStyle();
                org.apache.poi.xssf.usermodel.XSSFFont groupFont = workbook.createFont();
                groupFont.setBold(true);
                groupStyle.setFont(groupFont);
                groupStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
                groupStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                
                // Crear estilo para las celdas de datos
                org.apache.poi.xssf.usermodel.XSSFCellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                dataStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                dataStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                dataStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                
                // Estilo para las celdas de fecha
                org.apache.poi.xssf.usermodel.XSSFCellStyle dateStyle = workbook.createCellStyle();
                dateStyle.cloneStyleFrom(dataStyle);
                dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
                
                // Crear la fila de encabezado
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                
                String[] headers = {"Establecimiento", "Nombre", "Fecha", "Estado", "Usuario", "Categoría"};
                for (int i = 0; i < headers.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // Establecer anchos fijos para las columnas en lugar de autosizing
                // Establecimiento - Columna más ancha
                sheet.setColumnWidth(0, 30 * 256); // 30 caracteres de ancho aprox
                // Nombre - Necesita buen espacio
                sheet.setColumnWidth(1, 25 * 256); // 25 caracteres
                // Fecha
                sheet.setColumnWidth(2, 15 * 256); // 15 caracteres
                // Estado
                sheet.setColumnWidth(3, 15 * 256); // 15 caracteres
                // Usuario
                sheet.setColumnWidth(4, 20 * 256); // 20 caracteres
                // Categoría
                sheet.setColumnWidth(5, 20 * 256); // 20 caracteres
                
                // Variables para controlar la agrupación por establecimiento
                String establecimientoActual = null;
                int rowIndex = 1;
                
                // Escribir datos agrupados por establecimiento
                for (Aviso aviso : avisosOrdenados) {
                    // Obtener el nombre del establecimiento
                    String nombreEstablecimiento = aviso.getEstablecimientoNombre();
                    if (nombreEstablecimiento == null || nombreEstablecimiento.isEmpty()) {
                        nombreEstablecimiento = "Establecimiento " + aviso.getEstablecimientoId();
                    }
                    
                    // Si cambiamos de establecimiento, añadir una fila para el grupo
                    if (establecimientoActual == null || !establecimientoActual.equals(nombreEstablecimiento)) {
                        if (establecimientoActual != null) {
                            // Añadir una fila en blanco como separación entre establecimientos
                            sheet.createRow(rowIndex++);
                        }
                        
                        // Crear una fila para el nombre del establecimiento
                        org.apache.poi.ss.usermodel.Row groupRow = sheet.createRow(rowIndex++);
                        org.apache.poi.ss.usermodel.Cell groupCell = groupRow.createCell(0);
                        groupCell.setCellValue(nombreEstablecimiento);
                        groupCell.setCellStyle(groupStyle);
                        
                        // Combinar celdas para el título del grupo
                        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                                rowIndex - 1, rowIndex - 1, 0, headers.length - 1));
                        
                        establecimientoActual = nombreEstablecimiento;
                    }
                    
                    // Crear fila para el aviso
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex++);
                    
                    // 1. Establecimiento
                    org.apache.poi.ss.usermodel.Cell cell0 = row.createCell(0);
                    cell0.setCellValue(nombreEstablecimiento);
                    cell0.setCellStyle(dataStyle);
                    
                    // 2. Nombre
                    org.apache.poi.ss.usermodel.Cell cell1 = row.createCell(1);
                    cell1.setCellValue(aviso.getNombre() != null ? aviso.getNombre() : "");
                    cell1.setCellStyle(dataStyle);
                    
                    // 3. Fecha
                    org.apache.poi.ss.usermodel.Cell cell2 = row.createCell(2);
                    if (aviso.getFechaCreacion() != null && !aviso.getFechaCreacion().isEmpty()) {
                        // Intentar parsear la fecha si es posible
                        try {
                            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            Date date = parser.parse(aviso.getFechaCreacion());
                            cell2.setCellValue(date);
                            cell2.setCellStyle(dateStyle);
                        } catch (Exception e) {
                            // Si hay error en el formato, simplemente mostrar el texto
                            cell2.setCellValue(aviso.getFechaCreacion());
                            cell2.setCellStyle(dataStyle);
                        }
                    } else {
                        cell2.setCellStyle(dataStyle);
                    }
                    
                    // 4. Estado
                    org.apache.poi.ss.usermodel.Cell cell3 = row.createCell(3);
                    String estado = aviso.getEstado();
                    if (estado != null && !estado.equalsIgnoreCase("Completado")) {
                        cell3.setCellValue(estado);
                    }
                    cell3.setCellStyle(dataStyle);
                    
                    // 5. Usuario
                    org.apache.poi.ss.usermodel.Cell cell4 = row.createCell(4);
                    String nombreUsuario = aviso.getUsuarioNombre();
                    if (nombreUsuario == null || nombreUsuario.isEmpty()) {
                        nombreUsuario = "Usuario " + aviso.getUsuarioId();
                    }
                    cell4.setCellValue(nombreUsuario);
                    cell4.setCellStyle(dataStyle);
                    
                    // 6. Categoría
                    org.apache.poi.ss.usermodel.Cell cell5 = row.createCell(5);
                    cell5.setCellValue(aviso.getCategoria() != null ? aviso.getCategoria() : "");
                    cell5.setCellStyle(dataStyle);
                }
                
                // Ya no usamos autoSizeColumn que causa errores
                // En su lugar, definimos anchos fijos para las columnas más arriba
                
                // Escribir el archivo Excel
                try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(archivo)) {
                    workbook.write(outputStream);
                }
            }
            
            // Enviar el archivo Excel por correo
            enviarCorreoConExcel(destinatario, archivo);
            
        } catch (IOException e) {
            Log.e(TAG, "Error al exportar avisos a Excel: " + e.getMessage(), e);
            Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado al exportar a Excel: " + e.getMessage(), e);
            Toast.makeText(this, "Error inesperado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Método para enviar el correo con el archivo Excel adjunto
    private void enviarCorreoConExcel(String destinatario, File archivoExcel) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this, 
                    getApplicationContext().getPackageName() + ".fileprovider", 
                    archivoExcel);
                    
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{destinatario});
            
            // Verificar si hay al menos un aviso con proceso_id para cambiar el asunto
            boolean tieneProcesoId = false;
            for (Aviso aviso : avisos) {
                if (aviso.getProcesoId() > 0) {
                    tieneProcesoId = true;
                    break;
                }
            }
            
            // Cambiar el asunto del correo si hay proceso_id
            if (tieneProcesoId) {
                intent.putExtra(Intent.EXTRA_SUBJECT, "SMOOY AVISOS REGISTRO");
            } else {
                intent.putExtra(Intent.EXTRA_SUBJECT, "Exportación de avisos SMOOY");
            }
            
            intent.putExtra(Intent.EXTRA_TEXT, "Adjunto encontrará el archivo Excel con los avisos exportados, agrupados por establecimiento.");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(intent, "Enviar correo con:"));
            
            Toast.makeText(this, "Preparando envío del archivo Excel por correo electrónico", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al enviar correo con Excel: " + e.getMessage(), e);
            Toast.makeText(this, "Error al enviar correo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Método auxiliar para escapar campos en formato CSV
    private String escaparCampoCSV(String campo) {
        if (campo == null) return "";
        
        // Si el campo contiene comas, comillas o saltos de línea, encerrarlo en comillas dobles
        // y escapar las comillas dobles internas duplicándolas
        if (campo.contains(",") || campo.contains("\"") || campo.contains("\n")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }
}