package com.tesis.repository;

import com.tesis.entity.Seccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeccionRepository extends JpaRepository<Seccion, Integer> {

    @Override
    @EntityGraph(attributePaths = {"grado", "docentePrincipal", "turno"})
    Page<Seccion> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"grado", "docentePrincipal", "turno"})
    List<Seccion> findByGrado_Id(Integer gradoId);

    @EntityGraph(attributePaths = {"grado", "docentePrincipal", "turno"})
    Page<Seccion> findByGrado_Id(Integer gradoId, Pageable pageable);

    boolean existsByGrado_Id(Integer gradoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seccion s where s.grado.id = :gradoId order by s.id")
    List<Seccion> findByGrado_IdForUpdate(@Param("gradoId") Integer gradoId);

    List<Seccion> findByGrado_IdAndAnioEscolar(Integer gradoId, Short anioEscolar);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seccion s where s.anioEscolar = :anioEscolar "
            + "and s.activo = true order by s.id")
    List<Seccion> findActivasDelAnioForUpdate(@Param("anioEscolar") Short anioEscolar);

    @Query("select m.seccion.id, count(m) from Matricula m "
            + "where m.seccion.id in :seccionIds "
            + "and m.anioEscolar = m.seccion.anioEscolar "
            + "and m.estadoMatricula in ('preinscrito', 'en_proceso', 'completada') "
            + "group by m.seccion.id")
    List<Object[]> contarMatriculasActivasPorSecciones(@Param("seccionIds") List<Integer> seccionIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seccion s where s.id = :seccionId")
    Optional<Seccion> findByIdForUpdate(@Param("seccionId") Integer seccionId);

    @Query("select s from Seccion s where "
            + "s.activo = true and (select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar "
            + "and m.estadoMatricula in ('preinscrito', 'en_proceso', 'completada')) < s.capacidadMaxima")
    List<Seccion> findByCuposOcupadosLessThanCapacidadMaxima();

    @Query("select case when s.capacidadMaxima - ("
            + "select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar "
            + "and m.estadoMatricula in ('preinscrito', 'en_proceso', 'completada')) > 0 "
            + "then s.capacidadMaxima - (select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar "
            + "and m.estadoMatricula in ('preinscrito', 'en_proceso', 'completada')) "
            + "else 0 end from Seccion s where s.id = :seccionId and s.activo = true")
    Optional<Long> calcularCuposDisponibles(@Param("seccionId") Integer seccionId);
}