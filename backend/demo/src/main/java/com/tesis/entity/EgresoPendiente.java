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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "egresos_pendientes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_egreso_matricula_origen",
                columnNames = "matricula_origen_id")
})
@Getter
@Setter
@NoArgsConstructor
public class EgresoPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_origen_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_egreso_matricula_origen"))
    private Matricula matriculaOrigen;

    @Column(name = "anio_origen", nullable = false)
    private Short anioOrigen;

    @Column(name = "resolucion", length = 20)
    private String resolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por",
            foreignKey = @ForeignKey(name = "fk_egreso_resuelto_por"))
    private Usuario resueltoPor;

    @Column(name = "resuelto_en")
    private LocalDateTime resueltoEn;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_destino_id", unique = true,
            foreignKey = @ForeignKey(name = "fk_egreso_matricula_destino"))
    private Matricula matriculaDestino;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}
