package com.tesis.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class CedulaVenezolana {

    public static final String FORMATO =
            "(?i)^\\s*(?:V-(?:[6-9]\\d{6}|[1-9]\\d{7})|E-[89]\\d{7})\\s*$";
    public static final String MENSAJE =
            "La cédula debe tener formato V-12345678 (desde 6000000) o E-81234567 (desde 80000000)";

    private static final Pattern PATRON = Pattern.compile(FORMATO);

    private CedulaVenezolana() {
    }

    public static String normalizar(String cedula) {
        return cedula == null ? null : cedula.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizarFormato(String cedula) {
        String normalizada = normalizar(cedula);
        return normalizada == null ? null : normalizada.replaceAll("[\\s.]", "");
    }

    public static boolean esValida(String cedula) {
        String normalizada = normalizarFormato(cedula);
        return normalizada != null && PATRON.matcher(normalizada).matches();
    }
}
