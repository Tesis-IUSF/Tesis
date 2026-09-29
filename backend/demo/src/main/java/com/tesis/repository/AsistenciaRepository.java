package com.tesis.repository;

import com.tesis.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    Optional<Asistencia> findByPersonal_IdAndFecha(Integer empleadoId, LocalDate fecha);

    List<Asistencia> findByPersonal_IdAndFechaBetweenOrderByFechaDesc(
            Integer empleadoId, LocalDate desde, LocalDate hasta);
}