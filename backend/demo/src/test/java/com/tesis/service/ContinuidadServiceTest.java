package com.tesis.service;

import com.tesis.dto.ContinuidadDTO.EgresoPendienteResponseDTO;
import com.tesis.dto.ContinuidadDTO.GeneracionResponseDTO;
import com.tesis.dto.ContinuidadDTO.ResolverEgresoRequestDTO;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.Roles;
import com.tesis.entity.Seccion;
import com.tesis.entity.Usuario;
import com.tesis.repository.EgresoPendienteRepository;
import com.tesis.repository.MatriculaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContinuidadServiceTest {

    private static final short ANIO_ORIGEN = 2196;
    private static final short ANIO_DESTINO = 2197;

    @Autowired
    private ContinuidadService continuidadService;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private EgresoPendienteRepository egresoRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @Test
    void generaContinuidadesBalanceaCuposExcluyeRetiradosYRegistraUltimoGrado() {
        crearAnio(ANIO_ORIGEN, false);
        crearAnio(ANIO_DESTINO, true);
        NivelEducativo nivel = crearNivel();
        List<Grado> grados = crearGrados(nivel);
        Seccion origenPrimero = crearSeccion(grados.getFirst(), ANIO_ORIGEN, 20, "A");
        Seccion origenQuinto = crearSeccion(grados.get(4), ANIO_ORIGEN, 20, "E");
        Seccion destinoA = crearSeccion(grados.get(1), ANIO_DESTINO, 1, "A");
        Seccion destinoB = crearSeccion(grados.get(1), ANIO_DESTINO, 1, "B");

        Estudiante estudianteUno = crearEstudiante("CONT-EST-01");
        Estudiante estudianteDos = crearEstudiante("CONT-EST-02");
        Estudiante estudianteSinCupo = crearEstudiante("CONT-EST-03");
        Estudiante estudianteRetirado = crearEstudiante("CONT-EST-04");
        Estudiante estudianteQuinto = crearEstudiante("CONT-EST-05");
        crearMatricula(estudianteUno, origenPrimero, "en_proceso");
        crearMatricula(estudianteDos, origenPrimero, "completada");
        crearMatricula(estudianteSinCupo, origenPrimero, "en_proceso");
        crearMatricula(estudianteRetirado, origenPrimero, "retirada");
        Matricula origenEgreso = crearMatricula(estudianteQuinto, origenQuinto, "en_proceso");

        GeneracionResponseDTO resultado = continuidadService.generarContinuidades(ANIO_ORIGEN);

        assertEquals(2, resultado.continuidadesGeneradas());
        assertEquals(2, resultado.continuidades().size());
        assertEquals(1, resultado.asignacionPendiente().size());
        assertEquals(estudianteSinCupo.getId(),
                resultado.asignacionPendiente().getFirst().estudianteId());
        assertEquals(1, resultado.egresosPendientes().size());
        assertEquals(origenEgreso.getId(),
                resultado.egresosPendientes().getFirst().matriculaOrigenId());
        assertTrue(resultado.continuidades().stream()
                .allMatch(item -> item.gradoDestinoId().equals(grados.get(1).getId())));
        assertTrue(resultado.continuidades().stream()
                .allMatch(item -> item.seccionId().equals(destinoA.getId())
                        || item.seccionId().equals(destinoB.getId())));
        assertEquals(2, matriculaRepository.findByEstudiante_IdAndAnioEscolar(
                estudianteUno.getId(), ANIO_DESTINO).size()
                + matriculaRepository.findByEstudiante_IdAndAnioEscolar(
                estudianteDos.getId(), ANIO_DESTINO).size());
        assertTrue(matriculaRepository.findByEstudiante_IdAndAnioEscolar(
                estudianteRetirado.getId(), ANIO_DESTINO).isEmpty());

        GeneracionResponseDTO segundoIntento = continuidadService.generarContinuidades(ANIO_ORIGEN);

        assertEquals(0, segundoIntento.continuidadesGeneradas());
        assertEquals(1, segundoIntento.asignacionPendiente().size());
        assertEquals(1, egresoRepository.count());
        assertEquals(0L, Math.min(
                seccionRepositoryDisponibles(destinoA), seccionRepositoryDisponibles(destinoB)));
    }

    @Test
    void soloPermiteGenerarContinuidadesDespuesDeCerrarElAnioOrigen() {
        crearAnio(ANIO_ORIGEN, true);
        crearAnio(ANIO_DESTINO, true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> continuidadService.generarContinuidades(ANIO_ORIGEN));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void resuelveEgresoYRegistraUsuarioYFecha() {
        crearAnio(ANIO_ORIGEN, false);
        crearAnio(ANIO_DESTINO, true);
        NivelEducativo nivel = crearNivel();
        List<Grado> grados = crearGrados(nivel);
        Seccion ultimoGrado = crearSeccion(grados.get(4), ANIO_ORIGEN, 10, "F");
        Estudiante estudiante = crearEstudiante("CONT-EGRESO-01");
        Matricula matricula = crearMatricula(estudiante, ultimoGrado, "completada");
        Usuario usuario = crearUsuario();

        GeneracionResponseDTO generado = continuidadService.generarContinuidades(ANIO_ORIGEN);
        EgresoPendienteResponseDTO pendiente = generado.egresosPendientes().getFirst();
        EgresoPendienteResponseDTO resuelto = continuidadService.resolverEgreso(
                pendiente.egresoId(), new ResolverEgresoRequestDTO("egresar", null),
                usuario.getEmail());

        assertEquals("egresado", resuelto.resolucion());
        assertEquals(usuario.getId(), resuelto.resueltoPorId());
        assertEquals(LocalDate.now(clock).atStartOfDay(),
                resuelto.resueltoEn().toLocalDate().atStartOfDay());
        assertEquals(matricula.getId(), resuelto.matriculaOrigenId());
        assertEquals("egresado", continuidadService.listarEgresosPendientes(ANIO_ORIGEN)
                .getFirst().resolucion());
    }

    @Test
    void repetirUltimoGradoCreaMatriculaEnElMismoGradoYAsignaSeccion() {
        crearAnio(ANIO_ORIGEN, false);
        crearAnio(ANIO_DESTINO, true);
        NivelEducativo nivel = crearNivel();
        List<Grado> grados = crearGrados(nivel);
        Seccion ultimoGradoOrigen = crearSeccion(grados.get(4), ANIO_ORIGEN, 10, "F");
        Seccion ultimoGradoDestino = crearSeccion(grados.get(4), ANIO_DESTINO, 10, "G");
        Estudiante estudiante = crearEstudiante("CONT-REPITE-01");
        Matricula origen = crearMatricula(estudiante, ultimoGradoOrigen, "completada");
        Usuario usuario = crearUsuario();

        EgresoPendienteResponseDTO pendiente = continuidadService
                .generarContinuidades(ANIO_ORIGEN).egresosPendientes().getFirst();
        EgresoPendienteResponseDTO repeticion = continuidadService.resolverEgreso(
                pendiente.egresoId(), new ResolverEgresoRequestDTO("repetir", null),
                usuario.getEmail());

        assertEquals("repetir", repeticion.resolucion());
        assertEquals(usuario.getId(), repeticion.resueltoPorId());
        assertTrue(repeticion.matriculaDestinoId() != null);
        Matricula destino = matriculaRepository.findById(repeticion.matriculaDestinoId())
                .orElseThrow();
        assertEquals(ANIO_DESTINO, destino.getAnioEscolar());
        assertEquals(grados.get(4).getId(), destino.getSeccion().getGrado().getId());
        assertEquals(ultimoGradoDestino.getId(), destino.getSeccion().getId());
        assertEquals(origen.getId(), destino.getContinuidadDeMatricula().getId());
        assertEquals("preinscrito", destino.getEstadoMatricula());

        EgresoPendienteResponseDTO reintento = continuidadService.resolverEgreso(
                pendiente.egresoId(), new ResolverEgresoRequestDTO("repetir", null),
                usuario.getEmail());
        assertEquals(repeticion.matriculaDestinoId(), reintento.matriculaDestinoId());
        assertEquals(1, matriculaRepository.findByEstudiante_IdAndAnioEscolar(
                estudiante.getId(), ANIO_DESTINO).size());
    }

    @Test
    void mantienePendienteSinResolverSiNoHayCupoParaRepetir() {
        crearAnio(ANIO_ORIGEN, false);
        crearAnio(ANIO_DESTINO, true);
        NivelEducativo nivel = crearNivel();
        List<Grado> grados = crearGrados(nivel);
        Seccion ultimoGradoOrigen = crearSeccion(grados.get(4), ANIO_ORIGEN, 10, "F");
        Seccion ultimoGradoDestino = crearSeccion(grados.get(4), ANIO_DESTINO, 1, "G");
        Estudiante estudiante = crearEstudiante("CONT-REPITE-02");
        Estudiante estudianteOcupante = crearEstudiante("CONT-REPITE-03");
        crearMatricula(estudiante, ultimoGradoOrigen, "completada");
        crearMatricula(estudianteOcupante, ultimoGradoDestino, "preinscrito");
        Usuario usuario = crearUsuario();
        EgresoPendienteResponseDTO pendiente = continuidadService
                .generarContinuidades(ANIO_ORIGEN).egresosPendientes().getFirst();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> continuidadService.resolverEgreso(
                        pendiente.egresoId(),
                        new ResolverEgresoRequestDTO("repetir", null),
                        usuario.getEmail()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertNull(continuidadService.listarEgresosPendientes(ANIO_ORIGEN)
                .getFirst().resolucion());
    }

    @Test
    void devuelveSinAsignacionSiElGradoSiguienteNoTieneSecciones() {
        crearAnio(ANIO_ORIGEN, false);
        crearAnio(ANIO_DESTINO, true);
        NivelEducativo nivel = crearNivel();
        List<Grado> grados = crearGrados(nivel);
        Seccion origen = crearSeccion(grados.getFirst(), ANIO_ORIGEN, 10, "G");
        Estudiante estudiante = crearEstudiante("CONT-SIN-SECCION");
        crearMatricula(estudiante, origen, "completada");

        GeneracionResponseDTO resultado = continuidadService.generarContinuidades(ANIO_ORIGEN);

        assertEquals(0, resultado.continuidadesGeneradas());
        assertEquals(1, resultado.asignacionPendiente().size());
        assertEquals(grados.get(1).getId(),
                resultado.asignacionPendiente().getFirst().gradoDestinoId());
        assertFalse(resultado.asignacionPendiente().getFirst().motivo().isBlank());
    }

    private long seccionRepositoryDisponibles(Seccion seccion) {
        return entityManager.createQuery(
                        "select s.capacidadMaxima - (select count(m) from Matricula m "
                                + "where m.seccion = s and m.anioEscolar = s.anioEscolar "
                                + "and m.estadoMatricula in ('preinscrito', 'en_proceso', 'completada')) "
                                + "from Seccion s where s.id = :id", Long.class)
                .setParameter("id", seccion.getId())
                .getSingleResult();
    }

    private void crearAnio(short anio, boolean activo) {
        AnioEscolar escolar = new AnioEscolar();
        escolar.setAnio(anio);
        escolar.setNombre(anio + "-" + (anio + 1));
        escolar.setFechaInicio(LocalDate.of(anio, 9, 1));
        escolar.setFechaFin(LocalDate.of(anio + 1, 7, 15));
        escolar.setActivo(activo);
        entityManager.persist(escolar);
    }

    private NivelEducativo crearNivel() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Media General Continuidad");
        nivel.setOrdinal((short) 90);
        entityManager.persist(nivel);
        return nivel;
    }

    private List<Grado> crearGrados(NivelEducativo nivel) {
        List<Grado> grados = new ArrayList<>();
        for (short numero = 1; numero <= 5; numero++) {
            Grado grado = new Grado();
            grado.setNivelEducativo(nivel);
            grado.setNumeroGrado(numero);
            grado.setEdadMinima((short) (12 + numero - 1));
            grado.setEdadMaxima((short) (13 + numero - 1));
            grado.setActivo(true);
            entityManager.persist(grado);
            grados.add(grado);
        }
        entityManager.flush();
        return grados;
    }

    private Seccion crearSeccion(Grado grado, short anio, int capacidad, String letra) {
        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setAnioEscolar(anio);
        seccion.setCapacidadMaxima((short) capacidad);
        seccion.setLetraSeccion(letra);
        seccion.setActivo(true);
        entityManager.persist(seccion);
        entityManager.flush();
        return seccion;
    }

    private Estudiante crearEstudiante(String cedula) {
        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(cedula);
        estudiante.setNombre("Estudiante");
        estudiante.setApellido("Continuidad");
        estudiante.setFechaNacimiento(LocalDate.of(2012, 1, 1));
        estudiante.setSexo("F");
        entityManager.persist(estudiante);
        entityManager.flush();
        return estudiante;
    }

    private Matricula crearMatricula(Estudiante estudiante, Seccion seccion, String estado) {
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar(seccion.getAnioEscolar());
        matricula.setEstadoMatricula(estado);
        matricula.setTipoIngreso("regular");
        matricula.setFechaSolicitud(LocalDate.of(seccion.getAnioEscolar(), 9, 1));
        entityManager.persist(matricula);
        entityManager.flush();
        return matricula;
    }

    private Usuario crearUsuario() {
        Roles rol = new Roles();
        rol.setNombreRol("Director continuidad");
        entityManager.persist(rol);
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("director-continuidad");
        usuario.setEmail("director-continuidad@example.test");
        usuario.setPasswordHash("hash");
        usuario.setRol(rol);
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }
}
