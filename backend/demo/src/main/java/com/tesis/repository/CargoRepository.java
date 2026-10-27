package com.tesis.repository;

import com.tesis.entity.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoRepository extends JpaRepository<Cargo, Integer> {

    boolean existsByDepartamento_Id(Integer departamentoId);

    boolean existsByNombreCargoIgnoreCaseAndDepartamento_Id(String nombreCargo, Integer departamentoId);

    boolean existsByNombreCargoIgnoreCaseAndDepartamento_IdAndIdNot(
            String nombreCargo, Integer departamentoId, Integer id);
}