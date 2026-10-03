package com.tesis.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EstadisticasRepository extends Repository<com.tesis.entity.Matricula, Integer> {

    String ESTADOS_QUE_NO_OCUPAN_CUPO = "('cancelada', 'suspendida', 'retirada')";

    @Query("select coalesce(sum(case when s.capacidadMaxima - "
            + "(select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar and m.estadoMatricula not in "
            + ESTADOS_QUE_NO_OCUPAN_CUPO + ") > 0 then s.capacidadMaxima - "
            + "(select count(m) from Matricula m where m.seccion = s "
            + "and m.anioEscolar = s.anioEscolar and m.estadoMatricula not in "
            + ESTADOS_QUE_NO_OCUPAN_CUPO + ") else 0 end), 0) "
            + "from Seccion s where s.activo = true "
            + "and (:seccionId is null or s.id = :seccionId) "
            + "and (:nivel is null or lower(s.grado.nivelEducativo.nombre) = lower(:nivel))")
    Long obtenerCuposDisponibles(@Param("seccionId") Integer seccionId, @Param("nivel") String nivel);

    default Long obtenerCuposDisponibles() {
        return obtenerCuposDisponibles(null, null);
    }

    @Query("select coalesce(sum(s.capacidadMaxima), 0) from Seccion s "
            + "where s.activo = true and (:seccionId is null or s.id = :seccionId) "
            + "and (:nivel is null or lower(s.grado.nivelEducativo.nombre) = lower(:nivel))")
    Long obtenerCapacidadTotal(@Param("seccionId") Integer seccionId, @Param("nivel") String nivel);

    @Query("select count(m) from Matricula m where m.seccion.activo = true "
            + "and m.anioEscolar = m.seccion.anioEscolar and m.estadoMatricula not in "
            + ESTADOS_QUE_NO_OCUPAN_CUPO + " and (:seccionId is null or m.seccion.id = :seccionId) "
            + "and (:nivel is null or lower(m.seccion.grado.nivelEducativo.nombre) = lower(:nivel))")
    Long obtenerCuposOcupados(@Param("seccionId") Integer seccionId, @Param("nivel") String nivel);

    @Query("select m.estudiante.sexo, count(distinct m.estudiante.id) from Matricula m "
            + "where m.seccion.activo = true and m.anioEscolar = m.seccion.anioEscolar "
            + "and m.estadoMatricula not in " + ESTADOS_QUE_NO_OCUPAN_CUPO
            + " and (:seccionId is null or m.seccion.id = :seccionId) "
            + "and (:nivel is null or lower(m.seccion.grado.nivelEducativo.nombre) = lower(:nivel)) "
            + "group by m.estudiante.sexo")
    List<Object[]> obtenerEstudiantesPorSexo(@Param("seccionId") Integer seccionId,
                                              @Param("nivel") String nivel);

    default List<Object[]> obtenerEstudiantesPorSexo() {
        return obtenerEstudiantesPorSexo(null, null);
    }

        @Query("select n.nombre, n.ordinal, g.numeroGrado, g.nombreEspecial, s.letraSeccion, s.id, "
                        + "m.estudiante.sexo, count(distinct m.estudiante.id) "
                        + "from Matricula m join m.seccion s join s.grado g join g.nivelEducativo n "
                        + "where s.activo = true and m.anioEscolar = s.anioEscolar and m.estadoMatricula not in "
                        + ESTADOS_QUE_NO_OCUPAN_CUPO + " and (:seccionId is null or s.id = :seccionId) "
                        + "and (:nivel is null or lower(n.nombre) = lower(:nivel)) "
                        + "group by n.nombre, n.ordinal, g.numeroGrado, g.nombreEspecial, s.letraSeccion, s.id, "
                        + "m.estudiante.sexo order by n.ordinal, g.numeroGrado, s.letraSeccion, s.id, m.estudiante.sexo")
        List<Object[]> obtenerEstudiantesPorSeccionYSexo(@Param("seccionId") Integer seccionId,
                                                                                                          @Param("nivel") String nivel);

    @Query("select n.nombre, count(distinct m.estudiante.id) from Matricula m "
            + "join m.seccion.grado.nivelEducativo n where m.seccion.activo = true "
            + "and m.anioEscolar = m.seccion.anioEscolar and m.estadoMatricula not in "
            + ESTADOS_QUE_NO_OCUPAN_CUPO + " group by n.nombre order by n.nombre")
    List<Object[]> obtenerEstudiantesPorNivel();

    @Query("select count(m) from Matricula m where m.seccion.activo = true "
            + "and m.anioEscolar = m.seccion.anioEscolar and m.estadoMatricula not in "
            + ESTADOS_QUE_NO_OCUPAN_CUPO + " and m.tipoIngreso = 'nuevo_ingreso' "
            + "and (:seccionId is null or m.seccion.id = :seccionId) "
            + "and (:nivel is null or lower(m.seccion.grado.nivelEducativo.nombre) = lower(:nivel))")
    Long obtenerNuevoIngreso(@Param("seccionId") Integer seccionId, @Param("nivel") String nivel);

    @Query("select count(m) from Matricula m where m.seccion.activo = true "
            + "and m.anioEscolar = m.seccion.anioEscolar and m.tipoIngreso = 'regular' "
            + "and m.estadoMatricula in ('preinscrito', 'en_proceso') "
            + "and (:seccionId is null or m.seccion.id = :seccionId) "
            + "and (:nivel is null or lower(m.seccion.grado.nivelEducativo.nombre) = lower(:nivel))")
    Long obtenerRegularesPendientes(@Param("seccionId") Integer seccionId, @Param("nivel") String nivel);
}