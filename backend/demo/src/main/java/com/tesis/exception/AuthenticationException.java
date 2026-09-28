package com.tesis.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String mensaje) {
        super(mensaje);
    }

    public AuthenticationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
