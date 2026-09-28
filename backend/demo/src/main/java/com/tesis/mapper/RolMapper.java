package com.tesis.mapper;

import com.tesis.dto.RolDTO;
import com.tesis.entity.Roles;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RolMapper {

    // Entity -> DTO
    RolDTO.RolResponseDTO rolToRolResponseDTO(Roles rol);

    // DTO -> Entity
    Roles rolRequestDTOToRol(RolDTO.RolRequestDTO rolRequestDTO);

    // Actualizar entidad existente
    void updateRolFromDTO(RolDTO.RolRequestDTO dto, @MappingTarget Roles rol);
}