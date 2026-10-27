package com.tesis.service;

import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NominaEstudiantilServiceTest {

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private RelacionEstudianteRepresentanteRepository relacionRepository;

    @Mock
    private CalendarioMatriculaService calendarioService;

    @Mock
    private NominaEstudiantilPdfGenerator pdfGenerator;

    private NominaEstudiantilService service;

    @BeforeEach
    void setUp() {
        service = new NominaEstudiantilService(
                matriculaRepository, relacionRepository, calendarioService, pdfGenerator);
    }

    @Test
    void usaAlcanceAcademicoYColumnasSolicitadasYPorDefectoIncluyeTodosLosEstados() {
        when(matriculaRepository.listarParaNominaEstudiantil(
                (short) 2026, List.of("preinscrito", "en_proceso", "completada", "retirada", "anulada"),
                2, 4, 8)).thenReturn(List.of());
        when(pdfGenerator.generar(eq((short) 2026), eq(List.of()), eq(Map.of()),
                eq(List.of("nombre", "edad")))).thenReturn(new byte[] {1, 2, 3});

        byte[] pdf = service.generar(
                (short) 2026, null, 2, 4, 8, List.of("edad", "nombre", "edad"));

        assertEquals(3, pdf.length);
        verify(calendarioService).validarAnioExistente((short) 2026);
        verify(matriculaRepository).listarParaNominaEstudiantil(
                (short) 2026, List.of("preinscrito", "en_proceso", "completada", "retirada", "anulada"),
                2, 4, 8);
        verify(pdfGenerator).generar(eq((short) 2026), eq(List.of()), eq(Map.of()),
                eq(List.of("nombre", "edad")));
    }

    @Test
    void rechazaEstadosYColumnasFueraDeLaListaPermitida() {
        ResponseStatusException errorEstado = assertThrows(ResponseStatusException.class,
                () -> service.generar((short) 2026, List.of("otro"), null, null, null, List.of("nombre")));
        assertEquals(HttpStatus.BAD_REQUEST, errorEstado.getStatusCode());

        ResponseStatusException errorColumna = assertThrows(ResponseStatusException.class,
                () -> service.generar((short) 2026, List.of("completada"), null, null, null,
                        List.of("direccion")));
        assertEquals(HttpStatus.BAD_REQUEST, errorColumna.getStatusCode());
        verify(matriculaRepository, never()).listarParaNominaEstudiantil(
                eq((short) 2026), anyList(), eq(null), eq(null), eq(null));
    }
}
