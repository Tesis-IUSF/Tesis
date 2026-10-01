package com.tesis.repository;

import com.tesis.entity.ChecklistMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ChecklistMatriculaRepository extends JpaRepository<ChecklistMatricula, Integer> {

    List<ChecklistMatricula> findByMatricula_Id(Integer matriculaId);

    @EntityGraph(attributePaths = {"requisito", "verificadoPor"})
    List<ChecklistMatricula> findByMatricula_IdIn(Collection<Integer> matriculaIds);

    List<ChecklistMatricula> findByMatricula_IdAndCumplidoTrue(Integer matriculaId);

    long countByMatricula_IdAndCumplidoTrue(Integer matriculaId);

    Optional<ChecklistMatricula> findByMatricula_IdAndRequisito_Id(
            Integer matriculaId, Integer requisitoId);
}