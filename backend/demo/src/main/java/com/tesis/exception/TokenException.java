package com.tesis.exception;

public class TokenException extends RuntimeException {
    public TokenException(String mensaje) {
        super(mensaje);
    }

    public TokenException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
