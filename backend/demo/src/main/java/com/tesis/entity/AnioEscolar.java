package com.tesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "anios_escolares", uniqueConstraints = {
        @UniqueConstraint(name = "uq_anio", columnNames = "anio")
})
@Getter
@Setter
@NoArgsConstructor
public class AnioEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "anio", nullable = false)
    private Short anio;

    @Column(name = "nombre", length = 20, nullable = false)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activo", nullable = false)
    private Boolean activo = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}