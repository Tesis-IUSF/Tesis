package com.tesis.repository;

import com.tesis.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SeccionRepository extends JpaRepository<Seccion, Integer> {

    List<Seccion> findByGrado_IdAndAnioEscolar(Integer gradoId, Short anioEscolar);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seccion s where s.id = :seccionId")
    Optional<Seccion> findByIdForUpdate(@Param("seccionId") Integer seccionId);

    @Query("select s from Seccion s where "
            + "s.activo = true and (select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar "
            + "and m.estadoMatricula not in ('cancelada', 'suspendida')) < s.capacidadMaxima")
    List<Seccion> findByCuposOcupadosLessThanCapacidadMaxima();

        @Query("select case when s.capacidadMaxima - ("
            + "select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar "
            + "and m.estadoMatricula not in ('cancelada', 'suspendida')) > 0 "
            + "then s.capacidadMaxima - (select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar "
            + "and m.estadoMatricula not in ('cancelada', 'suspendida')) "
            + "else 0 end from Seccion s where s.id = :seccionId and s.activo = true")
        Optional<Long> calcularCuposDisponibles(@Param("seccionId") Integer seccionId);
}