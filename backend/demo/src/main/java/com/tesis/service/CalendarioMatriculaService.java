package com.tesis.service;

import com.tesis.dto.CalendarioMatriculaDTO;
import com.tesis.dto.CalendarioMatriculaDTO.AnioEscolarDTO;
import com.tesis.dto.CalendarioMatriculaDTO.PeriodoDTO;
import com.tesis.dto.GestionCalendarioDTO.AnioEscolarRequestDTO;
import com.tesis.dto.GestionCalendarioDTO.AnioEscolarResponseDTO;
import com.tesis.dto.GestionCalendarioDTO.PeriodoRequestDTO;
import com.tesis.dto.GestionCalendarioDTO.PeriodoResponseDTO;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.TipoPeriodoMatricula;
import com.tesis.repository.AnioEscolarRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.PeriodoMatriculaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CalendarioMatriculaService {

    private static final String EN_PROCESO = "en_proceso";

    private final AnioEscolarRepository anioRepository;
    private final PeriodoMatriculaRepository periodoRepository;
    private final MatriculaRepository matriculaRepository;
    private final Clock clock;

    public CalendarioMatriculaService(AnioEscolarRepository anioRepository,
                                      PeriodoMatriculaRepository periodoRepository,
                                      MatriculaRepository matriculaRepository,
                                      Clock clock) {
        this.anioRepository = anioRepository;
        this.periodoRepository = periodoRepository;
        this.matriculaRepository = matriculaRepository;
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
        AnioEscolar solicitado = obtenerAnio(anio);
        if (!Boolean.TRUE.equals(solicitado.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Las nuevas preinscripciones solo se permiten para el año escolar activo");
        }
        if (!hayPeriodoVigente(anio, TipoPeriodoMatricula.preinscripcion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay un período activo de preinscripción abierto para el año escolar solicitado");
        }
    }

    public void validarGestionInscripcion(Short anio, String estadoMatricula) {
        AnioEscolar solicitado = obtenerAnio(anio);
        if (Boolean.TRUE.equals(solicitado.getActivo())
                && hayPeriodoVigente(anio, TipoPeriodoMatricula.inscripcion)) {
            return;
        }
        if (EN_PROCESO.equals(estadoMatricula)
                && hayPeriodoVigente(anio, TipoPeriodoMatricula.extemporanea)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No hay un período activo de inscripción abierto para gestionar esta matrícula");
    }

    @Transactional
    public AnioEscolarResponseDTO crearAnio(AnioEscolarRequestDTO request) {
        validarDatosAnio(request);
        if (anioRepository.existsByAnio(request.getAnio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un año escolar con ese número");
        }

        AnioEscolar nuevoAnio = new AnioEscolar();
        nuevoAnio.setAnio(request.getAnio());
        nuevoAnio.setNombre(request.getNombre().trim());
        nuevoAnio.setFechaInicio(request.getFechaInicio());
        nuevoAnio.setFechaFin(request.getFechaFin());
        nuevoAnio.setActivo(false);
        AnioEscolar anio = anioRepository.saveAndFlush(nuevoAnio);

        Optional<AnioEscolar> origen = anioRepository
                .findTopByAnioLessThanOrderByAnioDesc(anio.getAnio());
        Short sugeridoDesde = null;
        if (origen.isPresent()) {
            AnioEscolar anioOrigen = origen.get();
            List<PeriodoMatricula> sugerencias = periodoRepository
                    .findByAnioEscolarAndActivoTrue(anioOrigen.getAnio()).stream()
                    .map(periodo -> copiarPeriodo(periodo, anio.getAnio(), anioOrigen.getAnio()))
                    .toList();
            if (!sugerencias.isEmpty()) {
                periodoRepository.saveAll(sugerencias);
                sugeridoDesde = anioOrigen.getAnio();
            }
        }
        return respuestaAnio(anio, sugeridoDesde);
    }

    @Transactional(readOnly = true)
    public List<AnioEscolarResponseDTO> listarAnios() {
        return anioRepository.findAllByOrderByAnioDesc().stream()
                .map(anio -> respuestaAnio(anio, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public AnioEscolarResponseDTO obtenerAnioGestion(Short anio) {
        return respuestaAnio(obtenerAnio(anio), null);
    }

    @Transactional
    public AnioEscolarResponseDTO actualizarAnio(Short anio, AnioEscolarRequestDTO request) {
        validarDatosAnio(request);
        AnioEscolar actual = anioRepository.findByAnioForUpdate(anio).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El año escolar no existe"));
        if (!actual.getAnio().equals(request.getAnio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cambiar el número de un año escolar ya creado");
        }
        actual.setNombre(request.getNombre().trim());
        actual.setFechaInicio(request.getFechaInicio());
        actual.setFechaFin(request.getFechaFin());
        anioRepository.save(actual);
        return respuestaAnio(actual, null);
    }

    @Transactional
    public AnioEscolarResponseDTO activarAnio(Short anio) {
        List<AnioEscolar> aniosBloqueados = anioRepository.findAllForUpdate();
        AnioEscolar nuevoActivo = aniosBloqueados.stream()
                .filter(escolar -> escolar.getAnio().equals(anio))
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "El año escolar no existe"));
        validarDatosAnio(nuevoActivo.getNombre(), nuevoActivo.getFechaInicio(), nuevoActivo.getFechaFin());

        List<PeriodoMatricula> periodos = periodoRepository
                .findByAnioEscolarOrderByFechaInicioAsc(anio);
        for (PeriodoMatricula periodo : periodos) {
            validarDatosPeriodo(periodo.getTipo(), periodo.getFechaInicio(), periodo.getFechaFin());
            validarSolapamiento(periodo.getAnioEscolar(), periodo.getTipo(),
                    periodo.getFechaInicio(), periodo.getFechaFin(), periodo.getId());
        }
        for (TipoPeriodoMatricula tipo : TipoPeriodoMatricula.values()) {
            boolean tienePeriodoActivo = periodos.stream()
                    .anyMatch(periodo -> periodo.getTipo() == tipo && Boolean.TRUE.equals(periodo.getActivo()));
            if (!tienePeriodoActivo) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Debe configurar al menos un período activo de tipo " + tipo
                                + " antes de activar el año escolar");
            }
        }

        for (AnioEscolar anterior : aniosBloqueados) {
            if (!anterior.getAnio().equals(anio)) {
                anterior.setActivo(false);
                anioRepository.save(anterior);
                for (PeriodoMatricula periodo : periodoRepository
                        .findByAnioEscolarAndActivoTrue(anterior.getAnio())) {
                    if (periodo.getTipo() == TipoPeriodoMatricula.preinscripcion
                            || periodo.getTipo() == TipoPeriodoMatricula.inscripcion) {
                        periodo.setActivo(false);
                    }
                }
            }
        }
        nuevoActivo.setActivo(true);
        anioRepository.save(nuevoActivo);
        return respuestaAnio(nuevoActivo, null);
    }

    @Transactional
    public PeriodoResponseDTO crearPeriodo(Short anio, PeriodoRequestDTO request) {
        AnioEscolar escolar = obtenerAnio(anio);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar los datos del período de matrícula");
        }
        validarDatosPeriodo(request.getTipo(), request.getFechaInicio(), request.getFechaFin());
        validarSolapamiento(anio, request.getTipo(), request.getFechaInicio(), request.getFechaFin(), null);
        PeriodoMatricula periodo = new PeriodoMatricula();
        periodo.setAnioEscolar(escolar.getAnio());
        periodo.setTipo(request.getTipo());
        periodo.setFechaInicio(request.getFechaInicio());
        periodo.setFechaFin(request.getFechaFin());
        periodo.setActivo(true);
        return respuestaPeriodo(periodoRepository.save(periodo));
    }

    @Transactional
    public PeriodoResponseDTO actualizarPeriodo(Integer id, PeriodoRequestDTO request) {
        PeriodoMatricula periodo = obtenerPeriodo(id);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar los datos del período de matrícula");
        }
        validarDatosPeriodo(request.getTipo(), request.getFechaInicio(), request.getFechaFin());
        validarSolapamiento(periodo.getAnioEscolar(), request.getTipo(),
                request.getFechaInicio(), request.getFechaFin(), id);
        periodo.setTipo(request.getTipo());
        periodo.setFechaInicio(request.getFechaInicio());
        periodo.setFechaFin(request.getFechaFin());
        return respuestaPeriodo(periodoRepository.save(periodo));
    }

    @Transactional
    public PeriodoResponseDTO activarPeriodo(Integer id) {
        PeriodoMatricula periodo = obtenerPeriodo(id);
        validarSolapamiento(periodo.getAnioEscolar(), periodo.getTipo(),
                periodo.getFechaInicio(), periodo.getFechaFin(), id);
        periodo.setActivo(true);
        return respuestaPeriodo(periodoRepository.save(periodo));
    }

    @Transactional
    public PeriodoResponseDTO desactivarPeriodo(Integer id) {
        PeriodoMatricula periodo = obtenerPeriodo(id);
        periodo.setActivo(false);
        return respuestaPeriodo(periodoRepository.save(periodo));
    }

    @Transactional
    public void eliminarPeriodoEnPreparacion(Integer id) {
        PeriodoMatricula periodo = obtenerPeriodo(id);
        AnioEscolar anio = obtenerAnio(periodo.getAnioEscolar());
        if (Boolean.TRUE.equals(anio.getActivo())
                || !anio.getFechaInicio().isAfter(LocalDate.now(clock))
                || matriculaRepository.existsByAnioEscolar(anio.getAnio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede eliminar definitivamente un período no utilizado de un año en preparación");
        }
        periodoRepository.delete(periodo);
    }

    public CalendarioMatriculaDTO obtenerCalendario(Short anioSolicitado) {
        Optional<AnioEscolar> anioActivo = anioRepository.findFirstByActivoTrue();
        if (anioActivo.isEmpty()) {
            return new CalendarioMatriculaDTO(null, null, List.of(),
                    "No hay un año escolar activo configurado");
        }
        AnioEscolar activo = anioActivo.get();
        Short anioSeleccionado = anioSolicitado == null ? activo.getAnio() : anioSolicitado;
        AnioEscolar consultado = anioRepository.findByAnio(anioSeleccionado).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El año escolar solicitado no existe"));
        LocalDate hoy = LocalDate.now(clock);
        List<PeriodoDTO> periodos = periodoRepository
                .findByAnioEscolarAndActivoTrueOrderByFechaInicioAsc(anioSeleccionado).stream()
                .map(periodo -> toPeriodoDTO(periodo, hoy))
                .toList();
        return new CalendarioMatriculaDTO(toAnioDTO(activo), toAnioDTO(consultado), periodos, null);
    }

    private boolean hayPeriodoVigente(Short anio, TipoPeriodoMatricula tipo) {
        LocalDate hoy = LocalDate.now(clock);
        return periodoRepository.findByAnioEscolarAndTipoAndActivoTrue(anio, tipo).stream()
                .anyMatch(periodo -> incluyeFecha(periodo, hoy));
    }

    private boolean incluyeFecha(PeriodoMatricula periodo, LocalDate fecha) {
        return !fecha.isBefore(periodo.getFechaInicio()) && !fecha.isAfter(periodo.getFechaFin());
    }

    private AnioEscolar obtenerAnio(Short anio) {
        if (anio == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el año escolar");
        }
        return anioRepository.findByAnio(anio).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El año escolar solicitado no existe"));
    }

    private PeriodoMatricula obtenerPeriodo(Integer id) {
        return periodoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El período de matrícula no existe"));
    }

    private void validarDatosAnio(AnioEscolarRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar los datos del año escolar");
        }
        validarDatosAnio(request.getNombre(), request.getFechaInicio(), request.getFechaFin());
        if (request.getAnio() == null || request.getAnio() < 1900 || request.getAnio() > 2200
                || request.getNombre().isBlank() || request.getNombre().trim().length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El número y nombre del año escolar no son válidos");
        }
    }

    private void validarDatosAnio(String nombre, LocalDate inicio, LocalDate fin) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 20
                || inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año escolar debe tener nombre y fechas válidas; el cierre no puede preceder al inicio");
        }
    }

    private PeriodoMatricula copiarPeriodo(PeriodoMatricula origen, Short anioNuevo, Short anioOrigen) {
        PeriodoMatricula copia = new PeriodoMatricula();
        int desplazamiento = anioNuevo - anioOrigen;
        copia.setAnioEscolar(anioNuevo);
        copia.setTipo(origen.getTipo());
        try {
            copia.setFechaInicio(origen.getFechaInicio().plusYears(desplazamiento));
            copia.setFechaFin(origen.getFechaFin().plusYears(desplazamiento));
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudieron proponer fechas para el año escolar solicitado", exception);
        }
        copia.setActivo(true);
        return copia;
    }

    private void validarDatosPeriodo(TipoPeriodoMatricula tipo, LocalDate inicio, LocalDate fin) {
        if (tipo == null || inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El período debe tener tipo y fechas válidas; el cierre no puede preceder al inicio");
        }
    }

    private void validarSolapamiento(Short anio,
                                     TipoPeriodoMatricula tipo,
                                     LocalDate inicio,
                                     LocalDate fin,
                                     Integer ignorarId) {
        boolean seSolapa = periodoRepository.findByAnioEscolarAndTipo(anio, tipo).stream()
                .filter(periodo -> !java.util.Objects.equals(periodo.getId(), ignorarId))
                .anyMatch(periodo -> !fin.isBefore(periodo.getFechaInicio())
                        && !inicio.isAfter(periodo.getFechaFin()));
        if (seSolapa) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Las fechas se solapan con otro período del mismo tipo para el año escolar");
        }
    }

    private AnioEscolarResponseDTO respuestaAnio(AnioEscolar anio, Short sugeridoDesde) {
        List<PeriodoResponseDTO> periodos = periodoRepository
                .findByAnioEscolarOrderByFechaInicioAsc(anio.getAnio()).stream()
                .map(this::respuestaPeriodo)
                .toList();
        return new AnioEscolarResponseDTO(anio.getAnio(), anio.getNombre(),
                anio.getFechaInicio(), anio.getFechaFin(), Boolean.TRUE.equals(anio.getActivo()),
                sugeridoDesde, periodos);
    }

    private PeriodoResponseDTO respuestaPeriodo(PeriodoMatricula periodo) {
        return new PeriodoResponseDTO(periodo.getId(), periodo.getAnioEscolar(), periodo.getTipo(),
                periodo.getFechaInicio(), periodo.getFechaFin(), Boolean.TRUE.equals(periodo.getActivo()));
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