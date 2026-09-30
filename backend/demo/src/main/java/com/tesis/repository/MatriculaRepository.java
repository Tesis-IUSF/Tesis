package com.tesis.repository;

import com.tesis.entity.Matricula;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

    List<Matricula> findByEstudiante_IdAndAnioEscolar(Integer estudianteId, Short anioEscolar);

        List<Matricula> findByEstudiante_IdAndEstadoMatriculaAndAnioEscolar(
            Integer estudianteId, String estadoMatricula, Short anioEscolar);

        List<Matricula> findByEstadoMatricula(String estadoMatricula);

    List<Matricula> findByAnioEscolarAndEstadoMatricula(Short anioEscolar, String estadoMatricula);

    List<Matricula> findBySeccion_IdAndAnioEscolar(Integer seccionId, Short anioEscolar);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Matricula m where m.id = :matriculaId")
    java.util.Optional<Matricula> findByIdForUpdate(@Param("matriculaId") Integer matriculaId);
}