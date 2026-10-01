import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import ConfirmDialog from "../components/ConfirmDialog";
import "./Empleados.css";

function Empleados() {
  const toast = useToast();
  const [empleados, setEmpleados] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [eliminando, setEliminando] = useState(false);
  const [empleadoAEliminar, setEmpleadoAEliminar] = useState(null);

  const cargarEmpleados = async () => {
    setCargando(true);
    setError("");
    try {
      const { data } = await api.get("/empleados");
      setEmpleados(data);
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudo cargar la lista de empleados."),
      );
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarEmpleados();
  }, []);

  const confirmarEliminar = async () => {
    if (!empleadoAEliminar) return;
    setEliminando(true);
    try {
      await api.delete(`/empleados/${empleadoAEliminar.id}`);
      setEmpleados((prev) => prev.filter((e) => e.id !== empleadoAEliminar.id));
      toast.success(
        `${empleadoAEliminar.nombre} ${empleadoAEliminar.apellido} fue eliminado correctamente.`,
      );
      setEmpleadoAEliminar(null);
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo eliminar al empleado."));
    } finally {
      setEliminando(false);
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
                      onClick={() => setEmpleadoAEliminar(emp)}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        open={!!empleadoAEliminar}
        title="Eliminar empleado"
        message={
          empleadoAEliminar
            ? `¿Eliminar a ${empleadoAEliminar.nombre} ${empleadoAEliminar.apellido}? Esta acción no se puede deshacer.`
            : ""
        }
        onConfirm={confirmarEliminar}
        onCancel={() => setEmpleadoAEliminar(null)}
        cargando={eliminando}
      />
    </div>
  );
}

export default Empleados;
