package com.tesis.service;

import com.tesis.dto.CalendarioMatriculaDTO;
import com.tesis.dto.CalendarioMatriculaDTO.AnioEscolarDTO;
import com.tesis.dto.CalendarioMatriculaDTO.PeriodoDTO;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.TipoPeriodoMatricula;
import com.tesis.repository.AnioEscolarRepository;
import com.tesis.repository.PeriodoMatriculaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CalendarioMatriculaService {

    private static final String EN_PROCESO = "en_proceso";

    private final AnioEscolarRepository anioRepository;
    private final PeriodoMatriculaRepository periodoRepository;
    private final Clock clock;

    public CalendarioMatriculaService(AnioEscolarRepository anioRepository,
                                      PeriodoMatriculaRepository periodoRepository,
                                      Clock clock) {
        this.anioRepository = anioRepository;
        this.periodoRepository = periodoRepository;
        this.clock = clock;
    }

    public AnioEscolar obtenerAnioActivo() {
        return anioRepository.findFirstByActivoTrue().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay un año escolar activo configurado"));
    }

    public void validarAnioExistente(Short anio) {
        if (anio == null || !anioRepository.existsByAnio(anio)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El año escolar solicitado no existe");
        }
    }

    public void validarPreinscripcion(Short anio) {
        validarAnioExistente(anio);
        if (!hayPeriodoVigente(anio, TipoPeriodoMatricula.preinscripcion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay un período activo de preinscripción abierto para el año escolar solicitado");
        }
    }

    public void validarGestionInscripcion(Short anio, String estadoMatricula) {
        validarAnioExistente(anio);
        if (hayPeriodoVigente(anio, TipoPeriodoMatricula.inscripcion)) {
            return;
        }
        // La ventana extemporánea solo permite continuar trámites ya iniciados.
        if (EN_PROCESO.equals(estadoMatricula)
                && hayPeriodoVigente(anio, TipoPeriodoMatricula.extemporanea)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No hay un período activo de inscripción abierto para gestionar esta matrícula");
    }

    public CalendarioMatriculaDTO obtenerCalendario(Short anioSolicitado) {
        AnioEscolar activo = obtenerAnioActivo();
        Short anioSeleccionado = anioSolicitado == null ? activo.getAnio() : anioSolicitado;
        AnioEscolar consultado = anioRepository.findByAnio(anioSeleccionado).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El año escolar solicitado no existe"));
        LocalDate hoy = LocalDate.now(clock);
        List<PeriodoDTO> periodos = periodoRepository
                .findByAnioEscolarAndActivoTrueOrderByFechaInicioAsc(anioSeleccionado).stream()
                .map(periodo -> toPeriodoDTO(periodo, hoy))
                .toList();
        return new CalendarioMatriculaDTO(toAnioDTO(activo), toAnioDTO(consultado), periodos);
    }

    private boolean hayPeriodoVigente(Short anio, TipoPeriodoMatricula tipo) {
        LocalDate hoy = LocalDate.now(clock);
        return periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(anio, tipo).stream()
                .anyMatch(periodo -> incluyeFecha(periodo, hoy));
    }

    private boolean incluyeFecha(PeriodoMatricula periodo, LocalDate fecha) {
        return !fecha.isBefore(periodo.getFechaInicio()) && !fecha.isAfter(periodo.getFechaFin());
    }

    private AnioEscolarDTO toAnioDTO(AnioEscolar anio) {
        return new AnioEscolarDTO(anio.getAnio(), anio.getNombre(),
                anio.getFechaInicio(), anio.getFechaFin());
    }

    private PeriodoDTO toPeriodoDTO(PeriodoMatricula periodo, LocalDate hoy) {
        return new PeriodoDTO(periodo.getId(), periodo.getAnioEscolar(), periodo.getTipo(),
                periodo.getFechaInicio(), periodo.getFechaFin(), incluyeFecha(periodo, hoy));
    }
}