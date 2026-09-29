package com.tesis.service;

import com.tesis.entity.CredencialQr;
import com.tesis.entity.Empleado;
import com.tesis.repository.CredencialQrRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Transactional
public class CarnetService {

    private final EmpleadoRepository empleadoRepository;
    private final CredencialQrRepository credencialQrRepository;
    private final JwtProvider jwtProvider;
    private final CarnetPdfGenerator carnetPdfGenerator;

    @Value("${app.qr.expiration-days:365}")
    private long diasVigencia;

    public CarnetService(EmpleadoRepository empleadoRepository,
                         CredencialQrRepository credencialQrRepository,
                         JwtProvider jwtProvider,
                         CarnetPdfGenerator carnetPdfGenerator) {
        this.empleadoRepository = empleadoRepository;
        this.credencialQrRepository = credencialQrRepository;
        this.jwtProvider = jwtProvider;
        this.carnetPdfGenerator = carnetPdfGenerator;
    }

    public byte[] generarCarnet(Integer empleadoId) {
        if (diasVigencia < 1) {
            throw new IllegalStateException("app.qr.expiration-days debe ser mayor que cero");
        }
        Empleado empleado = empleadoRepository.findByIdForUpdate(empleadoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
        if (!Boolean.TRUE.equals(empleado.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede emitir carnet para un empleado inactivo");
        }

        LocalDateTime ahora = LocalDateTime.now();
        credencialQrRepository.findByEmpleado_IdAndActivaTrue(empleadoId).forEach(credencial -> {
            credencial.setActiva(false);
            credencial.setRevocadaEn(ahora);
        });
        credencialQrRepository.flush();

        String credencialId = UUID.randomUUID().toString();
        LocalDateTime expiraEn = ahora.plusDays(diasVigencia);
        Instant expiracionToken = expiraEn.atZone(ZoneId.systemDefault()).toInstant();
        CredencialQr credencial = new CredencialQr();
        credencial.setId(credencialId);
        credencial.setEmpleado(empleado);
        credencial.setActiva(true);
        credencial.setCreadaEn(ahora);
        credencial.setExpiraEn(expiraEn);
        credencialQrRepository.saveAndFlush(credencial);

        String qrToken = jwtProvider.generarTokenQr(empleadoId, credencialId, expiracionToken);
        return carnetPdfGenerator.generar(empleado, qrToken);
    }
}