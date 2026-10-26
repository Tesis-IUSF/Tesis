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

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    @Query("select m from Matricula m where m.id = :matriculaId")
    java.util.Optional<Matricula> buscarExpediente(@Param("matriculaId") Integer matriculaId);

    @EntityGraph(attributePaths = {"seccion.grado.nivelEducativo"})
    @Query("select m from Matricula m where m.estudiante.id = :estudianteId "
            + "order by m.anioEscolar desc, m.id desc")
    List<Matricula> buscarHistorialAcademico(@Param("estudianteId") Integer estudianteId);

    boolean existsByEstudiante_IdAndAnioEscolarAndEstadoMatriculaIn(
            Integer estudianteId, Short anioEscolar, List<String> estados);

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

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    @Query("select m from Matricula m where "
            + "(:estado is null or m.estadoMatricula = :estado) and "
            + "(:anioEscolar is null or m.anioEscolar = :anioEscolar) and "
            + "(:gradoId is null or m.seccion.grado.id = :gradoId) and "
            + "(:seccionId is null or m.seccion.id = :seccionId) and "
            + "(:busqueda is null "
            + "or lower(m.estudiante.cedula) like lower(concat('%', :busqueda, '%')) "
            + "or lower(m.estudiante.nombre) like lower(concat('%', :busqueda, '%')) "
            + "or lower(m.estudiante.apellido) like lower(concat('%', :busqueda, '%')) "
            + "or lower(concat(concat(m.estudiante.nombre, ' '), m.estudiante.apellido)) "
            + "like lower(concat('%', :busqueda, '%')))")
    Page<Matricula> buscarListado(@Param("estado") String estado,
                                  @Param("anioEscolar") Short anioEscolar,
                                  @Param("gradoId") Integer gradoId,
                                  @Param("seccionId") Integer seccionId,
                                  @Param("busqueda") String busqueda,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    @Query("select m from Matricula m where m.anioEscolar = :anioEscolar "
            + "and m.estadoMatricula in ('en_proceso', 'completada') "
            + "and m.seccion.grado.activo = true "
            + "and m.seccion.grado.numeroGrado = ("
            + "select max(g2.numeroGrado) from Grado g2 "
            + "where g2.nivelEducativo.id = m.seccion.grado.nivelEducativo.id "
            + "and g2.activo = true) "
            + "and (:gradoId is null or m.seccion.grado.id = :gradoId) "
            + "and (:seccionId is null or m.seccion.id = :seccionId) "
            + "and (:busqueda is null "
            + "or lower(m.estudiante.cedula) like lower(concat('%', :busqueda, '%')) "
            + "or lower(m.estudiante.nombre) like lower(concat('%', :busqueda, '%')) "
            + "or lower(m.estudiante.apellido) like lower(concat('%', :busqueda, '%')) "
            + "or lower(concat(concat(m.estudiante.nombre, ' '), m.estudiante.apellido)) "
            + "like lower(concat('%', :busqueda, '%')))")
    Page<Matricula> buscarPosiblesGraduados(@Param("anioEscolar") Short anioEscolar,
                                            @Param("gradoId") Integer gradoId,
                                            @Param("seccionId") Integer seccionId,
                                            @Param("busqueda") String busqueda,
                                            Pageable pageable);

            java.util.Optional<Matricula> findByEstudiante_IdAndAnioEscolarAndSeccion_Id(
                    Integer estudianteId, Short anioEscolar, Integer seccionId);

    List<Matricula> findByAnioEscolarAndEstadoMatricula(Short anioEscolar, String estadoMatricula);

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    @Query("select m from Matricula m where m.anioEscolar = :anioEscolar "
            + "and m.estadoMatricula in :estados "
            + "and (:nivelId is null or m.seccion.grado.nivelEducativo.id = :nivelId) "
            + "and (:gradoId is null or m.seccion.grado.id = :gradoId) "
            + "and (:seccionId is null or m.seccion.id = :seccionId) "
            + "order by m.estudiante.apellido, m.estudiante.nombre, m.estudiante.cedula")
    List<Matricula> listarParaNominaEstudiantil(
            @Param("anioEscolar") Short anioEscolar,
            @Param("estados") List<String> estados,
            @Param("nivelId") Integer nivelId,
            @Param("gradoId") Integer gradoId,
            @Param("seccionId") Integer seccionId);

    @EntityGraph(attributePaths = {"estudiante", "seccion.grado.nivelEducativo"})
    @Query("select m from Matricula m where m.anioEscolar = :anioEscolar "
            + "order by m.fechaSolicitud asc, m.id asc")
    List<Matricula> findAllForContinuityByAnio(@Param("anioEscolar") Short anioEscolar);

    boolean existsByEstudiante_IdAndAnioEscolar(Integer estudianteId, Short anioEscolar);

    boolean existsByAnioEscolar(Short anioEscolar);

    List<Matricula> findBySeccion_IdAndAnioEscolar(Integer seccionId, Short anioEscolar);

    boolean existsBySeccion_Id(Integer seccionId);

    boolean existsBySeccion_Grado_Id(Integer gradoId);

    boolean existsBySeccion_IdAndAnioEscolarAndEstadoMatriculaIn(
            Integer seccionId, Short anioEscolar, List<String> estados);

    boolean existsBySeccion_Grado_IdAndAnioEscolarAndEstadoMatriculaIn(
            Integer gradoId, Short anioEscolar, List<String> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Matricula m where m.id = :matriculaId")
    java.util.Optional<Matricula> findByIdForUpdate(@Param("matriculaId") Integer matriculaId);
}