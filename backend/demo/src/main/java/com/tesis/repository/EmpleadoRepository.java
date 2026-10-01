package com.tesis.repository;

import com.tesis.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empleado e where e.id = :id")
    Optional<Empleado> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByCedula(String cedula);

    Optional<Empleado> findByCedula(String cedula);

    java.util.List<Empleado> findByActivoTrue(Sort sort);

    boolean existsByCedulaAndIdNot(String cedula, Integer id);

    boolean existsByUsuario_Id(Integer usuarioId);

    boolean existsByUsuario_IdAndIdNot(Integer usuarioId, Integer id);
}