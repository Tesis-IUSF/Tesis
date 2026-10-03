// src/pages/Empleados.jsx
import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { useCargos, useTurnos } from "../hooks/useCatalogos";
import ConfirmDialog from "../components/ConfirmDialog";
import "./Empleados.css";
import Topbar from "../components/Topbar";

function Empleados() {
  const toast = useToast();
  const { items: cargos } = useCargos();
  const { items: turnos } = useTurnos();

  const [empleados, setEmpleados] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [eliminando, setEliminando] = useState(false);
  const [empleadoAEliminar, setEmpleadoAEliminar] = useState(null);
  const [filtroCargoId, setFiltroCargoId] = useState("");
  const [filtroTurnoId, setFiltroTurnoId] = useState("");

  const cargarEmpleados = useCallback(async (page = 0) => {
    setCargando(true);
    setError("");
    try {
      const { data } = await api.get("/empleados", {
        params: { page, size: 25 },
      });
      setEmpleados(data.content);
      setPagina({
        page: data.page,
        totalPages: data.totalPages,
        totalElements: data.totalElements,
      });
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudo cargar la lista de empleados."),
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargarEmpleados(0);
  }, [cargarEmpleados]);

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

  // Filtro en cliente, sobre la página ya cargada (según pide el criterio de SW-44)
  const empleadosFiltrados = empleados.filter((empleado) =>
    (!filtroCargoId || String(empleado.cargoId) === filtroCargoId) &&
    (!filtroTurnoId || String(empleado.turnoId) === filtroTurnoId),
  );

  const limpiarFiltros = () => {
    setFiltroCargoId("");
    setFiltroTurnoId("");
  };

  return (
    <>
      <Topbar mostrarVolver />
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Empleados</h1>
          <div className="mod-heading-actions">
            <Link to="/empleados/nuevo" className="btn-primary">
              + Crear Empleado
            </Link>
          </div>
        </div>

        <div className="filters-bar">
          <label>
            Cargo
            <select
              value={filtroCargoId}
              onChange={(e) => setFiltroCargoId(e.target.value)}
            >
              <option value="">Todos</option>
              {cargos.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombreCargo}
                </option>
              ))}
            </select>
          </label>

          <label>
            Turno
            <select
              value={filtroTurnoId}
              onChange={(e) => setFiltroTurnoId(e.target.value)}
            >
              <option value="">Todos</option>
              {turnos.map((turno) => (
                <option key={turno.id} value={turno.id}>
                  {turno.nombre}
                </option>
              ))}
            </select>
          </label>

          {(filtroCargoId || filtroTurnoId) && (
            <button
              type="button"
              className="btn-secondary"
              onClick={limpiarFiltros}
            >
              Limpiar Filtros
            </button>
          )}
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
              ) : empleadosFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={5} className="tabla-estado">
                    <span className="tabla-estado-icono">📋</span>
                      {filtroCargoId || filtroTurnoId
                      ? "No hay empleados con esos filtros en esta página."
                      : "No hay empleados registrados todavía."}
                  </td>
                </tr>
              ) : (
                empleadosFiltrados.map((emp) => (
                  <tr key={emp.id}>
                    <td data-label="Cédula">{emp.cedula}</td>
                    <td data-label="Nombre">
                      {emp.nombre} {emp.apellido}
                    </td>
                    <td data-label="Cargo">{emp.cargoNombre || "—"}</td>
                    <td data-label="Turno">{emp.turnoNombre || "—"}</td>
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

        {pagina.totalPages > 1 && (
          <div className="paginacion">
            <button
              disabled={pagina.page === 0 || cargando}
              onClick={() => cargarEmpleados(pagina.page - 1)}
            >
              ← Anterior
            </button>
            <span>
              Página {pagina.page + 1} de {pagina.totalPages} (
              {pagina.totalElements} empleados)
            </span>
            <button
              disabled={pagina.page + 1 >= pagina.totalPages || cargando}
              onClick={() => cargarEmpleados(pagina.page + 1)}
            >
              Siguiente →
            </button>
          </div>
        )}

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
    </>
  );
}

export default Empleados;
