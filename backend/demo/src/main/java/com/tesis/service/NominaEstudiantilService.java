package com.tesis.service;

import com.tesis.entity.Matricula;
import com.tesis.entity.Representante;
import com.tesis.repository.MatriculaRepository;
import com.tesis.repository.RelacionEstudianteRepresentanteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NominaEstudiantilService {

    private static final List<String> ESTADOS_VALIDOS =
            List.of("preinscrito", "en_proceso", "completada", "retirada", "anulada");
    private static final List<String> COLUMNAS_VALIDAS = List.of(
            "nombre", "cedula", "fechaNacimiento", "edad", "sexo", "nivelGradoSeccion",
            "tipoIngreso", "representanteNombre", "representanteCedula", "representanteTelefono");

    private final MatriculaRepository matriculaRepository;
    private final RelacionEstudianteRepresentanteRepository relacionRepository;
    private final CalendarioMatriculaService calendarioService;
    private final NominaEstudiantilPdfGenerator pdfGenerator;

    public NominaEstudiantilService(MatriculaRepository matriculaRepository,
                                    RelacionEstudianteRepresentanteRepository relacionRepository,
                                    CalendarioMatriculaService calendarioService,
                                    NominaEstudiantilPdfGenerator pdfGenerator) {
        this.matriculaRepository = matriculaRepository;
        this.relacionRepository = relacionRepository;
        this.calendarioService = calendarioService;
        this.pdfGenerator = pdfGenerator;
    }

    public byte[] generar(Short anioEscolar,
                         List<String> estados,
                         Integer nivelId,
                         Integer gradoId,
                         Integer seccionId,
                         List<String> columnas) {
        if (anioEscolar == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el año escolar");
        }
        calendarioService.validarAnioExistente(anioEscolar);

        List<String> estadosSeleccionados = normalizarEstados(estados);
        List<String> columnasSeleccionadas = normalizarColumnas(columnas);
        List<Matricula> matriculas = matriculaRepository.listarParaNominaEstudiantil(
                anioEscolar, estadosSeleccionados, nivelId, gradoId, seccionId);
        List<Integer> idsEstudiantes = matriculas.stream()
                .map(matricula -> matricula.getEstudiante().getId())
                .distinct()
                .toList();
        Map<Integer, Representante> representantes = idsEstudiantes.isEmpty()
                ? Map.of()
                : relacionRepository.findByEstudiante_IdInAndActivoTrueAndRelacionPrincipalTrue(idsEstudiantes)
                        .stream()
                        .collect(Collectors.toMap(
                                relacion -> relacion.getEstudiante().getId(),
                                relacion -> relacion.getRepresentante(),
                                (primero, ignorado) -> primero));

        return pdfGenerator.generar(anioEscolar, matriculas, representantes, columnasSeleccionadas);
    }

    private List<String> normalizarEstados(List<String> estados) {
        if (estados == null || estados.isEmpty()) {
            return ESTADOS_VALIDOS;
        }
        Set<String> seleccionados = new LinkedHashSet<>(estados);
        if (!ESTADOS_VALIDOS.containsAll(seleccionados)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de matrícula no válido");
        }
        return List.copyOf(seleccionados);
    }

    private List<String> normalizarColumnas(List<String> columnas) {
        if (columnas == null || columnas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar al menos una columna para la nómina");
        }
        Set<String> seleccionadas = new LinkedHashSet<>(columnas);
        if (!COLUMNAS_VALIDAS.containsAll(seleccionadas)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Columna de nómina no válida");
        }
        return COLUMNAS_VALIDAS.stream().filter(seleccionadas::contains).toList();
    }
}
