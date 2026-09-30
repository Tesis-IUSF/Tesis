package com.tesis.repository;

import com.tesis.entity.ChecklistMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistMatriculaRepository extends JpaRepository<ChecklistMatricula, Integer> {

    List<ChecklistMatricula> findByMatricula_Id(Integer matriculaId);

    List<ChecklistMatricula> findByMatricula_IdAndCumplidoTrue(Integer matriculaId);

    long countByMatricula_IdAndCumplidoTrue(Integer matriculaId);

    Optional<ChecklistMatricula> findByMatricula_IdAndRequisito_Id(
            Integer matriculaId, Integer requisitoId);
}