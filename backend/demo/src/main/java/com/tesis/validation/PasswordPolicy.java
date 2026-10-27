package com.tesis.validation;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

public final class PasswordPolicy {

    private static final Set<String> PASSWORDS_COMUNES = Set.of(
            "123456",
            "12345678",
            "123456789",
            "1234567890",
            "123456789012",
            "password",
            "password1",
            "password123",
            "qwerty",
            "qwerty123",
            "admin",
            "admin123",
            "letmein",
            "welcome",
            "iloveyou",
            "monkey",
            "dragon",
            "sunshine",
            "princess",
            "football",
            "baseball",
            "secret",
            "contrasena",
            "contraseña",
            "clave123",
            "tequiero");

    private PasswordPolicy() {
    }

    public static String validar(String password, String nombreUsuario, String email) {
        if (password.codePointCount(0, password.length()) < 12) {
            return "La contraseña debe tener al menos 12 caracteres";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return "La contraseña no puede superar 72 bytes UTF-8";
        }
        if (password.codePoints().anyMatch(PasswordPolicy::esEspacio)) {
            return "La contraseña no puede contener espacios";
        }
        if (password.codePoints().noneMatch(Character::isUpperCase)) {
            return "La contraseña debe incluir al menos una mayúscula";
        }
        if (password.codePoints().noneMatch(Character::isDigit)) {
            return "La contraseña debe incluir al menos un número";
        }
        if (password.codePoints().noneMatch(codePoint ->
                !Character.isLetterOrDigit(codePoint) && !esEspacio(codePoint))) {
            return "La contraseña debe incluir al menos un símbolo";
        }

        String passwordNormalizada = password.toLowerCase(Locale.ROOT);
        String baseComun = passwordNormalizada.replaceAll("[^\\p{L}]+$", "");
        if (PASSWORDS_COMUNES.contains(passwordNormalizada)
                || PASSWORDS_COMUNES.contains(baseComun)) {
            return "Elige una contraseña menos común";
        }

        String usuarioNormalizado = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        String usuarioEmail = emailNormalizado.split("@", 2)[0];
        if (contieneIdentificador(passwordNormalizada, usuarioNormalizado)
                || contieneIdentificador(passwordNormalizada, emailNormalizado)
                || contieneIdentificador(passwordNormalizada, usuarioEmail)) {
            return "La contraseña no puede incluir el nombre de usuario ni el correo";
        }
        return null;
    }

    private static boolean contieneIdentificador(String password, String identificador) {
        return identificador.length() >= 3 && password.contains(identificador);
    }

    private static boolean esEspacio(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
