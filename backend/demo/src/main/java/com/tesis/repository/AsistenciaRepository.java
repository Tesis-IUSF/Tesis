package com.tesis.repository;

import com.tesis.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    @Query("select a from Asistencia a join fetch a.personal "
            + "where a.fecha = :fecha order by a.horaEntrada asc, a.personal.apellido asc")
    List<Asistencia> findAllByFechaWithPersonalOrderByHoraEntrada(@Param("fecha") LocalDate fecha);

    Optional<Asistencia> findByPersonal_IdAndFecha(Integer empleadoId, LocalDate fecha);

    List<Asistencia> findByPersonal_IdAndFechaBetweenOrderByFechaDesc(
            Integer empleadoId, LocalDate desde, LocalDate hasta);
}