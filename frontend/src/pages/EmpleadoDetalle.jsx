import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import "./Empleados.css";

const formatearFecha = (fecha) => {
  if (!fecha) return "—";
  const [anio, mes, dia] = String(fecha).split("-");
  return `${dia}/${mes}/${anio}`;
};

function EmpleadoDetalle() {
  const { id } = useParams();
  const [empleado, setEmpleado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let vigente = true;
    api
      .get(`/empleados/${id}`)
      .then(({ data }) => {
        if (vigente) setEmpleado(data);
      })
      .catch((err) => {
        if (vigente) {
          setError(
            extractErrorMessage(err, "No se pudieron cargar los datos del empleado."),
          );
        }
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [id]);

  return (
    <main className="mod-main">
      <div className="mod-heading">
        <h1>Detalle del empleado</h1>
        <Link to="/empleados" className="btn-secondary">
          Volver a Empleados
        </Link>
      </div>

      {cargando ? (
        <p className="tabla-estado">Cargando datos del empleado...</p>
      ) : error ? (
        <p className="error-msg">{error}</p>
      ) : empleado ? (
        <section className="emp-detalle">
          <div className="emp-detalle-cabecera">
            {empleado.fotoUrl && (
              <img src={empleado.fotoUrl} alt="" className="emp-detalle-foto" />
            )}
            <div>
              <h2>
                {empleado.nombre} {empleado.apellido}
              </h2>
              <span
                className={`badge-estado ${
                  empleado.activo ? "badge-activo" : "badge-inactivo"
                }`}
              >
                {empleado.activo ? "Activo" : "Dado de baja"}
              </span>
            </div>
          </div>

          <dl className="emp-detalle-datos">
            <div><dt>Cédula</dt><dd>{empleado.cedula || "—"}</dd></div>
            <div><dt>Fecha de nacimiento</dt><dd>{formatearFecha(empleado.fechaNacimiento)}</dd></div>
            <div><dt>Sexo</dt><dd>{empleado.sexo || "—"}</dd></div>
            <div><dt>Correo</dt><dd>{empleado.correo || "—"}</dd></div>
            <div><dt>Teléfono</dt><dd>{empleado.telefono || "—"}</dd></div>
            <div><dt>Cargo</dt><dd>{empleado.cargoNombre || "—"}</dd></div>
            <div><dt>Departamento</dt><dd>{empleado.departamentoNombre || "—"}</dd></div>
            <div><dt>Turno</dt><dd>{empleado.turnoNombre || "—"}</dd></div>
            <div><dt>Fecha de ingreso</dt><dd>{formatearFecha(empleado.fechaIngreso)}</dd></div>
            <div><dt>Fecha de egreso</dt><dd>{formatearFecha(empleado.fechaEgreso)}</dd></div>
          </dl>
        </section>
      ) : null}
    </main>
  );
}

export default EmpleadoDetalle;
