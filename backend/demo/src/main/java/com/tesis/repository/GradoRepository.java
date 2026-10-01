package com.tesis.repository;

import com.tesis.entity.Grado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradoRepository extends JpaRepository<Grado, Integer> {

    List<Grado> findByNivelEducativo_Id(Integer nivelEducativoId);

    Page<Grado> findByNivelEducativo_Id(Integer nivelEducativoId, Pageable pageable);
}
