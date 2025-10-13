package com.smooy.smooypr1;

public class Establecimientos {

        private final int id;
        private final String nombre;
        private final String direccion;
        private final String tipo;
        private final String estado;


        public Establecimientos(int id, String nombre, String direccion, String tipo, String estado) {
            this.id = id;
            this.nombre = nombre;
            this.direccion = direccion;
            this.tipo = tipo;
            this.estado = estado;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getDireccion() {
            return direccion;
        }

        public String getTipo() {
            return tipo;
        }

        public String getEstado() {
            return estado;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }