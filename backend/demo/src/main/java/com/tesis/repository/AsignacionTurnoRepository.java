package com.tesis.repository;

import com.tesis.entity.AsignacionTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AsignacionTurnoRepository extends JpaRepository<AsignacionTurno, Integer> {

    @Query("select a from AsignacionTurno a join fetch a.turno "
            + "where a.personal.id = :empleadoId and a.fechaDesde <= :fecha "
            + "and (a.fechaHasta is null or a.fechaHasta >= :fecha) and a.turno.activo = true "
            + "order by a.fechaDesde desc")
    List<AsignacionTurno> buscarVigentes(@Param("empleadoId") Integer empleadoId,
                                         @Param("fecha") LocalDate fecha);
}