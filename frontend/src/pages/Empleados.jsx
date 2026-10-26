// src/pages/Empleados.jsx
import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { useCargos, useTurnos } from "../hooks/useCatalogos";
import ConfirmDialog from "../components/ConfirmDialog";
import "./Empleados.css";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";

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
  const [filtroActivo, setFiltroActivo] = useState("");

  const cargarEmpleados = useCallback(
    async (page = 0) => {
      setCargando(true);
      setError("");
      try {
        const params = { page, size: 25 };
        if (filtroCargoId) params.cargoId = Number(filtroCargoId);
        if (filtroTurnoId) params.turnoId = Number(filtroTurnoId);
        if (filtroActivo === "true") params.activo = true;
        if (filtroActivo === "false") params.activo = false;

        const { data } = await api.get("/empleados", { params });
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
    },
    [filtroCargoId, filtroTurnoId, filtroActivo],
  );

  useEffect(() => {
    cargarEmpleados(0);
  }, [cargarEmpleados]);

  const confirmarEliminar = async () => {
    if (!empleadoAEliminar) return;
    setEliminando(true);
    try {
      await api.delete(`/empleados/${empleadoAEliminar.id}`);
      toast.success(
        `${empleadoAEliminar.nombre} ${empleadoAEliminar.apellido} fue dado de baja correctamente.`,
      );
      setEmpleadoAEliminar(null);
      await cargarEmpleados(pagina.page);
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo dar de baja al empleado."),
      );
    } finally {
      setEliminando(false);
    }
  };

  const limpiarFiltros = () => {
    setFiltroCargoId("");
    setFiltroTurnoId("");
    setFiltroActivo("");
  };

  return (
    <>
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

          <label>
            Estado
            <select
              value={filtroActivo}
              onChange={(e) => setFiltroActivo(e.target.value)}
            >
              <option value="">Todos</option>
              <option value="true">Activos</option>
              <option value="false">Dados de baja</option>
            </select>
          </label>

          {(filtroCargoId || filtroTurnoId || filtroActivo) && (
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
                <th>Estado</th>
                <th>Fecha Egreso</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={7} className="tabla-estado">
                    Cargando empleados...
                  </td>
                </tr>
              ) : empleados.length === 0 ? (
                <tr>
                  <td colSpan={7} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.lista} />
                    </span>
                    {filtroCargoId || filtroTurnoId || filtroActivo
                      ? "No hay empleados con esos filtros."
                      : "No hay empleados registrados todavía."}
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
                    <td data-label="Turno">{emp.turnoNombre || "—"}</td>
                    <td data-label="Estado">
                      <span
                        className={`badge-estado ${
                          emp.activo ? "badge-activo" : "badge-inactivo"
                        }`}
                      >
                        {emp.activo ? "Activo" : "Dado de baja"}
                      </span>
                    </td>
                    <td data-label="Fecha Egreso">{emp.fechaEgreso || "—"}</td>
                    <td data-label="Acciones">
                      <Link
                        to={`/empleados/${emp.id}/editar`}
                        className="btn-edit"
                      >
                        Editar
                      </Link>
                      {emp.activo ? (
                        <button
                          className="btn-delete"
                          onClick={() => setEmpleadoAEliminar(emp)}
                        >
                          Dar de baja
                        </button>
                      ) : (
                        <button
                          className="btn-delete"
                          disabled
                          title="El empleado ya está dado de baja"
                        >
                          Dado de baja
                        </button>
                      )}
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
          title="Dar de baja empleado"
          message={
            empleadoAEliminar
              ? `¿Dar de baja a ${empleadoAEliminar.nombre} ${empleadoAEliminar.apellido}? Esta acción cerrará su asignación vigente y revocará sus credenciales QR activas.`
              : ""
          }
          confirmText="Sí, dar de baja"
          confirmLoadingText="Dando de baja..."
          onConfirm={confirmarEliminar}
          onCancel={() => setEmpleadoAEliminar(null)}
          cargando={eliminando}
        />
      </div>
    </>
  );
}

export default Empleados;
