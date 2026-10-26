package com.tesis.controller;

import com.tesis.dto.CatalogoDTO;
import com.tesis.dto.PaginacionDTO;
import com.tesis.dto.RolDTO;
import com.tesis.entity.Cargo;
import com.tesis.entity.Departamento;
import com.tesis.entity.Grado;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Roles;
import com.tesis.entity.Seccion;
import com.tesis.entity.Turno;
import com.tesis.mapper.CatalogoMapper;
import com.tesis.mapper.RolMapper;
import com.tesis.service.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final CatalogoMapper catalogoMapper;
    private final RolMapper rolMapper;

    public CatalogoController(CatalogoService catalogoService, CatalogoMapper catalogoMapper, RolMapper rolMapper) {
        this.catalogoService = catalogoService;
        this.catalogoMapper = catalogoMapper;
        this.rolMapper = rolMapper;
    }

    @GetMapping("/departamentos")
    @Cacheable(cacheNames = "catalogos", key = "'departamentos:' + #p0.page + ':' + #p0.size")
    public PaginacionDTO.Respuesta<CatalogoDTO.DepartamentoResponseDTO> listarDepartamentos(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(catalogoService.listarDepartamentos(paginacion.toPageable()),
                catalogoMapper::departamentoToDepartamentoResponseDTO);
    }

    @PostMapping("/departamentos")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<CatalogoDTO.DepartamentoResponseDTO> crearDepartamento(@Valid @RequestBody CatalogoDTO.DepartamentoRequestDTO request) {
        Departamento departamento = catalogoMapper.departamentoRequestDTOToDepartamento(request);
        Departamento creado = catalogoService.crearDepartamento(departamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMapper.departamentoToDepartamentoResponseDTO(creado));
    }

    @PutMapping("/departamentos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public CatalogoDTO.DepartamentoResponseDTO actualizarDepartamento(@PathVariable Integer id,
                                                                    @Valid @RequestBody CatalogoDTO.DepartamentoRequestDTO request) {
        Departamento departamento = catalogoMapper.departamentoRequestDTOToDepartamento(request);
        return catalogoMapper.departamentoToDepartamentoResponseDTO(catalogoService.actualizarDepartamento(id, departamento));
    }

    @DeleteMapping("/departamentos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<Void> eliminarDepartamento(@PathVariable Integer id) {
        catalogoService.eliminarDepartamento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cargos")
    @Cacheable(cacheNames = "catalogos", key = "'cargos:' + #p0.page + ':' + #p0.size")
    public PaginacionDTO.Respuesta<CatalogoDTO.CargoResponseDTO> listarCargos(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(catalogoService.listarCargos(paginacion.toPageable()),
                catalogoMapper::cargoToCargoResponseDTO);
    }

    @PostMapping("/cargos")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<CatalogoDTO.CargoResponseDTO> crearCargo(@Valid @RequestBody CatalogoDTO.CargoRequestDTO request) {
        Cargo cargo = catalogoMapper.cargoRequestDTOToCargo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMapper.cargoToCargoResponseDTO(catalogoService.crearCargo(cargo)));
    }

    @PutMapping("/cargos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public CatalogoDTO.CargoResponseDTO actualizarCargo(@PathVariable Integer id, @Valid @RequestBody CatalogoDTO.CargoRequestDTO request) {
        Cargo cargo = catalogoMapper.cargoRequestDTOToCargo(request);
        return catalogoMapper.cargoToCargoResponseDTO(catalogoService.actualizarCargo(id, cargo));
    }

    @DeleteMapping("/cargos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<Void> eliminarCargo(@PathVariable Integer id) {
        catalogoService.eliminarCargo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/turnos")
    @Cacheable(cacheNames = "catalogos", key = "'turnos:' + #p0.page + ':' + #p0.size")
    public PaginacionDTO.Respuesta<CatalogoDTO.TurnoResponseDTO> listarTurnos(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(catalogoService.listarTurnos(paginacion.toPageable()),
                catalogoMapper::turnoToTurnoResponseDTO);
    }

    @PostMapping("/turnos")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<CatalogoDTO.TurnoResponseDTO> crearTurno(@Valid @RequestBody CatalogoDTO.TurnoRequestDTO request) {
        Turno turno = catalogoMapper.turnoRequestDTOToTurno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMapper.turnoToTurnoResponseDTO(catalogoService.crearTurno(turno)));
    }

    @PutMapping("/turnos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public CatalogoDTO.TurnoResponseDTO actualizarTurno(@PathVariable Integer id, @Valid @RequestBody CatalogoDTO.TurnoRequestDTO request) {
        Turno turno = catalogoMapper.turnoRequestDTOToTurno(request);
        return catalogoMapper.turnoToTurnoResponseDTO(catalogoService.actualizarTurno(id, turno));
    }

    @DeleteMapping("/turnos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<Void> eliminarTurno(@PathVariable Integer id) {
        catalogoService.eliminarTurno(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    @Cacheable(cacheNames = "catalogos", key = "'roles:' + #p0.page + ':' + #p0.size")
    public PaginacionDTO.Respuesta<RolDTO.RolResponseDTO> listarRoles(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(catalogoService.listarRoles(paginacion.toPageable()), rolMapper::rolToRolResponseDTO);
    }

    @PostMapping("/roles")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<RolDTO.RolResponseDTO> crearRol(@Valid @RequestBody RolDTO.RolRequestDTO request) {
        Roles roles = rolMapper.rolRequestDTOToRol(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rolMapper.rolToRolResponseDTO(catalogoService.crearRol(roles)));
    }

    @PutMapping("/roles/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public RolDTO.RolResponseDTO actualizarRol(@PathVariable Integer id, @Valid @RequestBody RolDTO.RolRequestDTO request) {
        Roles roles = rolMapper.rolRequestDTOToRol(request);
        return rolMapper.rolToRolResponseDTO(catalogoService.actualizarRol(id, roles));
    }

    @DeleteMapping("/roles/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<Void> eliminarRol(@PathVariable Integer id) {
        catalogoService.eliminarRol(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/niveles-educativos")
    @Cacheable(cacheNames = "catalogos", key = "'niveles:' + #p0.page + ':' + #p0.size")
    public PaginacionDTO.Respuesta<CatalogoDTO.NivelEducativoResponseDTO> listarNivelesEducativos(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(catalogoService.listarNivelesEducativos(paginacion.toPageable()),
                catalogoMapper::nivelEducativoToNivelEducativoResponseDTO);
    }

    @PostMapping("/niveles-educativos")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<CatalogoDTO.NivelEducativoResponseDTO> crearNivelEducativo(@Valid @RequestBody CatalogoDTO.NivelEducativoRequestDTO request) {
        NivelEducativo nivelEducativo = catalogoMapper.nivelEducativoRequestDTOToNivelEducativo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMapper.nivelEducativoToNivelEducativoResponseDTO(catalogoService.crearNivelEducativo(nivelEducativo)));
    }

    @PutMapping("/niveles-educativos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public CatalogoDTO.NivelEducativoResponseDTO actualizarNivelEducativo(@PathVariable Integer id,
                                                                         @Valid @RequestBody CatalogoDTO.NivelEducativoRequestDTO request) {
        NivelEducativo nivelEducativo = catalogoMapper.nivelEducativoRequestDTOToNivelEducativo(request);
        return catalogoMapper.nivelEducativoToNivelEducativoResponseDTO(catalogoService.actualizarNivelEducativo(id, nivelEducativo));
    }

    @DeleteMapping("/niveles-educativos/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<Void> eliminarNivelEducativo(@PathVariable Integer id) {
        catalogoService.eliminarNivelEducativo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grados")
    @Cacheable(cacheNames = "catalogos", key = "'grados:' + #p0 + ':' + #p1.page + ':' + #p1.size")
    public PaginacionDTO.Respuesta<CatalogoDTO.GradoResponseDTO> listarGrados(
            @RequestParam(required = false) Integer nivelEducativoId,
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        if (nivelEducativoId != null) {
            return paginar(catalogoService.listarGradosPorNivel(nivelEducativoId, pageable),
                    catalogoMapper::gradoToGradoResponseDTO);
        }
        return paginar(catalogoService.listarGrados(pageable), catalogoMapper::gradoToGradoResponseDTO);
    }

    @PostMapping("/grados")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<CatalogoDTO.GradoResponseDTO> crearGrado(@Valid @RequestBody CatalogoDTO.GradoRequestDTO request) {
        Grado grado = catalogoMapper.gradoRequestDTOToGrado(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMapper.gradoToGradoResponseDTO(catalogoService.crearGrado(grado)));
    }

    @PutMapping("/grados/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public CatalogoDTO.GradoResponseDTO actualizarGrado(@PathVariable Integer id, @Valid @RequestBody CatalogoDTO.GradoRequestDTO request) {
        Grado grado = catalogoMapper.gradoRequestDTOToGrado(request);
        return catalogoMapper.gradoToGradoResponseDTO(catalogoService.actualizarGrado(id, grado));
    }

    @DeleteMapping("/grados/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<Void> eliminarGrado(@PathVariable Integer id) {
        catalogoService.eliminarGrado(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/secciones")
    public PaginacionDTO.Respuesta<CatalogoDTO.SeccionResponseDTO> listarSecciones(
            @RequestParam(required = false) Integer gradoId,
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        Pageable pageable = paginacion.toPageable();
        if (gradoId != null) {
            return seccionesConOcupacion(catalogoService.listarSeccionesPorGrado(gradoId, pageable));
        }
        return seccionesConOcupacion(catalogoService.listarSecciones(pageable));
    }

    private PaginacionDTO.Respuesta<CatalogoDTO.SeccionResponseDTO> seccionesConOcupacion(
            Page<Seccion> secciones) {
        Map<Integer, Long> matriculados = catalogoService.contarMatriculasActivasPorSecciones(
                secciones.getContent().stream().map(Seccion::getId).toList());
        Page<CatalogoDTO.SeccionResponseDTO> respuesta = secciones.map(seccion -> {
            CatalogoDTO.SeccionResponseDTO dto = catalogoMapper.seccionToSeccionResponseDTO(seccion);
            long cantidad = matriculados.getOrDefault(seccion.getId(), 0L);
            dto.setMatriculados(cantidad);
            dto.setCuposDisponibles(Math.max(0L, seccion.getCapacidadMaxima() - cantidad));
            return dto;
        });
        return PaginacionDTO.Respuesta.desde(respuesta);
    }

    @PostMapping("/secciones")
    public ResponseEntity<CatalogoDTO.SeccionResponseDTO> crearSeccion(@Valid @RequestBody CatalogoDTO.SeccionRequestDTO request) {
        Seccion seccion = catalogoMapper.seccionRequestDTOToSeccion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seccionConOcupacion(catalogoService.crearSeccion(seccion)));
    }

    @PutMapping("/secciones/{id}")
    public CatalogoDTO.SeccionResponseDTO actualizarSeccion(@PathVariable Integer id, @Valid @RequestBody CatalogoDTO.SeccionRequestDTO request) {
        Seccion seccion = catalogoMapper.seccionRequestDTOToSeccion(request);
        return seccionConOcupacion(catalogoService.actualizarSeccion(id, seccion));
    }

    private CatalogoDTO.SeccionResponseDTO seccionConOcupacion(Seccion seccion) {
        CatalogoDTO.SeccionResponseDTO dto = catalogoMapper.seccionToSeccionResponseDTO(seccion);
        long matriculados = catalogoService.contarMatriculasActivasPorSecciones(List.of(seccion.getId()))
                .getOrDefault(seccion.getId(), 0L);
        dto.setMatriculados(matriculados);
        dto.setCuposDisponibles(Math.max(0L, seccion.getCapacidadMaxima() - matriculados));
        return dto;
    }

    @DeleteMapping("/secciones/{id}")
    public ResponseEntity<Void> eliminarSeccion(@PathVariable Integer id) {
        catalogoService.eliminarSeccion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requisitos-matricula")
    @Cacheable(cacheNames = "catalogos", key = "'requisitos:' + #p0.page + ':' + #p0.size")
    public PaginacionDTO.Respuesta<CatalogoDTO.RequisitoMatriculaResponseDTO> listarRequisitosMatricula(
            @ModelAttribute PaginacionDTO.Solicitud paginacion) {
        return paginar(catalogoService.listarRequisitosMatricula(paginacion.toPageable()),
                catalogoMapper::requisitoMatriculaToRequisitoMatriculaResponseDTO);
    }

    @PostMapping("/requisitos-matricula")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<CatalogoDTO.RequisitoMatriculaResponseDTO> crearRequisitoMatricula(@Valid @RequestBody CatalogoDTO.RequisitoMatriculaRequestDTO request) {
        RequisitoMatricula requisitoMatricula = catalogoMapper.requisitoMatriculaRequestDTOToRequisitoMatricula(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoMapper.requisitoMatriculaToRequisitoMatriculaResponseDTO(catalogoService.crearRequisitoMatricula(requisitoMatricula)));
    }

    @PutMapping("/requisitos-matricula/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public CatalogoDTO.RequisitoMatriculaResponseDTO actualizarRequisitoMatricula(@PathVariable Integer id,
                                                                                @Valid @RequestBody CatalogoDTO.RequisitoMatriculaRequestDTO request) {
        RequisitoMatricula requisitoMatricula = catalogoMapper.requisitoMatriculaRequestDTOToRequisitoMatricula(request);
        return catalogoMapper.requisitoMatriculaToRequisitoMatriculaResponseDTO(catalogoService.actualizarRequisitoMatricula(id, requisitoMatricula));
    }

    @DeleteMapping("/requisitos-matricula/{id}")
    @CacheEvict(cacheNames = "catalogos", allEntries = true)
    public ResponseEntity<Void> eliminarRequisitoMatricula(@PathVariable Integer id) {
        catalogoService.eliminarRequisitoMatricula(id);
        return ResponseEntity.noContent().build();
    }

    private <S, T> PaginacionDTO.Respuesta<T> paginar(Page<S> pagina, Function<S, T> mapper) {
        return PaginacionDTO.Respuesta.desde(pagina.map(mapper));
    }
}
