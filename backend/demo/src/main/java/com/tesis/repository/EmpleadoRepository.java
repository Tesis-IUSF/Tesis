package com.tesis.repository;

import com.tesis.entity.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empleado e where e.id = :id")
    Optional<Empleado> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByCedula(String cedula);

    Optional<Empleado> findByCedula(String cedula);

    java.util.List<Empleado> findByActivoTrue(Sort sort);

    @EntityGraph(attributePaths = {"cargo", "departamento", "usuario"})
    @Query("select e from Empleado e where "
            + "(:cargoId is null or e.cargo.id = :cargoId) and "
            + "(:turnoId is null or exists (select a.id from AsignacionTurno a "
            + "where a.personal = e and a.turno.id = :turnoId and a.turno.activo = true "
            + "and a.fechaDesde <= :fecha and (a.fechaHasta is null or a.fechaHasta >= :fecha)))")
    Page<Empleado> buscarConFiltros(@Param("cargoId") Integer cargoId,
                                    @Param("turnoId") Integer turnoId,
                                    @Param("fecha") LocalDate fecha,
                                    Pageable pageable);

    boolean existsByCedulaAndIdNot(String cedula, Integer id);

    boolean existsByUsuario_Id(Integer usuarioId);

    boolean existsByUsuario_IdAndIdNot(Integer usuarioId, Integer id);

    boolean existsByCargo_Id(Integer cargoId);

    boolean existsByDepartamento_Id(Integer departamentoId);
}