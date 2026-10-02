package com.tesis.repository;

import com.tesis.entity.CategoriaComunicado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaComunicadoRepository extends JpaRepository<CategoriaComunicado, Integer> {

    Page<CategoriaComunicado> findByActivoTrue(Pageable pageable);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}