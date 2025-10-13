package com.smooy.smooypr1;

import android.util.Log;

import java.sql.DriverManager;

public class Conexion {

    protected static String db = "smooydb";
    protected static String ip = "0.0.0.0";  // 10.0.2.2 es el localhost en el emulador de Android
    protected static String port = "3306";
    protected static String username = "root";
    protected static String password = "root";

    public java.sql.Connection Con() {  // Asegúrate de que el tipo de retorno sea java.sql.Connection

        java.sql.Connection conn = null;

        try {
            // Cargar el driver de MySQL
            Class.forName("com.mysql.jdbc.Driver");  // Utiliza el driver de MySQL 8.0

            // Crear la cadena de conexión
            String conexion = "jdbc:mysql://" + ip + ":" + port + "/" + db;

            // Establecer la conexión
            conn = DriverManager.getConnection(conexion, username, password);

        } catch (Exception e) {
            // Si ocurre un error, lo registramos
            Log.e("Error", e.getMessage());
        }

        return conn;
    }
}
