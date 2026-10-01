package com.tesis.repository;

import com.tesis.entity.NivelEducativo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NivelEducativoRepository extends JpaRepository<NivelEducativo, Integer> {

    Optional<NivelEducativo> findByNombreIgnoreCase(String nombre);
}
