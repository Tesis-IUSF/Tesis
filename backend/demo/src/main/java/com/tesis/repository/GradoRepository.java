package com.tesis.repository;

import com.tesis.entity.Grado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface GradoRepository extends JpaRepository<Grado, Integer> {

    @Override
    @EntityGraph(attributePaths = "nivelEducativo")
    Page<Grado> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "nivelEducativo")
    List<Grado> findByNivelEducativo_Id(Integer nivelEducativoId);

    @EntityGraph(attributePaths = "nivelEducativo")
    List<Grado> findByNivelEducativo_IdAndActivoTrue(Integer nivelEducativoId);

    @EntityGraph(attributePaths = "nivelEducativo")
    Page<Grado> findByNivelEducativo_Id(Integer nivelEducativoId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from Grado g where g.id = :id")
    Optional<Grado> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
