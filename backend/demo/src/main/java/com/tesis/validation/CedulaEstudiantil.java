package com.tesis.validation;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CedulaEstudiantil {

    public static final String FORMATO = "(?i)^CE-[VE][1-9]\\d{10}$";
    public static final String MENSAJE =
            "La cédula escolar debe tener formato CE-V11818456789 o CE-E11818456789";

    private static final Pattern PATRON = Pattern.compile(FORMATO);

    private CedulaEstudiantil() {
    }

    public static String normalizar(String cedula) {
        return cedula == null ? null : cedula.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean esValida(String cedula) {
        return cedula != null && PATRON.matcher(cedula).matches();
    }

    public static String generar(String nacionalidad,
                                 Integer ordenNacimiento,
                                 LocalDate fechaNacimiento,
                                 String cedulaRepresentante) {
        String nacionalidadNormalizada = nacionalidad == null
                ? null : nacionalidad.trim().toUpperCase(Locale.ROOT);
        if (!"V".equals(nacionalidadNormalizada) && !"E".equals(nacionalidadNormalizada)) {
            throw new IllegalArgumentException("La nacionalidad debe ser V o E");
        }
        if (ordenNacimiento == null || ordenNacimiento < 1 || ordenNacimiento > 9) {
            throw new IllegalArgumentException("El orden de nacimiento debe estar entre 1 y 9");
        }
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria");
        }

        String representanteNormalizado =
                CedulaVenezolana.normalizarFormato(cedulaRepresentante);
        if (!CedulaVenezolana.esValida(representanteNormalizado)) {
            throw new IllegalArgumentException(CedulaVenezolana.MENSAJE);
        }

        String digitosRepresentante = representanteNormalizado.substring(2);
        String cedulaRepresentanteCompleta =
                "0".repeat(8 - digitosRepresentante.length()) + digitosRepresentante;
        int anio = Math.floorMod(fechaNacimiento.getYear(), 100);
        return String.format(Locale.ROOT, "CE-%s%d%02d%s",
                nacionalidadNormalizada, ordenNacimiento, anio, cedulaRepresentanteCompleta);
    }
}
