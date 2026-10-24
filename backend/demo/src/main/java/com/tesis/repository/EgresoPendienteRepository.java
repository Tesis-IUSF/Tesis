package com.tesis.repository;

import com.tesis.entity.EgresoPendiente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EgresoPendienteRepository extends JpaRepository<EgresoPendiente, Integer> {

    boolean existsByMatriculaOrigen_Id(Integer matriculaOrigenId);

    @EntityGraph(attributePaths = {"matriculaOrigen.estudiante",
            "matriculaOrigen.seccion.grado.nivelEducativo"})
    List<EgresoPendiente> findByAnioOrigenOrderByIdAsc(Short anioOrigen);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"matriculaOrigen.estudiante",
            "matriculaOrigen.seccion.grado.nivelEducativo", "resueltoPor",
            "matriculaDestino"})
    @Query("select e from EgresoPendiente e where e.id = :id")
    Optional<EgresoPendiente> findByIdForUpdate(@Param("id") Integer id);
}
