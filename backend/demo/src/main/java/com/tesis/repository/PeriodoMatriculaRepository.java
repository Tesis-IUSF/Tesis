package com.tesis.repository;

import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.TipoPeriodoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodoMatriculaRepository extends JpaRepository<PeriodoMatricula, Integer> {

    List<PeriodoMatricula> findByAnioEscolarAndActivoTrueOrderByFechaInicioAsc(Short anioEscolar);

    List<PeriodoMatricula> findByAnioEscolarAndTipoAndActivoTrue(Short anioEscolar,
                                                                 TipoPeriodoMatricula tipo);
}