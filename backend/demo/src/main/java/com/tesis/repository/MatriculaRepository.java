package com.tesis.repository;

import com.tesis.entity.Matricula;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

    List<Matricula> findByEstudiante_IdAndAnioEscolar(Integer estudianteId, Short anioEscolar);

        boolean existsByEstudiante_IdAndAnioEscolarAndEstadoMatricula(
            Integer estudianteId, Short anioEscolar, String estadoMatricula);

        @Query("select case when count(m) > 0 then true else false end from Matricula m "
            + "where m.estudiante.id = :estudianteId and m.anioEscolar < :anioEscolar "
            + "and m.estadoMatricula <> 'anulada'")
        boolean tieneHistorialNoAnuladoAnteriorA(@Param("estudianteId") Integer estudianteId,
                             @Param("anioEscolar") Short anioEscolar);

        List<Matricula> findByEstudiante_IdAndEstadoMatriculaAndAnioEscolar(
            Integer estudianteId, String estadoMatricula, Short anioEscolar);

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    List<Matricula> findByEstadoMatricula(String estadoMatricula);

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    Page<Matricula> findByEstadoMatricula(String estadoMatricula, Pageable pageable);

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    List<Matricula> findByEstadoMatriculaOrderByFechaSolicitudAsc(String estadoMatricula);

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    Page<Matricula> findByEstadoMatriculaOrderByFechaSolicitudAsc(String estadoMatricula, Pageable pageable);

            java.util.Optional<Matricula> findByEstudiante_IdAndAnioEscolarAndSeccion_Id(
                    Integer estudianteId, Short anioEscolar, Integer seccionId);

    List<Matricula> findByAnioEscolarAndEstadoMatricula(Short anioEscolar, String estadoMatricula);

    List<Matricula> findBySeccion_IdAndAnioEscolar(Integer seccionId, Short anioEscolar);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Matricula m where m.id = :matriculaId")
    java.util.Optional<Matricula> findByIdForUpdate(@Param("matriculaId") Integer matriculaId);
}