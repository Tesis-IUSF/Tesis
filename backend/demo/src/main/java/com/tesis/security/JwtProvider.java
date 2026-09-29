package com.tesis.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtProvider {

    private static final String QR_ISSUER = "sitio-web";
    private static final String QR_PURPOSE = "employee-attendance";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration; // 24 horas en milisegundos

    @PostConstruct
    void validateConfiguration() {
        signingKey();
    }

    /**
     * Genera un token JWT para un usuario
     */
    public String generarToken(Integer usuarioId, String email, String nombreUsuario, String rolNombre) {
        try {
            return Jwts.builder()
                    .subject(email)
                    .claim("usuarioId", usuarioId)
                    .claim("nombreUsuario", nombreUsuario)
                    .claim("rolNombre", rolNombre)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(signingKey())
                    .compact();
        } catch (Exception e) {
            log.error("Error generando token JWT", e);
            throw new RuntimeException("Error al generar token", e);
        }
    }

    /**
     * Valida un token JWT y retorna las claims
     */
    public Claims validarToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error validando token JWT: {}", e.getMessage());
            throw new RuntimeException("Token inválido o expirado", e);
        }
    }

    public String generarTokenQr(Integer empleadoId, String credencialId, Instant expiracion) {
        return Jwts.builder()
                .issuer(QR_ISSUER)
                .subject(empleadoId.toString())
                .id(credencialId)
                .claim("purpose", QR_PURPOSE)
                .issuedAt(new Date())
                .expiration(Date.from(expiracion))
                .signWith(qrSigningKey())
                .compact();
    }

    public QrClaims validarTokenQr(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(qrSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!QR_ISSUER.equals(claims.getIssuer()) || !QR_PURPOSE.equals(claims.get("purpose"))) {
            throw new IllegalArgumentException("El token no es una credencial QR de asistencia");
        }

        Integer empleadoId;
        try {
            empleadoId = Integer.valueOf(claims.getSubject());
            UUID.fromString(claims.getId());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("La credencial QR no contiene datos válidos", exception);
        }
        if (empleadoId <= 0 || claims.getExpiration() == null) {
            throw new IllegalArgumentException("La credencial QR está incompleta");
        }
        return new QrClaims(empleadoId, claims.getId(), claims.getExpiration().toInstant());
    }

    public record QrClaims(Integer empleadoId, String credencialId, Instant expiracion) {
    }

    /**
     * Obtiene el email del token
     */
    public String obtenerEmailDelToken(String token) {
        return validarToken(token).getSubject();
    }

    /**
     * Obtiene el ID del usuario del token
     */
    public Integer obtenerUsuarioIdDelToken(String token) {
        return validarToken(token).get("usuarioId", Integer.class);
    }

    /**
     * Verifica si el token es válido
     */
    public boolean esTokenValido(String token) {
        try {
            validarToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey signingKey() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 bytes");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey qrSigningKey() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 bytes");
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] derivedKey = hmac.doFinal("employee-attendance-qr-v1".getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(derivedKey);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo inicializar la firma de QR", exception);
        }
    }
}