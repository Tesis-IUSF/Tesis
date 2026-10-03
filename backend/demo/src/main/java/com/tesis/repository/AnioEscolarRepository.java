package com.tesis.repository;

import com.tesis.entity.AnioEscolar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnioEscolarRepository extends JpaRepository<AnioEscolar, Integer> {

    Optional<AnioEscolar> findFirstByActivoTrue();

    Optional<AnioEscolar> findByAnio(Short anio);

    boolean existsByAnio(Short anio);
}