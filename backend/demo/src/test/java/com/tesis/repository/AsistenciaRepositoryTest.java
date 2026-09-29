package com.tesis.repository;

import com.tesis.entity.AsignacionTurno;
import com.tesis.entity.Asistencia;
import com.tesis.entity.CredencialQr;
import com.tesis.entity.Empleado;
import com.tesis.entity.Turno;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AsistenciaRepositoryTest {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private AsignacionTurnoRepository asignacionTurnoRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private CredencialQrRepository credencialQrRepository;

    @Autowired
    private com.tesis.service.CarnetService carnetService;

    @Test
    void persisteAsistenciaYEncuentraTurnoVigente() {
        LocalDate hoy = LocalDate.now();
        Turno turno = new Turno();
        turno.setNombre("Diurno");
        turno.setHoraEntrada(LocalTime.of(8, 0));
        turno.setHoraSalida(LocalTime.of(16, 0));
        turno = turnoRepository.saveAndFlush(turno);

        Empleado empleado = new Empleado();
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        empleado.setCedula("ASISTENCIA-TEST-01");
        empleado.setActivo(true);
        empleado = empleadoRepository.saveAndFlush(empleado);

        AsignacionTurno asignacion = new AsignacionTurno();
        asignacion.setPersonal(empleado);
        asignacion.setTurno(turno);
        asignacion.setFechaDesde(hoy);
        asignacionTurnoRepository.saveAndFlush(asignacion);

        Asistencia asistencia = new Asistencia();
        asistencia.setPersonal(empleado);
        asistencia.setTurno(turno);
        asistencia.setFecha(hoy);
        asistencia.setHoraEntrada(LocalTime.of(8, 5));
        asistencia.setEstado("tardanza");
        asistencia.setMinutosTardanza((short) 5);
        asistencia = asistenciaRepository.saveAndFlush(asistencia);

        assertEquals(1, asignacionTurnoRepository.buscarVigentes(empleado.getId(), hoy).size());
        Asistencia guardada = asistenciaRepository
                .findByPersonal_IdAndFecha(empleado.getId(), hoy).orElseThrow();
        assertEquals(asistencia.getId(), guardada.getId());
        assertEquals("tardanza", guardada.getEstado());
        assertTrue(guardada.getCreadoEn() != null);
    }

    @Test
    void generaPdfYRevocaCredencialAnteriorAlReemitirCarnet() {
        Empleado empleado = new Empleado();
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        empleado.setCedula("CARNET-TEST-01");
        empleado.setActivo(true);
        empleado = empleadoRepository.saveAndFlush(empleado);

        byte[] primerCarnet = carnetService.generarCarnet(empleado.getId());
        CredencialQr credencialAnterior = credencialQrRepository
                .findByEmpleado_IdAndActivaTrue(empleado.getId()).getFirst();
        byte[] carnetReemitido = carnetService.generarCarnet(empleado.getId());

        assertEquals("%PDF-", new String(primerCarnet, 0, 5));
        assertEquals("%PDF-", new String(carnetReemitido, 0, 5));
        assertFalse(credencialQrRepository.findById(credencialAnterior.getId())
                .orElseThrow().getActiva());
        assertEquals(1, credencialQrRepository
                .findByEmpleado_IdAndActivaTrue(empleado.getId()).size());
    }
}