package com.tesis.repository;

import com.tesis.entity.Comunicado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ComunicadoRepository extends JpaRepository<Comunicado, Integer> {

    @Query("select c from Comunicado c where c.eliminadoEn is null "
            + "and (:categoriaId is null or c.categoria.id = :categoriaId) "
            + "and (:publicado is null or c.publicado = :publicado)")
    Page<Comunicado> buscarAdministrativos(@Param("categoriaId") Integer categoriaId,
                                           @Param("publicado") Boolean publicado,
                                           Pageable pageable);

    @Query("select c from Comunicado c where c.eliminadoEn is null and c.publicado = true "
            + "and c.visiblePublico = true and c.categoria.activo = true "
            + "and c.fechaPublicacion is not null and c.fechaPublicacion <= :ahora "
            + "and (c.fechaExpiracion is null or c.fechaExpiracion >= :ahora) "
            + "and (:categoriaId is null or c.categoria.id = :categoriaId) "
            + "order by c.ordenVisualizacion asc, c.fechaPublicacion desc")
    Page<Comunicado> buscarPublicados(@Param("ahora") LocalDateTime ahora,
                                      @Param("categoriaId") Integer categoriaId,
                                      Pageable pageable);

    @Query("select c from Comunicado c where c.id = :id and c.eliminadoEn is null "
            + "and c.publicado = true and c.visiblePublico = true and c.categoria.activo = true "
            + "and c.fechaPublicacion is not null and c.fechaPublicacion <= :ahora "
            + "and (c.fechaExpiracion is null or c.fechaExpiracion >= :ahora)")
    Optional<Comunicado> buscarPublicadoPorId(@Param("id") Integer id,
                                              @Param("ahora") LocalDateTime ahora);

    boolean existsByCategoria_Id(Integer categoriaId);
}