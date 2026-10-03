package com.tesis.repository;

import com.tesis.entity.AsignacionTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AsignacionTurnoRepository extends JpaRepository<AsignacionTurno, Integer> {

            @Query("select a from AsignacionTurno a join fetch a.personal p "
                    + "left join fetch p.departamento join fetch a.turno t "
                    + "where t.activo = true "
                    + "and (p.fechaIngreso is null or p.fechaIngreso <= :hasta) "
                    + "and (p.fechaEgreso is null or p.fechaEgreso >= :desde) "
                        + "and a.fechaDesde <= :hasta and (a.fechaHasta is null or a.fechaHasta >= :desde) "
                        + "order by p.id asc, a.fechaDesde desc, a.id desc")
        List<AsignacionTurno> buscarAsignacionesActivasEnRango(@Param("desde") LocalDate desde,
                                                                                                                   @Param("hasta") LocalDate hasta);

    @Query("select a from AsignacionTurno a join fetch a.turno "
            + "where a.personal.id = :empleadoId and a.fechaDesde <= :fecha "
            + "and (a.fechaHasta is null or a.fechaHasta >= :fecha) and a.turno.activo = true "
            + "order by a.fechaDesde desc")
    List<AsignacionTurno> buscarVigentes(@Param("empleadoId") Integer empleadoId,
                                         @Param("fecha") LocalDate fecha);

    @Query("select a from AsignacionTurno a join fetch a.turno t "
            + "where a.personal.id in :empleadoIds and a.fechaDesde <= :fecha "
            + "and (a.fechaHasta is null or a.fechaHasta >= :fecha) and t.activo = true "
            + "order by a.fechaDesde desc, a.id desc")
    List<AsignacionTurno> buscarVigentesPorEmpleados(@Param("empleadoIds") List<Integer> empleadoIds,
                                                      @Param("fecha") LocalDate fecha);

    @Query("select case when count(a) > 0 then true else false end from AsignacionTurno a "
            + "where a.personal.id = :empleadoId "
            + "and (:fechaHasta is null or a.fechaDesde <= :fechaHasta) "
            + "and (a.fechaHasta is null or a.fechaHasta >= :fechaDesde)")
    boolean existeSolapamiento(@Param("empleadoId") Integer empleadoId,
                               @Param("fechaDesde") LocalDate fechaDesde,
                               @Param("fechaHasta") LocalDate fechaHasta);

    @Query("select a from AsignacionTurno a join fetch a.personal join fetch a.turno "
            + "where a.personal.id = :empleadoId order by a.fechaDesde desc, a.id desc")
    List<AsignacionTurno> buscarPorEmpleado(@Param("empleadoId") Integer empleadoId);

    @Query("select a from AsignacionTurno a join fetch a.personal join fetch a.turno "
            + "where a.personal.id = :empleadoId and a.fechaDesde <= :fecha "
            + "and (a.fechaHasta is null or a.fechaHasta >= :fecha) "
            + "order by a.fechaDesde desc, a.id desc")
    List<AsignacionTurno> buscarVigentesEnFecha(@Param("empleadoId") Integer empleadoId,
                                                 @Param("fecha") LocalDate fecha);

    @Query("select case when count(a) > 0 then true else false end from AsignacionTurno a "
            + "where a.personal.id = :empleadoId and a.id <> :asignacionId "
            + "and (:fechaHasta is null or a.fechaDesde <= :fechaHasta) "
            + "and (a.fechaHasta is null or a.fechaHasta >= :fechaDesde)")
    boolean existeSolapamientoExcepto(@Param("empleadoId") Integer empleadoId,
                                      @Param("fechaDesde") LocalDate fechaDesde,
                                      @Param("fechaHasta") LocalDate fechaHasta,
                                      @Param("asignacionId") Integer asignacionId);
}