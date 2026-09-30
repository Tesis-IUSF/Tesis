package com.tesis.repository;

import com.tesis.entity.RequisitoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequisitoMatriculaRepository extends JpaRepository<RequisitoMatricula, Integer> {

    List<RequisitoMatricula> findByNivelEducativo_IdAndObligatorioTrue(Integer nivelEducativoId);

    List<RequisitoMatricula> findByActivoTrue();

    @Query("select r from RequisitoMatricula r where r.activo = true "
            + "and (r.nivelEducativo is null or r.nivelEducativo.id = :nivelEducativoId) "
            + "order by r.obligatorio desc, r.nombre asc")
    List<RequisitoMatricula> findActivosAplicablesAlNivel(
            @Param("nivelEducativoId") Integer nivelEducativoId);

    @Query("select r from RequisitoMatricula r where r.activo = true "
            + "and r.obligatorio = true "
            + "and (r.nivelEducativo is null or r.nivelEducativo.id = :nivelEducativoId)")
    List<RequisitoMatricula> findObligatoriosActivosAplicablesAlNivel(
            @Param("nivelEducativoId") Integer nivelEducativoId);
}