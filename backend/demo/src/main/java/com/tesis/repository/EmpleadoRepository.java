package com.tesis.repository;

import com.tesis.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    boolean existsByCedula(String cedula);

    boolean existsByCedulaAndIdNot(String cedula, Integer id);

    boolean existsByUsuario_Id(Integer usuarioId);

    boolean existsByUsuario_IdAndIdNot(Integer usuarioId, Integer id);
}