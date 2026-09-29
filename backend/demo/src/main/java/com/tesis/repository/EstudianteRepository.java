package com.tesis.repository;

import com.tesis.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

    Optional<Estudiante> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    List<Estudiante> findByActivoTrue();

    long countBySexo(String sexo);

    long countByActivoTrue();

    long countByActivoFalse();
}