package com.tesis.service;

import com.tesis.dto.CarnetDTO.EmpleadoCarnetResponseDTO;
import com.tesis.repository.AsignacionTurnoRepository;
import com.tesis.entity.CredencialQr;
import com.tesis.entity.Empleado;
import com.tesis.repository.CredencialQrRepository;
import com.tesis.repository.EmpleadoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CarnetConsultaService {

    private static final int MAX_DIAS_AVISO = 365;
    private final EmpleadoRepository empleadoRepository;
    private final CredencialQrRepository credencialRepository;
    private final AsignacionTurnoRepository asignacionTurnoRepository;

    public CarnetConsultaService(EmpleadoRepository empleadoRepository,
                                 CredencialQrRepository credencialRepository,
                                 AsignacionTurnoRepository asignacionTurnoRepository) {
        this.empleadoRepository = empleadoRepository;
        this.credencialRepository = credencialRepository;
        this.asignacionTurnoRepository = asignacionTurnoRepository;
    }

    public Page<EmpleadoCarnetResponseDTO> buscar(String busqueda,
                                                   Integer departamentoId,
                                                   Integer cargoId,
                                                   Integer turnoId,
                                                   String estadoQr,
                                                   int diasAviso,
                                                   Pageable pageable) {
        if (diasAviso < 1 || diasAviso > MAX_DIAS_AVISO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "diasAviso debe estar entre 1 y " + MAX_DIAS_AVISO);
        }
        String estado = estadoQr == null || estadoQr.isBlank()
                ? null : estadoQr.trim().toLowerCase(Locale.ROOT);
        if (estado != null && !List.of("sin_carnet", "vigente", "por_vencer", "vencido").contains(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado QR no reconocido");
        }

        String termino = busqueda == null || busqueda.isBlank()
                ? null : busqueda.trim().toLowerCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limiteAviso = ahora.plusDays(diasAviso);
        Map<Integer, CredencialQr> credencialesActivas = new HashMap<>();
        for (CredencialQr credencial : credencialRepository.findByActivaTrueOrderByCreadaEnDesc()) {
            credencialesActivas.putIfAbsent(credencial.getEmpleado().getId(), credencial);
        }
        Set<Integer> empleadosConTurno = turnoId == null
                ? Set.of()
                : new HashSet<>(asignacionTurnoRepository.buscarPersonalIdsVigentesPorTurno(
                        turnoId, LocalDate.now()));

        List<EmpleadoCarnetResponseDTO> resultados = new ArrayList<>();
        for (Empleado empleado : empleadoRepository.findByActivoTrue(
                Sort.by(Sort.Order.asc("apellido"), Sort.Order.asc("nombre")))) {
            if (departamentoId != null && (empleado.getDepartamento() == null
                    || !departamentoId.equals(empleado.getDepartamento().getId()))) {
                continue;
            }
            if (cargoId != null && (empleado.getCargo() == null
                    || !cargoId.equals(empleado.getCargo().getId()))) {
                continue;
            }
            if (turnoId != null && !empleadosConTurno.contains(empleado.getId())) {
                continue;
            }
            CredencialQr credencial = credencialesActivas.get(empleado.getId());
            String estadoEmpleado = determinarEstado(credencial, ahora, limiteAviso);
            if (estado != null && !estado.equals(estadoEmpleado)) {
                continue;
            }
            String nombreCompleto = empleado.getNombre() + " " + empleado.getApellido();
            if (termino != null && !nombreCompleto.toLowerCase(Locale.ROOT).contains(termino)
                    && !empleado.getCedula().toLowerCase(Locale.ROOT).contains(termino)) {
                continue;
            }
            resultados.add(new EmpleadoCarnetResponseDTO(
                    empleado.getId(), nombreCompleto, empleado.getCedula(),
                    empleado.getCargo() == null ? null : empleado.getCargo().getNombreCargo(),
                    empleado.getDepartamento() == null ? null : empleado.getDepartamento().getNombre(),
                    estadoEmpleado, credencial == null ? null : credencial.getExpiraEn()));
        }

        int inicio = (int) Math.min(pageable.getOffset(), resultados.size());
        int fin = Math.min(inicio + pageable.getPageSize(), resultados.size());
        return new PageImpl<>(resultados.subList(inicio, fin), pageable, resultados.size());
    }

    private String determinarEstado(CredencialQr credencial,
                                    LocalDateTime ahora,
                                    LocalDateTime limiteAviso) {
        if (credencial == null) {
            return "sin_carnet";
        }
        if (!credencial.getExpiraEn().isAfter(ahora)) {
            return "vencido";
        }
        if (!credencial.getExpiraEn().isAfter(limiteAviso)) {
            return "por_vencer";
        }
        return "vigente";
    }
}