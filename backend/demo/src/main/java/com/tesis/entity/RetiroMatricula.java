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
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "retiros_matricula", indexes = {
        @Index(name = "idx_retiro_matricula", columnList = "matricula_id"),
        @Index(name = "idx_retiro_fecha", columnList = "fecha_retiro")
})
@Getter
@Setter
@NoArgsConstructor
public class RetiroMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_retiro_matricula"))
    private Matricula matricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representante_id",
            foreignKey = @ForeignKey(name = "fk_retiro_representante"))
    private Representante representante;

    @Column(name = "solicitante_nombre", length = 100, nullable = false)
    private String solicitanteNombre;

    @Column(name = "solicitante_apellido", length = 100, nullable = false)
    private String solicitanteApellido;

    @Column(name = "solicitante_cedula", length = 20, nullable = false)
    private String solicitanteCedula;

    @Column(name = "motivo", columnDefinition = "text", nullable = false)
    private String motivo;

    @Column(name = "fecha_retiro", nullable = false)
    private LocalDate fechaRetiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procesado_por",
            foreignKey = @ForeignKey(name = "fk_retiro_procesado_por"))
    private Usuario procesadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}