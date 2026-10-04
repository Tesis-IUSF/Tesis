package com.tesis.service;

import com.tesis.entity.CredencialQr;
import com.tesis.entity.Empleado;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.CredencialQrRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CarnetLoteServiceTest {

    @Autowired
    private CarnetLoteService carnetLoteService;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private CredencialQrRepository credencialRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void creaZipConUnPdfPorEmpleadoYNombreIncluyendoNombreApellidoYCi() throws Exception {
        Empleado primero = crearEmpleado("José María", "Pérez López", "LOTE-ZIP-001", true);
        Empleado segundo = crearEmpleado("Ana", "Gómez", "LOTE-ZIP-002", true);

        byte[] zip = carnetLoteService.generar(
                List.of(primero.getId(), segundo.getId()), "zip");

        List<String> entradas = new ArrayList<>();
        String manifiesto = "";
        try (ZipInputStream archivo = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entrada;
            while ((entrada = archivo.getNextEntry()) != null) {
                entradas.add(entrada.getName());
                if ("resultado.csv".equals(entrada.getName())) {
                    manifiesto = new String(archivo.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        assertTrue(entradas.contains("carnet-jose-maria-perez-lopez-lote-zip-001.pdf"));
        assertTrue(entradas.contains("carnet-ana-gomez-lote-zip-002.pdf"));
        assertTrue(entradas.contains("resultado.csv"));
        assertTrue(manifiesto.contains("José María"));
        assertTrue(manifiesto.contains("LOTE-ZIP-002"));
        assertEquals(1, credencialRepository.findByEmpleado_IdAndActivaTrue(primero.getId()).size());
        assertEquals(1, credencialRepository.findByEmpleado_IdAndActivaTrue(segundo.getId()).size());
    }

    @Test
    void combinaLosCarnetsEnUnPdfConUnaPaginaPorEmpleado() throws Exception {
        Empleado primero = crearEmpleado("Elena", "Uno", "LOTE-PDF-001", true);
        Empleado segundo = crearEmpleado("Marco", "Dos", "LOTE-PDF-002", true);
        Empleado tercero = crearEmpleado("Sonia", "Tres", "LOTE-PDF-003", true);

        byte[] pdf = carnetLoteService.generar(
                List.of(primero.getId(), segundo.getId(), tercero.getId()), "pdf");
        PdfReader lector = new PdfReader(pdf);

        assertEquals(3, lector.getNumberOfPages());
        assertTrue(lector.getPageSize(1).getWidth() < 300);
        lector.close();
    }

    @Test
    void descargaPdfSinReemplazarCredencialesActivas() throws Exception {
        Empleado primero = crearEmpleado("Elena", "Uno", "LOTE-DESCARGA-001", true);
        Empleado segundo = crearEmpleado("Marco", "Dos", "LOTE-DESCARGA-002", true);
        carnetLoteService.generar(List.of(primero.getId(), segundo.getId()), "pdf");
        CredencialQr credencialPrimero = credencialRepository
            .findByEmpleado_IdAndActivaTrue(primero.getId()).getFirst();
        CredencialQr credencialSegundo = credencialRepository
            .findByEmpleado_IdAndActivaTrue(segundo.getId()).getFirst();
        String idPrimero = credencialPrimero.getId();
        String idSegundo = credencialSegundo.getId();
        LocalDateTime expiraPrimero = credencialPrimero.getExpiraEn();
        LocalDateTime expiraSegundo = credencialSegundo.getExpiraEn();

        byte[] pdf = carnetLoteService.descargarExistentes(
            List.of(primero.getId(), segundo.getId()), "pdf");
        PdfReader lector = new PdfReader(pdf);

        assertEquals(2, lector.getNumberOfPages());
        assertEquals(idPrimero, credencialRepository
            .findByEmpleado_IdAndActivaTrue(primero.getId()).getFirst().getId());
        assertEquals(idSegundo, credencialRepository
            .findByEmpleado_IdAndActivaTrue(segundo.getId()).getFirst().getId());
        assertEquals(expiraPrimero, credencialRepository
            .findByEmpleado_IdAndActivaTrue(primero.getId()).getFirst().getExpiraEn());
        assertEquals(expiraSegundo, credencialRepository
            .findByEmpleado_IdAndActivaTrue(segundo.getId()).getFirst().getExpiraEn());
        lector.close();

        byte[] zip = carnetLoteService.descargarExistentes(
            List.of(primero.getId(), segundo.getId()), "zip");
        List<String> entradas = new ArrayList<>();
        try (ZipInputStream archivo = new ZipInputStream(
                new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entrada;
            while ((entrada = archivo.getNextEntry()) != null) {
                entradas.add(entrada.getName());
            }
        }
        assertTrue(entradas.contains("carnet-elena-uno-lote-descarga-001.pdf"));
        assertTrue(entradas.contains("carnet-marco-dos-lote-descarga-002.pdf"));
        assertTrue(entradas.contains("resultado.csv"));
    }

    @Test
    void noDescargaParcialmenteSiFaltaUnaCredencialVigente() {
        Empleado conCredencial = crearEmpleado("Con", "Credencial", "LOTE-DESCARGA-003", true);
        Empleado sinCredencial = crearEmpleado("Sin", "Credencial", "LOTE-DESCARGA-004", true);
        carnetLoteService.generar(List.of(conCredencial.getId()), "pdf");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> carnetLoteService.descargarExistentes(
                List.of(conCredencial.getId(), sinCredencial.getId()), "zip"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Sin Credencial"));
        assertTrue(exception.getReason().contains(sinCredencial.getId().toString()));
        assertEquals(1, credencialRepository
            .findByEmpleado_IdAndActivaTrue(conCredencial.getId()).size());
    }

    @Test
    void rechazaDescargaDeCredencialVencida() {
        Empleado empleado = crearEmpleado("Vencida", "Credencial", "LOTE-DESCARGA-005", true);
        carnetLoteService.generar(List.of(empleado.getId()), "pdf");
        CredencialQr credencial = credencialRepository
            .findByEmpleado_IdAndActivaTrue(empleado.getId()).getFirst();
        credencial.setExpiraEn(LocalDateTime.now().minusMinutes(1));
        credencialRepository.saveAndFlush(credencial);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> carnetLoteService.descargarExistentes(List.of(empleado.getId()), "pdf"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains(empleado.getId().toString()));
    }

    @Test
    void validaTodosLosEmpleadosAntesDeEmitirLaTanda() {
        Empleado activo = crearEmpleado("Activa", "Primero", "LOTE-INACTIVO-001", true);
        Empleado inactivo = crearEmpleado("Inactiva", "Segunda", "LOTE-INACTIVO-002", false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> carnetLoteService.generar(List.of(activo.getId(), inactivo.getId()), "zip"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(credencialRepository.findByEmpleado_IdAndActivaTrue(activo.getId()).isEmpty());
        assertTrue(credencialRepository.findByEmpleado_IdAndActivaTrue(inactivo.getId()).isEmpty());
    }

    private Empleado crearEmpleado(String nombre, String apellido, String cedula, boolean activo) {
        Empleado empleado = new Empleado();
        empleado.setNombre(nombre);
        empleado.setApellido(apellido);
        empleado.setCedula(cedula);
        empleado.setActivo(activo);
        empleado = empleadoRepository.saveAndFlush(empleado);
        entityManager.clear();
        return empleadoRepository.findById(empleado.getId()).orElseThrow();
    }
}