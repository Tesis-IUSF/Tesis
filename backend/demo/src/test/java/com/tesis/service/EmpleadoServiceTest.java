package com.tesis.service;

import com.tesis.dto.EmpleadoDTO.EmpleadoRequestDTO;
import com.tesis.dto.EmpleadoDTO.EmpleadoResponseDTO;
import com.tesis.entity.AsignacionTurno;
import com.tesis.entity.Cargo;
import com.tesis.entity.Departamento;
import com.tesis.entity.Empleado;
import com.tesis.entity.Turno;
import com.tesis.entity.Usuario;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.AsignacionTurnoRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.EmpleadoRepository;
import com.tesis.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private CargoRepository cargoRepository;

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AsignacionTurnoRepository asignacionTurnoRepository;

    private EmpleadoService empleadoService;

    @BeforeEach
    void setUp() {
        empleadoService = new EmpleadoService(
            empleadoRepository, cargoRepository, departamentoRepository, usuarioRepository,
            asignacionTurnoRepository);
        lenient().when(asignacionTurnoRepository.buscarVigentesPorEmpleados(any(), any()))
            .thenReturn(List.of());
    }

    @Test
    void listarDevuelveEmpleadosConDatosDeRelaciones() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(empleado), PageRequest.of(0, 25), 1));

        List<EmpleadoResponseDTO> respuesta = empleadoService.listar(PageRequest.of(0, 25)).getContent();

        assertEquals(1, respuesta.size());
        assertEquals(empleado.getId(), respuesta.get(0).getId());
        assertEquals(empleado.getCargo().getId(), respuesta.get(0).getCargoId());
        assertEquals("Docente", respuesta.get(0).getCargoNombre());
        assertEquals(empleado.getDepartamento().getId(), respuesta.get(0).getDepartamentoId());
        assertEquals("Académico", respuesta.get(0).getDepartamentoNombre());
        assertEquals(empleado.getUsuario().getId(), respuesta.get(0).getUsuarioId());
    }

    @Test
    void listarIncluyeTurnoVigenteEnLaRespuesta() {
        Empleado empleado = empleadoExistente();
        Turno turno = new Turno();
        turno.setId(4);
        turno.setNombre("Matutino");
        AsignacionTurno asignacion = new AsignacionTurno();
        asignacion.setPersonal(empleado);
        asignacion.setTurno(turno);
        when(empleadoRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(empleado), PageRequest.of(0, 25), 1));
        when(asignacionTurnoRepository.buscarVigentesPorEmpleados(List.of(3), LocalDate.now()))
                .thenReturn(List.of(asignacion));

        EmpleadoResponseDTO respuesta = empleadoService.listar(PageRequest.of(0, 25)).getContent().get(0);

        assertEquals(4, respuesta.getTurnoId());
        assertEquals("Matutino", respuesta.getTurnoNombre());
    }

    @Test
    void obtenerDevuelveEmpleadoExistente() {
        when(empleadoRepository.findById(3)).thenReturn(Optional.of(empleadoExistente()));

        EmpleadoResponseDTO respuesta = empleadoService.obtener(3);

        assertEquals(3, respuesta.getId());
        assertEquals("12345678", respuesta.getCedula());
    }

    @Test
    void obtenerEmpleadoInexistenteLanzaNotFound() {
        when(empleadoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class, () -> empleadoService.obtener(99));

        assertEquals(HttpStatus.NOT_FOUND, excepcion.getStatusCode());
    }

    @Test
    void crearGuardaEmpleadoYAsignaRelaciones() {
        EmpleadoRequestDTO solicitud = solicitudEmpleado(10, 20, 30);
        Cargo cargo = cargo(10, "Docente");
        Departamento departamento = departamento(20, "Académico");
        Usuario usuario = new Usuario();
        usuario.setId(30);
        when(empleadoRepository.existsByCedula("12345678")).thenReturn(false);
        when(cargoRepository.findById(10)).thenReturn(Optional.of(cargo));
        when(departamentoRepository.findById(20)).thenReturn(Optional.of(departamento));
        when(empleadoRepository.existsByUsuario_Id(30)).thenReturn(false);
        when(usuarioRepository.findById(30)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(invocation -> {
            Empleado empleado = invocation.getArgument(0);
            empleado.setId(5);
            return empleado;
        });

        EmpleadoResponseDTO respuesta = empleadoService.crear(solicitud);

        assertEquals(5, respuesta.getId());
        assertEquals(10, respuesta.getCargoId());
        assertEquals(20, respuesta.getDepartamentoId());
        assertEquals(30, respuesta.getUsuarioId());
        assertTrue(respuesta.getActivo());
        verify(empleadoRepository).save(any(Empleado.class));
    }

    @Test
    void crearConCedulaDuplicadaLanzaConflictSinGuardar() {
        when(empleadoRepository.existsByCedula("12345678")).thenReturn(true);

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> empleadoService.crear(solicitudEmpleado(null, null, null)));

        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        verify(empleadoRepository, never()).save(any(Empleado.class));
    }

    @Test
    void crearConCargoInexistenteLanzaBadRequest() {
        when(empleadoRepository.existsByCedula("12345678")).thenReturn(false);
        when(cargoRepository.findById(404)).thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> empleadoService.crear(solicitudEmpleado(404, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
        verify(empleadoRepository, never()).save(any(Empleado.class));
    }

    @Test
    void crearConUsuarioYaVinculadoLanzaConflict() {
        when(empleadoRepository.existsByCedula("12345678")).thenReturn(false);
        when(empleadoRepository.existsByUsuario_Id(30)).thenReturn(true);

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> empleadoService.crear(solicitudEmpleado(null, null, 30)));

        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        verify(usuarioRepository, never()).findById(30);
        verify(empleadoRepository, never()).save(any(Empleado.class));
    }

    @Test
    void actualizarModificaEmpleadoYConservaActivoSiNoSeEnvia() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findById(3)).thenReturn(Optional.of(empleado));
        when(empleadoRepository.existsByCedulaAndIdNot("87654321", 3)).thenReturn(false);
        when(empleadoRepository.save(any(Empleado.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EmpleadoRequestDTO solicitud = solicitudEmpleado(null, null, null);
        solicitud.setNombre("María");
        solicitud.setCedula("87654321");

        EmpleadoResponseDTO respuesta = empleadoService.actualizar(3, solicitud);

        assertEquals("María", respuesta.getNombre());
        assertEquals("87654321", respuesta.getCedula());
        assertTrue(respuesta.getActivo());
        assertNull(respuesta.getCargoId());
        verify(empleadoRepository).save(empleado);
    }

    @Test
    void actualizarConCedulaDeOtroEmpleadoLanzaConflictSinGuardar() {
        when(empleadoRepository.findById(3)).thenReturn(Optional.of(empleadoExistente()));
        when(empleadoRepository.existsByCedulaAndIdNot("12345678", 3)).thenReturn(true);

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> empleadoService.actualizar(3, solicitudEmpleado(null, null, null)));

        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        verify(empleadoRepository, never()).save(any(Empleado.class));
    }

    @Test
    void eliminarBorraEmpleadoExistente() {
        Empleado empleado = empleadoExistente();
        when(empleadoRepository.findById(3)).thenReturn(Optional.of(empleado));

        empleadoService.eliminar(3);

        verify(empleadoRepository).delete(empleado);
    }

    @Test
    void eliminarEmpleadoInexistenteLanzaNotFoundSinBorrar() {
        when(empleadoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class, () -> empleadoService.eliminar(99));

        assertEquals(HttpStatus.NOT_FOUND, excepcion.getStatusCode());
        verify(empleadoRepository, never()).delete(any(Empleado.class));
    }

    private EmpleadoRequestDTO solicitudEmpleado(Integer cargoId, Integer departamentoId, Integer usuarioId) {
        return new EmpleadoRequestDTO(
                "Ana", "Pérez", "12345678", "ana@example.com", "555-0100",
                LocalDate.of(1990, 5, 12), "F", cargoId, departamentoId,
                LocalDate.of(2020, 1, 15), null, usuarioId, null);
    }

    private Empleado empleadoExistente() {
        Empleado empleado = new Empleado();
        empleado.setId(3);
        empleado.setNombre("Ana");
        empleado.setApellido("Pérez");
        empleado.setCedula("12345678");
        empleado.setActivo(true);
        empleado.setCargo(cargo(10, "Docente"));
        empleado.setDepartamento(departamento(20, "Académico"));
        Usuario usuario = new Usuario();
        usuario.setId(30);
        empleado.setUsuario(usuario);
        return empleado;
    }

    private Cargo cargo(Integer id, String nombre) {
        Cargo cargo = new Cargo();
        cargo.setId(id);
        cargo.setNombreCargo(nombre);
        return cargo;
    }

    private Departamento departamento(Integer id, String nombre) {
        Departamento departamento = new Departamento();
        departamento.setId(id);
        departamento.setNombre(nombre);
        return departamento;
    }
}