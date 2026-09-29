package com.tesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "turnos")
@Getter
@Setter
@NoArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_salida", nullable = false)
    private LocalTime horaSalida;

    @Column(name = "tolerancia_min", nullable = false)
    private Short toleranciaMin = 0;

    @Column(name = "minutos_salida_anticipada_permitidos")
    private Short minutosSalidaAnticipadaPermitidos = 0;

    @Column(name = "requiere_justificacion_tardanza")
    private Boolean requiereJustificacionTardanza = true;

    @Column(name = "lunes", nullable = false)
    private Boolean lunes = true;

    @Column(name = "martes", nullable = false)
    private Boolean martes = true;

    @Column(name = "miercoles", nullable = false)
    private Boolean miercoles = true;

    @Column(name = "jueves", nullable = false)
    private Boolean jueves = true;

    @Column(name = "viernes", nullable = false)
    private Boolean viernes = true;

    @Column(name = "sabado", nullable = false)
    private Boolean sabado = false;

    @Column(name = "domingo", nullable = false)
    private Boolean domingo = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}