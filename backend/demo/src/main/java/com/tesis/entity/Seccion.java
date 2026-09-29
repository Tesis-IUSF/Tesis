package com.tesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "secciones", uniqueConstraints = {
        @UniqueConstraint(name = "uq_seccion_grado_letra_anio",
                columnNames = {"grado_id", "letra_seccion", "anio_escolar"})
})
@Getter
@Setter
@NoArgsConstructor
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "grado_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_seccion_grado"))
        private Grado grado;

    @Column(name = "letra_seccion", length = 1, nullable = false)
    private String letraSeccion;

    @Column(name = "capacidad_maxima", nullable = false)
    private Short capacidadMaxima = 40;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_principal_id")
    private Empleado docentePrincipal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @Column(name = "anio_escolar", nullable = false)
    private Short anioEscolar;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}