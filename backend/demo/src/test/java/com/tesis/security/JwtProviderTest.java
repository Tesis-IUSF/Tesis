package com.tesis.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtProviderTest {

    private static final String SECRET = "test_signing_secret_with_more_than_32_bytes";
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = providerWithSecret(SECRET);
    }

    @Test
    void tokenFirmadoContieneIdentidadYRol() {
        String token = jwtProvider.generarToken(12, "ana@example.com", "ana", "Administrativo");

        Claims claims = jwtProvider.validarToken(token);

        assertEquals("ana@example.com", claims.getSubject());
        assertEquals(12, claims.get("usuarioId", Integer.class));
        assertEquals("ana", claims.get("nombreUsuario", String.class));
        assertEquals("Administrativo", claims.get("rolNombre", String.class));
    }

    @Test
    void tokenFirmadoConOtraClaveNoEsValido() {
        String token = jwtProvider.generarToken(12, "ana@example.com", "ana", "Administrativo");
        JwtProvider otherProvider = providerWithSecret("another_test_signing_secret_more_than_32_bytes");

        assertFalse(otherProvider.esTokenValido(token));
    }

    @Test
    void tokenQrFirmadoContieneEmpleadoYCredencial() {
        String credencialId = UUID.randomUUID().toString();
        String token = jwtProvider.generarTokenQr(12, credencialId, Instant.now().plusSeconds(3600));

        JwtProvider.QrClaims claims = jwtProvider.validarTokenQr(token);

        assertEquals(12, claims.empleadoId());
        assertEquals(credencialId, claims.credencialId());
    }

    @Test
    void tokenDeSesionNoSeAceptaComoQr() {
        String token = jwtProvider.generarToken(12, "ana@example.com", "ana", "Escaner");

        assertThrows(RuntimeException.class, () -> jwtProvider.validarTokenQr(token));
    }

    @Test
    void rechazaQrFirmadoConOtraClave() {
        String token = jwtProvider.generarTokenQr(12, UUID.randomUUID().toString(),
                Instant.now().plusSeconds(3600));
        JwtProvider otherProvider = providerWithSecret("another_test_signing_secret_more_than_32_bytes");

        assertThrows(RuntimeException.class, () -> otherProvider.validarTokenQr(token));
    }

    @Test
    void rechazaQrVencido() {
        String token = jwtProvider.generarTokenQr(12, UUID.randomUUID().toString(),
                Instant.now().minusSeconds(60));

        assertThrows(RuntimeException.class, () -> jwtProvider.validarTokenQr(token));
    }

    @Test
    void rechazaClavesMenoresA32Bytes() {
        JwtProvider invalidProvider = providerWithSecret("too_short");

        assertThrows(IllegalStateException.class, invalidProvider::validateConfiguration);
    }

    private JwtProvider providerWithSecret(String secret) {
        JwtProvider provider = new JwtProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpiration", 3_600_000L);
        return provider;
    }
}