import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../utils/api";
import "./Empleados.css";

function Empleados() {
  const [empleados, setEmpleados] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [eliminandoId, setEliminandoId] = useState(null);

  const cargarEmpleados = async () => {
    setCargando(true);
    setError("");
    try {
      const { data } = await api.get("/empleados");
      setEmpleados(data);
    } catch (err) {
      setError("No se pudo cargar la lista de empleados. Intenta de nuevo.");
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarEmpleados();
  }, []);

  const handleEliminar = async (id, nombreCompleto) => {
    if (
      !window.confirm(
        `¿Eliminar a ${nombreCompleto}? Esta acción no se puede deshacer.`,
      )
    ) {
      return;
    }
    setEliminandoId(id);
    try {
      await api.delete(`/empleados/${id}`);
      setEmpleados((prev) => prev.filter((e) => e.id !== id));
    } catch (err) {
      alert("No se pudo eliminar al empleado. Intenta de nuevo.");
    } finally {
      setEliminandoId(null);
    }
  };

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>Empleados</h1>
        <div className="mod-heading-actions">
          <Link to="/empleados/nuevo" className="btn-primary">
            + Crear Empleado
          </Link>
        </div>
      </div>

      {error && <p className="error-msg">{error}</p>}

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Cédula</th>
              <th>Nombre</th>
              <th>Cargo</th>
              <th>Turno</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={5} className="tabla-estado">
                  Cargando empleados...
                </td>
              </tr>
            ) : empleados.length === 0 ? (
              <tr>
                <td colSpan={5} className="tabla-estado">
                  <span className="tabla-estado-icono">📋</span>
                  No hay empleados registrados todavía.
                </td>
              </tr>
            ) : (
              empleados.map((emp) => (
                <tr key={emp.id}>
                  <td data-label="Cédula">{emp.cedula}</td>
                  <td data-label="Nombre">
                    {emp.nombre} {emp.apellido}
                  </td>
                  <td data-label="Cargo">{emp.cargoNombre || "—"}</td>
                  {/* TODO: el backend aun no devuelve turno en EmpleadoResponseDTO */}
                  <td data-label="Turno">—</td>
                  <td data-label="Acciones">
                    <Link
                      to={`/empleados/${emp.id}/editar`}
                      className="btn-edit"
                    >
                      Editar
                    </Link>
                    <button
                      className="btn-delete"
                      onClick={() =>
                        handleEliminar(emp.id, `${emp.nombre} ${emp.apellido}`)
                      }
                      disabled={eliminandoId === emp.id}
                    >
                      {eliminandoId === emp.id ? "Eliminando..." : "Eliminar"}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default Empleados;
