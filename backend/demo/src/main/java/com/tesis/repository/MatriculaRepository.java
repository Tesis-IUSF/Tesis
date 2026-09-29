package com.tesis.repository;

import com.tesis.entity.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

    List<Matricula> findByEstudiante_IdAndAnioEscolar(Integer estudianteId, Short anioEscolar);

    List<Matricula> findByAnioEscolarAndEstadoMatricula(Short anioEscolar, String estadoMatricula);

    List<Matricula> findBySeccion_IdAndAnioEscolar(Integer seccionId, Short anioEscolar);
}