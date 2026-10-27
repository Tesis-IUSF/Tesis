package com.tesis.service;

import com.tesis.entity.Departamento;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.Grado;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Seccion;
import com.tesis.entity.Turno;
import com.tesis.repository.AnioEscolarRepository;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.ChecklistMatriculaRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.GradoRepository;
import com.tesis.repository.NivelEducativoRepository;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RequisitoMatriculaRepository;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.SeccionRepository;
import com.tesis.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private ChecklistMatriculaRepository checklistMatriculaRepository;
    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private RolesRepository rolesRepository;
    @Mock
    private NivelEducativoRepository nivelEducativoRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private SeccionRepository seccionRepository;
    @Mock
    private RequisitoMatriculaRepository requisitoMatriculaRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AnioEscolarRepository anioEscolarRepository;

    private CatalogoService catalogoService;

    @BeforeEach
    void setUp() {
        catalogoService = new CatalogoService(
                departamentoRepository,
                cargoRepository,
                empleadoRepository,
                turnoRepository,
                rolesRepository,
                nivelEducativoRepository,
                gradoRepository,
                seccionRepository,
                requisitoMatriculaRepository,
                checklistMatriculaRepository,
                matriculaRepository,
                anioEscolarRepository
        );
    }

    @Test
    void crearDepartamentoGuardaLaEntidad() {
        Departamento departamento = new Departamento();
        departamento.setNombre("Administración");
        when(departamentoRepository.save(any(Departamento.class))).thenAnswer(invocation -> {
            Departamento value = invocation.getArgument(0);
            value.setId(1);
            return value;
        });

        Departamento resultado = catalogoService.crearDepartamento(departamento);

        assertNotNull(resultado.getId());
        assertEquals("Administración", resultado.getNombre());
    }

    @Test
    void listarNivelesEducativosRetornaListado() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Primaria");
        when(nivelEducativoRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(nivel), PageRequest.of(0, 25), 1));

        List<NivelEducativo> resultado = catalogoService
            .listarNivelesEducativos(PageRequest.of(0, 25)).getContent();

        assertEquals(1, resultado.size());
        assertEquals("Primaria", resultado.get(0).getNombre());
    }

    @Test
    void crearTurnoCompletaDefaultsDeDiasLaborablesYTolerancias() {
        Turno turno = new Turno();
        turno.setNombre("  Turno mañana administrativo  ");
        turno.setHoraEntrada(java.time.LocalTime.of(7, 0));
        turno.setHoraSalida(java.time.LocalTime.of(12, 45));
        turno.setLunes(null);
        turno.setMartes(null);
        turno.setMiercoles(null);
        turno.setJueves(null);
        turno.setViernes(null);
        turno.setSabado(null);
        turno.setDomingo(null);
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turno resultado = catalogoService.crearTurno(turno);

        assertEquals("Turno mañana administrativo", resultado.getNombre());
        assertEquals((short) 0, resultado.getToleranciaMin());
        assertEquals(true, resultado.getLunes());
        assertEquals(true, resultado.getViernes());
        assertEquals(false, resultado.getSabado());
        assertEquals(false, resultado.getDomingo());
        assertEquals(true, resultado.getActivo());
    }

    @Test
    void crearTurnoRechazaHorasAusentes() {
        Turno turno = new Turno();
        turno.setNombre("Turno incompleto");

        org.springframework.web.server.ResponseStatusException error = assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> catalogoService.crearTurno(turno));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void actualizarTurnoConservaCamposOpcionalesOmitidos() {
        Turno actual = new Turno();
        actual.setId(4);
        actual.setNombre("Turno mañana administrativo");
        actual.setHoraEntrada(java.time.LocalTime.of(7, 0));
        actual.setHoraSalida(java.time.LocalTime.of(12, 45));
        actual.setToleranciaMin((short) 15);
        actual.setMinutosSalidaAnticipadaPermitidos((short) 10);
        actual.setLunes(true);
        actual.setViernes(true);
        actual.setSabado(false);
        actual.setDomingo(false);
        actual.setActivo(true);
        when(turnoRepository.findById(4)).thenReturn(Optional.of(actual));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Turno cambios = new Turno();
        cambios.setNombre("  Turno mañana administración  ");
        cambios.setToleranciaMin(null);
        cambios.setMinutosSalidaAnticipadaPermitidos(null);
        cambios.setRequiereJustificacionTardanza(null);
        cambios.setLunes(null);
        cambios.setMartes(null);
        cambios.setMiercoles(null);
        cambios.setJueves(null);
        cambios.setViernes(null);
        cambios.setSabado(null);
        cambios.setDomingo(null);
        cambios.setActivo(null);

        Turno resultado = catalogoService.actualizarTurno(4, cambios);

        assertEquals("Turno mañana administración", resultado.getNombre());
        assertEquals((short) 15, resultado.getToleranciaMin());
        assertEquals(true, resultado.getLunes());
        assertEquals(false, resultado.getSabado());
    }

    @Test
    void validaYNormalizaTiposDeIngresoDeLosRequisitos() {
        when(requisitoMatriculaRepository.save(any(RequisitoMatricula.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RequisitoMatricula valido = new RequisitoMatricula();
        valido.setNombre("Documento según ingreso");
        valido.setAplicaTipoIngreso(" traslado, nuevo_ingreso ");

        RequisitoMatricula guardado = catalogoService.crearRequisitoMatricula(valido);

        assertEquals("nuevo_ingreso,traslado", guardado.getAplicaTipoIngreso());

        RequisitoMatricula invalido = new RequisitoMatricula();
        invalido.setNombre("Tipo no admitido");
        invalido.setAplicaTipoIngreso("nuevo_ingreso,pendiente_documentos");
        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.crearRequisitoMatricula(invalido));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void impideEliminarRequisitoYaUtilizadoYDevuelve409() {
        when(requisitoMatriculaRepository.existsById(17)).thenReturn(true);
        when(checklistMatriculaRepository.existsByRequisito_Id(17)).thenReturn(true);

        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.eliminarRequisitoMatricula(17));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("El requisito ya se usó; desactívalo", error.getReason());
        verify(requisitoMatriculaRepository, never()).deleteById(17);
    }

    @Test
    void eliminaRequisitoQueAunNoSeHaUtilizado() {
        when(requisitoMatriculaRepository.existsById(18)).thenReturn(true);
        when(checklistMatriculaRepository.existsByRequisito_Id(18)).thenReturn(false);

        catalogoService.eliminarRequisitoMatricula(18);

        verify(requisitoMatriculaRepository).deleteById(18);
    }

    @Test
    void impideEliminarDepartamentoConEmpleadosOCargosAsociados() {
        when(departamentoRepository.existsById(21)).thenReturn(true);
        when(empleadoRepository.existsByDepartamento_Id(21)).thenReturn(false);
        when(cargoRepository.existsByDepartamento_Id(21)).thenReturn(true);

        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.eliminarDepartamento(21));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("No se puede eliminar el departamento porque tiene cargos o empleados asociados",
                error.getReason());
        verify(departamentoRepository, never()).deleteById(21);
    }

    @Test
    void impideEliminarCargoConEmpleadosAsociados() {
        when(cargoRepository.existsById(31)).thenReturn(true);
        when(empleadoRepository.existsByCargo_Id(31)).thenReturn(true);

        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.eliminarCargo(31));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("No se puede eliminar el cargo porque tiene empleados asociados", error.getReason());
        verify(cargoRepository, never()).deleteById(31);
    }

    @Test
    void eliminaDepartamentoOCargoSinDependencias() {
        when(departamentoRepository.existsById(22)).thenReturn(true);
        when(cargoRepository.existsById(32)).thenReturn(true);
        when(empleadoRepository.existsByDepartamento_Id(22)).thenReturn(false);
        when(cargoRepository.existsByDepartamento_Id(22)).thenReturn(false);
        when(empleadoRepository.existsByCargo_Id(32)).thenReturn(false);

        catalogoService.eliminarDepartamento(22);
        catalogoService.eliminarCargo(32);

        verify(departamentoRepository).deleteById(22);
        verify(cargoRepository).deleteById(32);
    }

    @Test
    void rechazaRangoDeEdadInvalidoAlCrearGrado() {
        Grado grado = new Grado();
        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(3);
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) 1);
        grado.setEdadMinima((short) 9);
        grado.setEdadMaxima((short) 8);

        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.crearGrado(grado));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatusCode());

        grado.setEdadMinima((short) 0);
        grado.setEdadMaxima((short) 8);
        error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.crearGrado(grado));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatusCode());

        grado.setEdadMinima((short) 1);
        grado.setEdadMaxima((short) 21);
        error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.crearGrado(grado));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaLetraDeSeccionQueNoSeaUnaMayuscula() {
        Seccion seccion = new Seccion();
        Grado grado = new Grado();
        grado.setId(3);
        seccion.setGrado(grado);
        seccion.setLetraSeccion("1");
        seccion.setAnioEscolar((short) 2026);

        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.crearSeccion(seccion));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void noPermiteDesactivarGradoConMatriculasActivasEnElAnioVigente() {
        Grado actual = new Grado();
        actual.setId(5);
        actual.setActivo(true);
        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(2);
        when(gradoRepository.findByIdForUpdate(5)).thenReturn(Optional.of(actual));
        when(nivelEducativoRepository.findById(2)).thenReturn(Optional.of(nivel));
        when(seccionRepository.findByGrado_IdForUpdate(5)).thenReturn(List.of(new Seccion()));
        AnioEscolar anioActivo = new AnioEscolar();
        anioActivo.setAnio((short) 2026);
        when(anioEscolarRepository.findFirstByActivoTrue()).thenReturn(Optional.of(anioActivo));
        when(matriculaRepository.existsBySeccion_Grado_IdAndAnioEscolarAndEstadoMatriculaIn(
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq((short) 2026),
                any())).thenReturn(true);

        Grado solicitud = new Grado();
        solicitud.setNivelEducativo(nivel);
        solicitud.setNumeroGrado((short) 1);
        solicitud.setEdadMinima((short) 5);
        solicitud.setEdadMaxima((short) 8);
        solicitud.setActivo(false);
        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.actualizarGrado(5, solicitud));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatusCode());
        verify(seccionRepository, never()).saveAll(any());
    }

    @Test
    void desactivarGradoSinMatriculasVigentesDesactivaSusSecciones() {
        Grado actual = new Grado();
        actual.setId(5);
        actual.setActivo(true);
        NivelEducativo nivel = new NivelEducativo();
        nivel.setId(2);
        Seccion seccion = new Seccion();
        seccion.setActivo(true);
        when(gradoRepository.findByIdForUpdate(5)).thenReturn(Optional.of(actual));
        when(nivelEducativoRepository.findById(2)).thenReturn(Optional.of(nivel));
        when(seccionRepository.findByGrado_IdForUpdate(5)).thenReturn(List.of(seccion));
        when(anioEscolarRepository.findFirstByActivoTrue()).thenReturn(Optional.empty());
        when(gradoRepository.save(any(Grado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Grado solicitud = new Grado();
        solicitud.setNivelEducativo(nivel);
        solicitud.setNumeroGrado((short) 1);
        solicitud.setEdadMinima((short) 5);
        solicitud.setEdadMaxima((short) 8);
        solicitud.setActivo(false);

        catalogoService.actualizarGrado(5, solicitud);

        assertEquals(false, seccion.getActivo());
        assertEquals(false, actual.getActivo());
        verify(seccionRepository).saveAll(List.of(seccion));
    }

    @Test
    void noPermiteEliminarSeccionConMatriculasHistoricas() {
        Seccion seccion = new Seccion();
        seccion.setId(12);
        when(seccionRepository.findByIdForUpdate(12)).thenReturn(Optional.of(seccion));
        when(matriculaRepository.existsBySeccion_Id(12)).thenReturn(true);

        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> catalogoService.eliminarSeccion(12));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatusCode());
        verify(seccionRepository, never()).deleteById(12);
    }
}
