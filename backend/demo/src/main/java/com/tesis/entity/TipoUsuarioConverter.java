package com.tesis.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter
public class TipoUsuarioConverter implements AttributeConverter<TipoUsuario, String> {

    @Override
    public String convertToDatabaseColumn(TipoUsuario tipoUsuario) {
        return tipoUsuario == null ? null : tipoUsuario.getValor();
    }

    @Override
    public TipoUsuario convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(TipoUsuario.values())
                .filter(tipoUsuario -> tipoUsuario.getValor().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de usuario desconocido: " + value));
    }
}