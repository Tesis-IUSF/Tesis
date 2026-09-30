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
@Table(name = "checklist_matricula", uniqueConstraints = {
        @UniqueConstraint(name = "uq_check_matricula_requisito",
                columnNames = {"matricula_id", "requisito_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ChecklistMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_check_matricula"))
    private Matricula matricula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisito_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_check_requisito"))
    private RequisitoMatricula requisito;

    @Column(name = "cumplido", nullable = false)
    private Boolean cumplido = false;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verificado_por",
            foreignKey = @ForeignKey(name = "fk_check_verificado_por"))
    private Usuario verificadoPor;

    @Column(name = "notas", columnDefinition = "text")
    private String notas;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}