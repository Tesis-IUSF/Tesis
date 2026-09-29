package com.tesis.repository;

import com.tesis.entity.Representante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepresentanteRepository extends JpaRepository<Representante, Integer> {

    Optional<Representante> findByCedula(String cedula);

    boolean existsByCedula(String cedula);
}