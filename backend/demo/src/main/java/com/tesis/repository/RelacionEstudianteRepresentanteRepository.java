package com.tesis.repository;

import com.tesis.entity.RelacionEstudianteRepresentante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelacionEstudianteRepresentanteRepository
        extends JpaRepository<RelacionEstudianteRepresentante, Integer> {

    List<RelacionEstudianteRepresentante> findByEstudiante_IdAndActivoTrue(Integer estudianteId);
}