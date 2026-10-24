package com.tesis.service;

import com.tesis.entity.Cargo;
import com.tesis.entity.Empleado;
import com.tesis.repository.AsignacionTurnoRepository;
import com.tesis.repository.CredencialQrRepository;
import com.tesis.repository.EmpleadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CarnetConsultaServiceTest {

    @Test
    void filtraCarnetsPorCargoYTurnoVigente() {
        Empleado conCargoYTurno = empleado(1, 10, "Ana");
        Empleado conCargoDiferente = empleado(2, 20, "Luis");
        EmpleadoRepository empleadoRepository = mock(EmpleadoRepository.class);
        CredencialQrRepository credencialRepository = mock(CredencialQrRepository.class);
        AsignacionTurnoRepository asignacionRepository = mock(AsignacionTurnoRepository.class);
        when(empleadoRepository.findByActivoTrue(any()))
                .thenReturn(List.of(conCargoYTurno, conCargoDiferente));
        when(credencialRepository.findByActivaTrueOrderByCreadaEnDesc()).thenReturn(List.of());
        when(asignacionRepository.buscarPersonalIdsVigentesPorTurno(
                org.mockito.ArgumentMatchers.eq(5), any()))
                .thenReturn(List.of(1));

        CarnetConsultaService service = new CarnetConsultaService(
                empleadoRepository, credencialRepository, asignacionRepository);

        Page<?> resultado = service.buscar(
                null, null, 10, 5, null, 30, PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, ((com.tesis.dto.CarnetDTO.EmpleadoCarnetResponseDTO)
                resultado.getContent().getFirst()).getEmpleadoId());
    }

    private Empleado empleado(Integer id, Integer cargoId, String nombre) {
        Cargo cargo = new Cargo();
        cargo.setId(cargoId);
        cargo.setNombreCargo("Cargo " + cargoId);
        Empleado empleado = new Empleado();
        empleado.setId(id);
        empleado.setNombre(nombre);
        empleado.setApellido("Prueba");
        empleado.setCedula("C-" + id);
        empleado.setActivo(true);
        empleado.setCargo(cargo);
        return empleado;
    }
}
