package com.tesis.repository;

import com.tesis.entity.RelacionEstudianteRepresentante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelacionEstudianteRepresentanteRepository
        extends JpaRepository<RelacionEstudianteRepresentante, Integer> {

    List<RelacionEstudianteRepresentante> findByEstudiante_IdAndActivoTrue(Integer estudianteId);

        Optional<RelacionEstudianteRepresentante> findByEstudiante_IdAndRepresentante_Id(
            Integer estudianteId, Integer representanteId);

        Optional<RelacionEstudianteRepresentante>
            findByEstudiante_IdAndRepresentante_IdAndActivoTrueAndAutorizadoRetirarTrue(
                Integer estudianteId, Integer representanteId);
}