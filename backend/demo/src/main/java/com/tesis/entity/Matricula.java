package com.tesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "matriculas", uniqueConstraints = {
        @UniqueConstraint(name = "uq_matricula_estudiante_anio_seccion",
                columnNames = {"estudiante_id", "anio_escolar", "seccion_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Column(name = "anio_escolar", nullable = false)
    private Short anioEscolar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Column(name = "tipo_ingreso", length = 20, nullable = false)
    private String tipoIngreso = "regular";

    @Column(name = "institucion_procedencia", length = 150)
    private String institucionProcedencia;

    @Column(name = "estado_matricula", length = 20, nullable = false)
    private String estadoMatricula = "preinscrito";

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;

    @Column(name = "fecha_formalizacion")
    private LocalDate fechaFormalizacion;

    @Column(name = "numero_constancia", length = 50)
    private String numeroConstancia;

    @Column(name = "constancia_url", length = 500)
    private String constanciaUrl;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        creadoEn = now;
        actualizadoEn = now;
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}