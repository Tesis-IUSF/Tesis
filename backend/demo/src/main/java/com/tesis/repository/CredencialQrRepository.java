package com.tesis.repository;

import com.tesis.entity.CredencialQr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CredencialQrRepository extends JpaRepository<CredencialQr, String> {

    List<CredencialQr> findByEmpleado_IdAndActivaTrue(Integer empleadoId);

    Optional<CredencialQr> findByIdAndEmpleado_IdAndActivaTrueAndExpiraEnAfter(
            String id, Integer empleadoId, LocalDateTime fechaHora);
}