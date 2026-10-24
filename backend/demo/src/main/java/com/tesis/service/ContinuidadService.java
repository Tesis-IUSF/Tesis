package com.tesis.service;

import com.tesis.dto.ContinuidadDTO.AsignacionPendienteDTO;
import com.tesis.dto.ContinuidadDTO.ContinuidadGeneradaDTO;
import com.tesis.dto.ContinuidadDTO.EgresoPendienteResponseDTO;
import com.tesis.dto.ContinuidadDTO.GeneracionResponseDTO;
import com.tesis.dto.ContinuidadDTO.ResolverEgresoRequestDTO;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.EgresoPendiente;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.Seccion;
import com.tesis.entity.Usuario;
import com.tesis.repository.AnioEscolarRepository;
import com.tesis.repository.EgresoPendienteRepository;
import com.tesis.repository.GradoRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.SeccionRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContinuidadService {

    private static final String EN_PROCESO = "en_proceso";
    private static final String COMPLETADA = "completada";
    private static final String PREINSCRITO = "preinscrito";
    private static final String REGULAR = "regular";
    private static final String EGRESAR = "egresar";
    private static final String REPETIR = "repetir";
    private static final String RESOLUCION_EGRESADO = "egresado";
    private static final String RESOLUCION_REPETIR = "repetir";
    private final AnioEscolarRepository anioRepository;
    private final MatriculaRepository matriculaRepository;
    private final GradoRepository gradoRepository;
    private final SeccionRepository seccionRepository;
    private final EgresoPendienteRepository egresoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public ContinuidadService(AnioEscolarRepository anioRepository,
                              MatriculaRepository matriculaRepository,
                              GradoRepository gradoRepository,
                              SeccionRepository seccionRepository,
                              EgresoPendienteRepository egresoRepository,
                              UsuarioRepository usuarioRepository,
                              Clock clock) {
        this.anioRepository = anioRepository;
        this.matriculaRepository = matriculaRepository;
        this.gradoRepository = gradoRepository;
        this.seccionRepository = seccionRepository;
        this.egresoRepository = egresoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    public GeneracionResponseDTO generarContinuidades(Short anioOrigen) {
        if (anioOrigen == null || anioOrigen == Short.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año escolar de origen no es válido");
        }
        short anioDestino = (short) (anioOrigen + 1);
        anioRepository.findByAnioForUpdate(anioDestino).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debe crear primero el año escolar de destino"));
        AnioEscolar origen = anioRepository.findByAnio(anioOrigen).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El año escolar de origen no existe"));
        if (Boolean.TRUE.equals(origen.getActivo())
                && origen.getFechaFin().isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Las continuidades se generan después de cerrar el año escolar de origen");
        }

        List<Matricula> historialAnual = matriculaRepository
                .findAllForContinuityByAnio(anioOrigen);
        Map<Integer, Matricula> ultimaMatriculaPorEstudiante = new LinkedHashMap<>();
        for (Matricula matricula : historialAnual) {
            ultimaMatriculaPorEstudiante.put(
                    matricula.getEstudiante().getId(), matricula);
        }

        List<Seccion> secciones = seccionRepository.findActivasDelAnioForUpdate(anioDestino);
        Map<Integer, Long> cuposPorSeccion = secciones.stream()
                .collect(Collectors.toMap(
                        seccion -> seccion.getId(),
                        seccion -> seccionRepository.calcularCuposDisponibles(seccion.getId())
                                .orElse(0L),
                        (primero, duplicado) -> primero,
                        LinkedHashMap::new));
        Map<Integer, List<Seccion>> seccionesPorGrado = secciones.stream()
                .collect(Collectors.groupingBy(
                        seccion -> seccion.getGrado().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Integer, List<Grado>> gradosActivosPorNivel = new HashMap<>();
        List<ContinuidadGeneradaDTO> generadas = new ArrayList<>();
        List<AsignacionPendienteDTO> sinAsignacion = new ArrayList<>();

        List<Matricula> candidatos = ultimaMatriculaPorEstudiante.values().stream()
                .filter(matricula -> EN_PROCESO.equals(matricula.getEstadoMatricula())
                        || COMPLETADA.equals(matricula.getEstadoMatricula()))
                .sorted(Comparator.comparing(matricula -> matricula.getEstudiante().getId()))
                .toList();

        for (Matricula matriculaOrigen : candidatos) {
            Integer estudianteId = matriculaOrigen.getEstudiante().getId();
            if (matriculaRepository.existsByEstudiante_IdAndAnioEscolar(estudianteId, anioDestino)) {
                continue;
            }

            Grado gradoOrigen = matriculaOrigen.getSeccion().getGrado();
            Integer nivelId = gradoOrigen.getNivelEducativo().getId();
            List<Grado> gradosActivos = gradosActivosPorNivel.computeIfAbsent(
                    nivelId, gradoId -> gradoRepository.findByNivelEducativo_IdAndActivoTrue(gradoId)
                            .stream()
                            .sorted(Comparator.comparing(grado -> grado.getNumeroGrado()))
                            .toList());
            Optional<Grado> ultimoGrado = gradosActivos.stream()
                    .max(Comparator.comparing(grado -> grado.getNumeroGrado()));
            if (ultimoGrado.isEmpty()) {
                sinAsignacion.add(asignacionPendiente(
                        matriculaOrigen, null, null,
                        "El nivel educativo no tiene grados activos"));
                continue;
            }

            if (Objects.equals(gradoOrigen.getNumeroGrado(), ultimoGrado.get().getNumeroGrado())) {
                crearEgresoPendiente(matriculaOrigen, anioOrigen);
                continue;
            }

            short numeroDestino = (short) (gradoOrigen.getNumeroGrado() + 1);
            Grado gradoDestino = gradosActivos.stream()
                    .filter(grado -> grado.getNumeroGrado() == numeroDestino)
                    .findFirst()
                    .orElse(null);
            if (gradoDestino == null) {
                sinAsignacion.add(asignacionPendiente(
                        matriculaOrigen, null, numeroDestino,
                        "No existe el grado siguiente configurado en este nivel"));
                continue;
            }

            Seccion seccionDestino = seleccionarSeccion(
                    seccionesPorGrado.getOrDefault(gradoDestino.getId(), List.of()),
                    cuposPorSeccion);
            if (seccionDestino == null) {
                sinAsignacion.add(asignacionPendiente(
                        matriculaOrigen, gradoDestino.getId(), numeroDestino,
                        "No hay una sección activa con cupo para el grado siguiente"));
                continue;
            }

            Matricula continuidad = new Matricula();
            continuidad.setEstudiante(matriculaOrigen.getEstudiante());
            continuidad.setAnioEscolar(anioDestino);
            continuidad.setSeccion(seccionDestino);
            continuidad.setTipoIngreso(REGULAR);
            continuidad.setEstadoMatricula(PREINSCRITO);
            continuidad.setFechaSolicitud(LocalDate.now(clock));
            continuidad.setContinuidadDeMatricula(matriculaOrigen);
            continuidad = matriculaRepository.saveAndFlush(continuidad);
            cuposPorSeccion.computeIfPresent(seccionDestino.getId(), (id, cupos) -> cupos - 1);
            generadas.add(toContinuidad(continuidad, matriculaOrigen));
        }

        return new GeneracionResponseDTO(
                anioOrigen,
                anioDestino,
                generadas.size(),
                generadas,
                sinAsignacion,
                egresoRepository.findByAnioOrigenOrderByIdAsc(anioOrigen).stream()
                        .map(this::toEgresoResponse)
                        .toList());
    }

    @Transactional(readOnly = true)
    public List<EgresoPendienteResponseDTO> listarEgresosPendientes(Short anioOrigen) {
        if (!anioRepository.existsByAnio(anioOrigen)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El año escolar de origen no existe");
        }
        return egresoRepository.findByAnioOrigenOrderByIdAsc(anioOrigen).stream()
                .map(this::toEgresoResponse)
                .toList();
    }

    public EgresoPendienteResponseDTO resolverEgreso(
            Integer egresoId, ResolverEgresoRequestDTO request, String usuarioEmail) {
        EgresoPendiente egreso = egresoRepository.findByIdForUpdate(egresoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El egreso pendiente no existe"));
        String decision = request.decision();
        if (!EGRESAR.equals(decision) && !REPETIR.equals(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La decisión debe ser 'egresar' o 'repetir'");
        }
        String resolucion = EGRESAR.equals(decision)
                ? RESOLUCION_EGRESADO : RESOLUCION_REPETIR;
        if (egreso.getResolucion() != null) {
            if (egreso.getResolucion().equals(resolucion)) {
                return toEgresoResponse(egreso);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este caso ya fue resuelto como " + egreso.getResolucion());
        }

        Usuario usuario = usuarioRepository.findByEmail(usuarioEmail).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado"));
        if (EGRESAR.equals(decision)) {
            if (request.seccionId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se debe indicar una sección para confirmar el egreso");
            }
            egreso.setResolucion(RESOLUCION_EGRESADO);
        } else {
            Matricula origen = egreso.getMatriculaOrigen();
            if (egreso.getAnioOrigen() == Short.MAX_VALUE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El año escolar de origen no es válido");
            }
            short anioDestino = (short) (egreso.getAnioOrigen() + 1);
            if (!anioRepository.existsByAnio(anioDestino)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Debe crear primero el año escolar de destino");
            }
            if (matriculaRepository.existsByEstudiante_IdAndAnioEscolar(
                    origen.getEstudiante().getId(), anioDestino)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El estudiante ya tiene una matrícula para el año escolar siguiente");
            }
            Seccion seccionDestino = seleccionarSeccionRepeticion(
                    origen.getSeccion().getGrado().getId(), anioDestino, request.seccionId());
            Matricula repeticion = new Matricula();
            repeticion.setEstudiante(origen.getEstudiante());
            repeticion.setAnioEscolar(anioDestino);
            repeticion.setSeccion(seccionDestino);
            repeticion.setTipoIngreso(REGULAR);
            repeticion.setEstadoMatricula(PREINSCRITO);
            repeticion.setFechaSolicitud(LocalDate.now(clock));
            repeticion.setContinuidadDeMatricula(origen);
            egreso.setMatriculaDestino(matriculaRepository.saveAndFlush(repeticion));
            egreso.setResolucion(RESOLUCION_REPETIR);
        }
        egreso.setResueltoPor(usuario);
        egreso.setResueltoEn(LocalDateTime.now(clock));
        return toEgresoResponse(egresoRepository.save(egreso));
    }

    private Seccion seleccionarSeccionRepeticion(
            Integer gradoId, Short anioDestino, Integer seccionId) {
        List<Seccion> secciones = seccionRepository.findActivasDelAnioForUpdate(anioDestino)
                .stream()
                .filter(seccion -> Objects.equals(seccion.getGrado().getId(), gradoId))
                .toList();
        Seccion seleccionada;
        if (seccionId == null) {
            Map<Integer, Long> cupos = secciones.stream()
                    .collect(Collectors.toMap(
                            seccion -> seccion.getId(),
                            seccion -> seccionRepository.calcularCuposDisponibles(seccion.getId())
                                    .orElse(0L)));
            seleccionada = seleccionarSeccion(secciones, cupos);
        } else {
            seleccionada = secciones.stream()
                    .filter(seccion -> seccion.getId().equals(seccionId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La sección debe estar activa, pertenecer al año siguiente y al mismo grado"));
            if (seccionRepository.calcularCuposDisponibles(seleccionada.getId()).orElse(0L) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La sección seleccionada no tiene cupos disponibles");
            }
        }
        if (seleccionada == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay una sección activa con cupo para repetir el último grado");
        }
        return seleccionada;
    }

    private EgresoPendienteResponseDTO toEgresoResponse(EgresoPendiente egreso) {
        Matricula origen = egreso.getMatriculaOrigen();
        return new EgresoPendienteResponseDTO(
                egreso.getId(),
                origen.getId(),
                origen.getEstudiante().getId(),
                origen.getEstudiante().getCedula(),
                nombreCompleto(origen),
                egreso.getAnioOrigen(),
                origen.getSeccion().getGrado().getNumeroGrado(),
                egreso.getResolucion(),
                egreso.getMatriculaDestino() == null
                        ? null : egreso.getMatriculaDestino().getId(),
                egreso.getResueltoPor() == null ? null : egreso.getResueltoPor().getId(),
                egreso.getResueltoEn());
    }

    private void crearEgresoPendiente(Matricula matriculaOrigen, Short anioOrigen) {
        if (egresoRepository.existsByMatriculaOrigen_Id(matriculaOrigen.getId())) {
            return;
        }
        EgresoPendiente egreso = new EgresoPendiente();
        egreso.setMatriculaOrigen(matriculaOrigen);
        egreso.setAnioOrigen(anioOrigen);
        egresoRepository.saveAndFlush(egreso);
    }

    private Seccion seleccionarSeccion(List<Seccion> secciones, Map<Integer, Long> cuposPorSeccion) {
        return secciones.stream()
                .filter(seccion -> cuposPorSeccion.getOrDefault(seccion.getId(), 0L) > 0)
                .max(Comparator
                        .comparingLong((Seccion seccion) ->
                                cuposPorSeccion.getOrDefault(seccion.getId(), 0L))
                        .thenComparing(seccion -> -seccion.getId()))
                .orElse(null);
    }

    private AsignacionPendienteDTO asignacionPendiente(
            Matricula matriculaOrigen,
            Integer gradoDestinoId,
            Short numeroGradoDestino,
            String motivo) {
        return new AsignacionPendienteDTO(
                matriculaOrigen.getEstudiante().getId(),
                matriculaOrigen.getEstudiante().getCedula(),
                nombreCompleto(matriculaOrigen),
                gradoDestinoId,
                numeroGradoDestino,
                motivo);
    }

    private ContinuidadGeneradaDTO toContinuidad(
            Matricula continuidad, Matricula origen) {
        return new ContinuidadGeneradaDTO(
                continuidad.getId(),
                continuidad.getEstudiante().getId(),
                continuidad.getEstudiante().getCedula(),
                nombreCompleto(continuidad),
                origen.getSeccion().getGrado().getId(),
                origen.getSeccion().getGrado().getNumeroGrado(),
                continuidad.getSeccion().getGrado().getId(),
                continuidad.getSeccion().getGrado().getNumeroGrado(),
                continuidad.getSeccion().getId(),
                continuidad.getSeccion().getLetraSeccion());
    }

    private String nombreCompleto(Matricula matricula) {
        return matricula.getEstudiante().getNombre() + " "
                + matricula.getEstudiante().getApellido();
    }
}
