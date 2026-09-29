package com.tesis.repository;

import com.tesis.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    @Query("select a from Asistencia a join fetch a.personal e "
            + "left join fetch e.departamento left join fetch a.turno "
            + "where a.fecha between :desde and :hasta "
            + "and (:empleadoId is null or e.id = :empleadoId) "
            + "and (:departamentoId is null or e.departamento.id = :departamentoId) "
            + "and (:turnoId is null or a.turno.id = :turnoId) "
            + "and (:estado is null or lower(a.estado) = lower(:estado)) "
            + "order by a.fecha desc, a.horaEntrada desc, e.apellido asc")
    List<Asistencia> buscarHistorico(@Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta,
                                    @Param("empleadoId") Integer empleadoId,
                                    @Param("departamentoId") Integer departamentoId,
                                    @Param("turnoId") Integer turnoId,
                                    @Param("estado") String estado);

    @Query("select a from Asistencia a where a.fecha between :desde and :hasta")
    List<Asistencia> findAllByFechaBetween(@Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta);

    @Query("select a from Asistencia a join fetch a.personal "
            + "where a.fecha = :fecha order by a.horaEntrada asc, a.personal.apellido asc")
    List<Asistencia> findAllByFechaWithPersonalOrderByHoraEntrada(@Param("fecha") LocalDate fecha);

    Optional<Asistencia> findByPersonal_IdAndFecha(Integer empleadoId, LocalDate fecha);

    List<Asistencia> findByPersonal_IdAndFechaBetweenOrderByFechaDesc(
            Integer empleadoId, LocalDate desde, LocalDate hasta);
}