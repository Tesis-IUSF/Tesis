package com.tesis.entity;

public enum TipoUsuario {
    PERSONAL("personal"),
    ESTUDIANTE("estudiante"),
    REPRESENTANTE("representante"),
    VISITANTE("visitante");

    private final String valor;

    TipoUsuario(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}