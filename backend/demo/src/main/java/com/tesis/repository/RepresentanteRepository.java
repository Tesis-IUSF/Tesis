package com.tesis.repository;

import com.tesis.entity.Representante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RepresentanteRepository extends JpaRepository<Representante, Integer> {

    Optional<Representante> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    @Query("""
            select r from Representante r
            where r.activo = true
              and (
                lower(concat(r.nombre, ' ', r.apellido)) like lower(concat('%', :termino, '%'))
                or lower(r.cedula) like lower(concat('%', :cedula, '%'))
              )
            """)
    Page<Representante> buscarActivos(@Param("termino") String termino,
                                      @Param("cedula") String cedula,
                                      Pageable pageable);
}