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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "relacion_estudiante_representante", uniqueConstraints = {
        @UniqueConstraint(name = "uq_relacion_estudiante_representante",
                columnNames = {"estudiante_id", "representante_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class RelacionEstudianteRepresentante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "representante_id", nullable = false)
    private Representante representante;

    @Column(name = "relacion_principal", nullable = false)
    private Boolean relacionPrincipal = false;

    @Column(name = "autorizado_retirar", nullable = false)
    private Boolean autorizadoRetirar = true;

    @Column(name = "recibe_comunicados", nullable = false)
    private Boolean recibeComunicados = true;

    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDate fechaVinculacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}