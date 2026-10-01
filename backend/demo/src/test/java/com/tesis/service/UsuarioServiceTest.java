package com.tesis.service;

import com.tesis.dto.UsuarioDTO;
import com.tesis.entity.Roles;
import com.tesis.entity.TipoUsuario;
import com.tesis.entity.Usuario;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolesRepository rolesRepository;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, rolesRepository);
    }

    @Test
    void listarUsuariosRetornaListado() {
        Roles rol = new Roles();
        rol.setId(2);
        rol.setNombreRol("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombreUsuario("admin");
        usuario.setEmail("admin@test.com");
        usuario.setRol(rol);
        usuario.setTipoUsuario(TipoUsuario.PERSONAL);
        usuario.setActivo(true);

        when(usuarioRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(usuario), PageRequest.of(0, 25), 1));

        List<UsuarioDTO.UsuarioResponseDTO> resultado = usuarioService.listar(PageRequest.of(0, 25)).getContent();

        assertEquals(1, resultado.size());
        assertEquals("admin", resultado.get(0).getNombreUsuario());
        assertEquals(2, resultado.get(0).getRolId());
    }

    @Test
    void crearUsuarioValidaDuplicadoEmail() {
        UsuarioDTO.UsuarioRequestDTO request = UsuarioDTO.UsuarioRequestDTO.builder()
                .nombreUsuario("usuario1")
                .email("usuario1@test.com")
                .passwordHash("abc")
                .rolId(2)
                .tipoUsuario(TipoUsuario.PERSONAL)
                .activo(true)
                .build();

        when(usuarioRepository.existsByEmail("usuario1@test.com")).thenReturn(true);

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> usuarioService.crear(request));

        assertTrue(ex.getMessage().contains("El email ya está registrado"));
    }

    @Test
    void crearUsuarioAceptaPasswordEnCampoClaro() {
        UsuarioDTO.UsuarioRequestDTO request = UsuarioDTO.UsuarioRequestDTO.builder()
                .nombreUsuario("usuario2")
                .email("usuario2@test.com")
                .password("abc123")
                .rolId(2)
                .tipoUsuario(TipoUsuario.PERSONAL)
                .activo(true)
                .build();

        Roles rol = new Roles();
        rol.setId(2);
        rol.setNombreRol("ADMIN");

        Usuario guardado = new Usuario();
        guardado.setId(5);
        guardado.setNombreUsuario("usuario2");
        guardado.setEmail("usuario2@test.com");
        guardado.setPasswordHash("abc123");
        guardado.setRol(rol);
        guardado.setTipoUsuario(TipoUsuario.PERSONAL);
        guardado.setActivo(true);

        when(rolesRepository.findById(2)).thenReturn(Optional.of(rol));
        when(usuarioRepository.existsByEmail("usuario2@test.com")).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuario("usuario2")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        UsuarioDTO.UsuarioResponseDTO resultado = usuarioService.crear(request);

        assertEquals("usuario2", resultado.getNombreUsuario());
        assertEquals(2, resultado.getRolId());
        assertEquals("abc123", request.getPassword());
    }
}
