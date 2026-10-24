package com.tesis.repository;

import com.tesis.entity.AnioEscolar;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnioEscolarRepository extends JpaRepository<AnioEscolar, Integer> {

    Optional<AnioEscolar> findFirstByActivoTrue();

    Optional<AnioEscolar> findByAnio(Short anio);

    boolean existsByAnio(Short anio);

    List<AnioEscolar> findAllByOrderByAnioDesc();

    Optional<AnioEscolar> findTopByAnioLessThanOrderByAnioDesc(Short anio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AnioEscolar a order by a.id")
    List<AnioEscolar> findAllForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AnioEscolar a where a.anio = :anio")
    Optional<AnioEscolar> findByAnioForUpdate(@Param("anio") Short anio);
}