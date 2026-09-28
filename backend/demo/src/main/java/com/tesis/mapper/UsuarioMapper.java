package com.tesis.mapper;

import com.tesis.dto.UsuarioDTO;
import com.tesis.entity.Usuario;
import com.tesis.entity.Roles;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UsuarioMapper {

    // Entity -> DTO Response (completo)
    @Mapping(source = "rol.id", target = "rolId")
    @Mapping(source = "rol.nombreRol", target = "rolNombre")
    UsuarioDTO.UsuarioResponseDTO usuarioToUsuarioResponseDTO(Usuario usuario);

    // Entity -> DTO Simple
    @Mapping(target = "tipoUsuario", expression = "java(usuario.getTipoUsuario())")
    UsuarioDTO.UsuarioSimpledDTO usuarioToUsuarioSimpledDTO(Usuario usuario);

    // DTO Request -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rol", ignore = true) // Se asigna manualmente en el servicio
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "ultimoAcceso", ignore = true)
    @Mapping(target = "bloqueadoHasta", ignore = true)
    @Mapping(target = "emailVerificado", ignore = true)
    Usuario usuarioRequestDTOToUsuario(UsuarioDTO.UsuarioRequestDTO usuarioRequestDTO);

    // Actualizar entidad existente
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rol", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    void updateUsuarioFromDTO(UsuarioDTO.UsuarioRequestDTO dto, @MappingTarget Usuario usuario);
}