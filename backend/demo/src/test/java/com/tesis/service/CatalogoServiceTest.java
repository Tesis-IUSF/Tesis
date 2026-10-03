package com.tesis.service;

import com.tesis.entity.Departamento;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.Turno;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.GradoRepository;
import com.tesis.repository.NivelEducativoRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;
    @Mock
    private CargoRepository cargoRepository;
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

    private CatalogoService catalogoService;

    @BeforeEach
    void setUp() {
        catalogoService = new CatalogoService(
                departamentoRepository,
                cargoRepository,
                turnoRepository,
                rolesRepository,
                nivelEducativoRepository,
                gradoRepository,
                seccionRepository,
                requisitoMatriculaRepository
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
}
