package com.tesis.repository;

import com.tesis.entity.RetiroMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetiroMatriculaRepository extends JpaRepository<RetiroMatricula, Integer> {

    List<RetiroMatricula> findByMatricula_IdOrderByFechaRetiroDesc(Integer matriculaId);
}