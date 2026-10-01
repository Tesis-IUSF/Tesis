package com.tesis.repository;

import com.tesis.entity.Estudiante;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

    Optional<Estudiante> findByCedula(String cedula);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estudiante e where e.cedula = :cedula")
    Optional<Estudiante> findByCedulaForUpdate(@Param("cedula") String cedula);

    boolean existsByCedula(String cedula);

    boolean existsByUsuario_Id(Integer usuarioId);

    List<Estudiante> findByActivoTrue();

    long countBySexo(String sexo);

    long countByActivoTrue();

    long countByActivoFalse();
}