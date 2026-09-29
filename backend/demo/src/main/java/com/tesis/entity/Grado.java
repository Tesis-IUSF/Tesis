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
@Table(name = "grados", uniqueConstraints = {
        @UniqueConstraint(name = "uq_grado_nivel_numero",
                columnNames = {"nivel_educativo_id", "numero_grado"})
})
@Getter
@Setter
@NoArgsConstructor
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "nivel_educativo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grado_nivel"))
        private NivelEducativo nivelEducativo;

    @Column(name = "numero_grado", nullable = false)
    private Short numeroGrado;

    @Column(name = "nombre_especial", length = 100)
    private String nombreEspecial;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}