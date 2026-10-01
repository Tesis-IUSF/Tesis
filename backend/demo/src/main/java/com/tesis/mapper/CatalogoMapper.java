package com.tesis.mapper;

import com.tesis.dto.CatalogoDTO;
import com.tesis.entity.Cargo;
import com.tesis.entity.Departamento;
import com.tesis.entity.Grado;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Seccion;
import com.tesis.entity.Turno;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CatalogoMapper {

    CatalogoDTO.DepartamentoResponseDTO departamentoToDepartamentoResponseDTO(Departamento departamento);

    @Mapping(target = "id", ignore = true)
    Departamento departamentoRequestDTOToDepartamento(CatalogoDTO.DepartamentoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    void updateDepartamentoFromDTO(CatalogoDTO.DepartamentoRequestDTO dto, @MappingTarget Departamento departamento);

    @Mapping(source = "departamento.id", target = "departamentoId")
    @Mapping(source = "departamento.nombre", target = "nombreDepartamento")
    CatalogoDTO.CargoResponseDTO cargoToCargoResponseDTO(Cargo cargo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "departamento", ignore = true)
    Cargo cargoRequestDTOToCargo(CatalogoDTO.CargoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "departamento", ignore = true)
    void updateCargoFromDTO(CatalogoDTO.CargoRequestDTO dto, @MappingTarget Cargo cargo);

    CatalogoDTO.TurnoResponseDTO turnoToTurnoResponseDTO(Turno turno);

    @Mapping(target = "id", ignore = true)
    Turno turnoRequestDTOToTurno(CatalogoDTO.TurnoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    void updateTurnoFromDTO(CatalogoDTO.TurnoRequestDTO dto, @MappingTarget Turno turno);

    CatalogoDTO.NivelEducativoResponseDTO nivelEducativoToNivelEducativoResponseDTO(NivelEducativo nivelEducativo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    NivelEducativo nivelEducativoRequestDTOToNivelEducativo(CatalogoDTO.NivelEducativoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    void updateNivelEducativoFromDTO(CatalogoDTO.NivelEducativoRequestDTO dto, @MappingTarget NivelEducativo nivelEducativo);

    @Mapping(source = "nivelEducativo.id", target = "nivelEducativoId")
    @Mapping(source = "nivelEducativo.nombre", target = "nivelEducativoNombre")
    CatalogoDTO.GradoResponseDTO gradoToGradoResponseDTO(Grado grado);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nivelEducativo", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    Grado gradoRequestDTOToGrado(CatalogoDTO.GradoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nivelEducativo", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    void updateGradoFromDTO(CatalogoDTO.GradoRequestDTO dto, @MappingTarget Grado grado);

    @Mapping(source = "grado.id", target = "gradoId")
    @Mapping(source = "grado.nombreEspecial", target = "gradoNombre")
    @Mapping(source = "docentePrincipal.id", target = "docentePrincipalId")
    @Mapping(source = "turno.id", target = "turnoId")
    CatalogoDTO.SeccionResponseDTO seccionToSeccionResponseDTO(Seccion seccion);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "grado", ignore = true)
    @Mapping(target = "docentePrincipal", ignore = true)
    @Mapping(target = "turno", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    Seccion seccionRequestDTOToSeccion(CatalogoDTO.SeccionRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "grado", ignore = true)
    @Mapping(target = "docentePrincipal", ignore = true)
    @Mapping(target = "turno", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    void updateSeccionFromDTO(CatalogoDTO.SeccionRequestDTO dto, @MappingTarget Seccion seccion);

    @Mapping(source = "nivelEducativo.id", target = "nivelEducativoId")
    @Mapping(source = "nivelEducativo.nombre", target = "nivelEducativoNombre")
    CatalogoDTO.RequisitoMatriculaResponseDTO requisitoMatriculaToRequisitoMatriculaResponseDTO(RequisitoMatricula requisitoMatricula);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nivelEducativo", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    RequisitoMatricula requisitoMatriculaRequestDTOToRequisitoMatricula(CatalogoDTO.RequisitoMatriculaRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nivelEducativo", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    void updateRequisitoMatriculaFromDTO(CatalogoDTO.RequisitoMatriculaRequestDTO dto, @MappingTarget RequisitoMatricula requisitoMatricula);
}
