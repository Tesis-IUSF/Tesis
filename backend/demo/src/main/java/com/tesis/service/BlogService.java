package com.tesis.service;

import com.tesis.dto.BlogDTO;
import com.tesis.entity.CategoriaComunicado;
import com.tesis.entity.Comunicado;
import com.tesis.entity.Usuario;
import com.tesis.repository.CategoriaComunicadoRepository;
import com.tesis.repository.ComunicadoRepository;
import com.tesis.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@Transactional
public class BlogService {

    private final CategoriaComunicadoRepository categoriaRepository;
    private final ComunicadoRepository comunicadoRepository;
    private final UsuarioRepository usuarioRepository;

    public BlogService(CategoriaComunicadoRepository categoriaRepository,
                       ComunicadoRepository comunicadoRepository,
                       UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.comunicadoRepository = comunicadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<BlogDTO.CategoriaResponseDTO> listarCategoriasPublicas(Pageable pageable) {
        return categoriaRepository.findByActivoTrue(pageable).map(this::categoriaResponse);
    }

    @Transactional(readOnly = true)
    public Page<BlogDTO.CategoriaResponseDTO> listarCategorias(Pageable pageable) {
        return categoriaRepository.findAll(pageable).map(this::categoriaResponse);
    }

    public BlogDTO.CategoriaResponseDTO crearCategoria(BlogDTO.CategoriaRequestDTO request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
        CategoriaComunicado categoria = new CategoriaComunicado();
        actualizarCategoria(categoria, request);
        return categoriaResponse(categoriaRepository.save(categoria));
    }

    public BlogDTO.CategoriaResponseDTO actualizarCategoria(Integer id,
                                                             BlogDTO.CategoriaRequestDTO request) {
        CategoriaComunicado categoria = buscarCategoria(id);
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
        actualizarCategoria(categoria, request);
        return categoriaResponse(categoriaRepository.save(categoria));
    }

    public void eliminarCategoria(Integer id) {
        buscarCategoria(id);
        if (comunicadoRepository.existsByCategoria_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar una categoría que tiene comunicados; desactívela en su lugar");
        }
        categoriaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<BlogDTO.ComunicadoResponseDTO> listarComunicados(Integer categoriaId,
                                                                 Boolean publicado,
                                                                 Pageable pageable) {
        return comunicadoRepository.buscarAdministrativos(categoriaId, publicado, pageable)
                .map(this::comunicadoResponse);
    }

    @Transactional(readOnly = true)
    public BlogDTO.ComunicadoResponseDTO obtenerComunicado(Integer id) {
        return comunicadoResponse(buscarComunicado(id));
    }

    @Transactional(readOnly = true)
    public Page<BlogDTO.ComunicadoResponseDTO> listarPublicaciones(Integer categoriaId,
                                                                   Pageable pageable) {
        return comunicadoRepository.buscarPublicados(LocalDateTime.now(), categoriaId, pageable)
                .map(this::comunicadoResponse);
    }

    @Transactional(readOnly = true)
    public BlogDTO.ComunicadoResponseDTO obtenerPublicacion(Integer id) {
        return comunicadoRepository.buscarPublicadoPorId(id, LocalDateTime.now())
                .map(this::comunicadoResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Publicación no encontrada"));
    }

    public BlogDTO.ComunicadoResponseDTO crearComunicado(BlogDTO.ComunicadoRequestDTO request,
                                                          String emailCreador) {
        Comunicado comunicado = new Comunicado();
        comunicado.setCreadoPor(buscarUsuario(emailCreador));
        actualizarComunicado(comunicado, request);
        return comunicadoResponse(comunicadoRepository.save(comunicado));
    }

    public BlogDTO.ComunicadoResponseDTO actualizarComunicado(Integer id,
                                                              BlogDTO.ComunicadoRequestDTO request) {
        Comunicado comunicado = buscarComunicado(id);
        actualizarComunicado(comunicado, request);
        return comunicadoResponse(comunicadoRepository.save(comunicado));
    }

    public void eliminarComunicado(Integer id) {
        Comunicado comunicado = buscarComunicado(id);
        comunicado.setEliminadoEn(LocalDateTime.now());
        comunicadoRepository.save(comunicado);
    }

    private void actualizarCategoria(CategoriaComunicado categoria,
                                     BlogDTO.CategoriaRequestDTO request) {
        categoria.setNombre(request.nombre().trim());
        categoria.setDescripcion(request.descripcion());
        categoria.setIconoUrl(request.iconoUrl());
        categoria.setColorHex(request.colorHex() == null ? "#3498db" : request.colorHex());
        categoria.setOrdinal(request.ordinal() == null ? (short) 0 : request.ordinal());
        categoria.setActivo(request.activo() == null || request.activo());
    }

    private void actualizarComunicado(Comunicado comunicado,
                                      BlogDTO.ComunicadoRequestDTO request) {
        if (request.fechaPublicacion() != null && request.fechaExpiracion() != null
                && request.fechaExpiracion().isBefore(request.fechaPublicacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de expiración no puede ser anterior a la fecha de publicación");
        }
        CategoriaComunicado categoria = buscarCategoria(request.categoriaId());
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pueden asignar comunicados a una categoría inactiva");
        }

        boolean publicado = Boolean.TRUE.equals(request.publicado());
        LocalDateTime fechaPublicacion = request.fechaPublicacion();
        if (publicado && fechaPublicacion == null) {
            fechaPublicacion = LocalDateTime.now();
        }
        comunicado.setTitulo(request.titulo().trim());
        comunicado.setContenidoHtml(request.contenidoHtml());
        comunicado.setResumen(request.resumen());
        comunicado.setCategoria(categoria);
        comunicado.setPublicado(publicado);
        comunicado.setFechaPublicacion(fechaPublicacion);
        comunicado.setFechaExpiracion(request.fechaExpiracion());
        comunicado.setImagenUrl(request.imagenUrl());
        comunicado.setOrdenVisualizacion(request.ordenVisualizacion() == null ? 0 : request.ordenVisualizacion());
        comunicado.setVisiblePublico(Boolean.TRUE.equals(request.visiblePublico()));
    }

    private BlogDTO.CategoriaResponseDTO categoriaResponse(CategoriaComunicado categoria) {
        return new BlogDTO.CategoriaResponseDTO(categoria.getId(), categoria.getNombre(),
                categoria.getDescripcion(), categoria.getIconoUrl(), categoria.getColorHex(),
                categoria.getOrdinal(), categoria.getActivo(), categoria.getCreadoEn());
    }

    private BlogDTO.ComunicadoResponseDTO comunicadoResponse(Comunicado comunicado) {
        return new BlogDTO.ComunicadoResponseDTO(comunicado.getId(), comunicado.getTitulo(),
                comunicado.getContenidoHtml(), comunicado.getResumen(), comunicado.getCategoria().getId(),
                comunicado.getCategoria().getNombre(), comunicado.getCreadoPor().getId(),
                comunicado.getPublicado(), comunicado.getFechaPublicacion(), comunicado.getFechaExpiracion(),
                comunicado.getImagenUrl(), comunicado.getOrdenVisualizacion(), comunicado.getVisiblePublico(),
                comunicado.getCreadoEn(), comunicado.getActualizadoEn());
    }

    private CategoriaComunicado buscarCategoria(Integer id) {
        return categoriaRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private Comunicado buscarComunicado(Integer id) {
        return comunicadoRepository.findById(id)
                .filter(comunicado -> comunicado.getEliminadoEn() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Comunicado no encontrado"));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }
}