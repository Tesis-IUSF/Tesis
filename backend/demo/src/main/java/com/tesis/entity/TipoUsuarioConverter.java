package com.tesis.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter
public class TipoUsuarioConverter implements AttributeConverter<TipoUsuario, String> {

    @Override
    public String convertToDatabaseColumn(TipoUsuario tipoUsuario) {
        return tipoUsuario == null ? null : tipoUsuario.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public TipoUsuario convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoUsuario.valueOf(valor.toUpperCase(Locale.ROOT));
    }
}