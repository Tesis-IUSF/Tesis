package com.tesis.repository;

import com.tesis.entity.Roles;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RolesRepository extends JpaRepository<Roles, Integer> {

    Optional<Roles> findByNombreRol(String nombreRol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Roles r where r.nombreRol = :nombreRol")
    Optional<Roles> findByNombreRolForUpdate(@Param("nombreRol") String nombreRol);
}
