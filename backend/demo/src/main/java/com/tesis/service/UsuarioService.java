package com.tesis.service;

import com.tesis.dto.UsuarioDTO;
import com.tesis.entity.Roles;
import com.tesis.entity.TipoUsuario;
import com.tesis.entity.Usuario;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolesRepository rolesRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolesRepository rolesRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolesRepository = rolesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDTO.UsuarioResponseDTO> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO.UsuarioResponseDTO obtener(Integer id) {
        return toResponse(buscarUsuario(id));
    }

    public UsuarioDTO.UsuarioResponseDTO crear(UsuarioDTO.UsuarioRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud es obligatoria");
        }
        validarCampos(request, true);
        validarUnicidad(null, request.getEmail(), request.getNombreUsuario());

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(request.getNombreUsuario().trim());
        usuario.setEmail(request.getEmail().trim());
        usuario.setPasswordHash(passwordEncoder.encode(obtenerPassword(request)));
        usuario.setRol(buscarRol(request.getRolId()));
        usuario.setTipoUsuario(request.getTipoUsuario() == null ? TipoUsuario.PERSONAL : request.getTipoUsuario());
        usuario.setActivo(request.getActivo() == null || request.getActivo());
        usuario.setEmailVerificado(false);

        return toResponse(usuarioRepository.save(usuario));
    }

    public UsuarioDTO.UsuarioResponseDTO actualizar(Integer id, UsuarioDTO.UsuarioRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud es obligatoria");
        }
        Usuario usuario = buscarUsuario(id);
        validarCampos(request, false);
        validarUnicidad(id, request.getEmail(), request.getNombreUsuario());

        usuario.setNombreUsuario(request.getNombreUsuario().trim());
        usuario.setEmail(request.getEmail().trim());

        String password = obtenerPassword(request);
        if (password != null && !password.isBlank()) {
            validarLongitudPassword(password);
            usuario.setPasswordHash(passwordEncoder.encode(password));
        }
        usuario.setRol(buscarRol(request.getRolId()));
        usuario.setTipoUsuario(request.getTipoUsuario() == null ? TipoUsuario.PERSONAL : request.getTipoUsuario());
        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    public void eliminar(Integer id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    private Usuario buscarUsuario(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Roles buscarRol(Integer rolId) {
        if (rolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el rol del usuario");
        }
        return rolesRepository.findById(rolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no encontrado"));
    }

    private void validarCampos(UsuarioDTO.UsuarioRequestDTO request, boolean passwordObligatoria) {
        if (request.getNombreUsuario() == null || request.getNombreUsuario().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de usuario es obligatorio");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email es obligatorio");
        }
        if (passwordObligatoria
                && (obtenerPassword(request) == null || obtenerPassword(request).isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }
        String password = obtenerPassword(request);
        if (password != null && !password.isBlank()) {
            validarLongitudPassword(password);
        }
    }

    private void validarLongitudPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La contraseña no puede superar 72 bytes UTF-8");
        }
    }

    private String obtenerPassword(UsuarioDTO.UsuarioRequestDTO request) {
        String password = request.getPassword();
        if (password == null || password.isBlank()) {
            password = request.getPasswordHash();
        }
        return password;
    }

    private void validarUnicidad(Integer excludeId, String email, String nombreUsuario) {
        if (usuarioRepository.existsByEmail(email.trim())
                && (excludeId == null || !usuarioRepository.findById(excludeId).map(Usuario::getEmail).orElse("").equalsIgnoreCase(email.trim()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario.trim())
                && (excludeId == null || !usuarioRepository.findById(excludeId).map(Usuario::getNombreUsuario).orElse("").equalsIgnoreCase(nombreUsuario.trim()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");
        }
    }

    private UsuarioDTO.UsuarioResponseDTO toResponse(Usuario usuario) {
        return UsuarioDTO.UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nombreUsuario(usuario.getNombreUsuario())
                .email(usuario.getEmail())
                .rolId(usuario.getRol() == null ? null : usuario.getRol().getId())
                .rolNombre(usuario.getRol() == null ? null : usuario.getRol().getNombreRol())
                .tipoUsuario(usuario.getTipoUsuario())
                .activo(usuario.getActivo())
                .emailVerificado(usuario.getEmailVerificado())
                .ultimoAcceso(usuario.getUltimoAcceso())
                .bloqueadoHasta(usuario.getBloqueadoHasta())
                .creadoEn(usuario.getCreadoEn())
                .actualizadoEn(usuario.getActualizadoEn())
                .build();
    }
}
