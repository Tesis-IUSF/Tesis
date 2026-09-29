package com.tesis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "nombre_usuario", columnNames = "nombre_usuario"),
        @UniqueConstraint(name = "email", columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_usuario", length = 100, nullable = false)
    private String nombreUsuario;

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_rol"))
    private Roles rol;

    @Convert(converter = TipoUsuarioConverter.class)
    @Column(name = "tipo_usuario", length = 20, nullable = false, columnDefinition = "ENUM('personal','estudiante','representante','visitante') DEFAULT 'personal'")
        @Builder.Default
    private TipoUsuario tipoUsuario = TipoUsuario.PERSONAL;

    @Column(name = "activo", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
        @Builder.Default
    private Boolean activo = true;

    @Column(name = "email_verificado", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
        @Builder.Default
    private Boolean emailVerificado = false;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "creado_en", nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}