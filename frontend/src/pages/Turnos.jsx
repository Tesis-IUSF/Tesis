// src/pages/Turnos.jsx
import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import "./Turnos.css";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";

const DIAS = [
  { campo: "lunes", etiqueta: "L" },
  { campo: "martes", etiqueta: "M" },
  { campo: "miercoles", etiqueta: "X" },
  { campo: "jueves", etiqueta: "J" },
  { campo: "viernes", etiqueta: "V" },
  { campo: "sabado", etiqueta: "S" },
  { campo: "domingo", etiqueta: "D" },
];

function formatHora(hora) {
  return hora ? hora.substring(0, 5) : "—";
}

function diasActivos(turno) {
  return (
    DIAS.filter((d) => turno[d.campo])
      .map((d) => d.etiqueta)
      .join(" ") || "—"
  );
}

function Turnos() {
  const toast = useToast();
  const [turnos, setTurnos] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const cargarTurnos = useCallback(async (page = 0) => {
    setCargando(true);
    setError("");
    try {
      const { data } = await api.get("/catalogos/turnos", {
        params: { page, size: 25 },
      });
      setTurnos(data.content);
      setPagina({
        page: data.page,
        totalPages: data.totalPages,
        totalElements: data.totalElements,
      });
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudo cargar la lista de turnos."),
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargarTurnos(0);
  }, [cargarTurnos]);

  const alternarActivo = async (turno) => {
    try {
      const payload = { ...turno, activo: !turno.activo };
      delete payload.id;
      await api.put(`/catalogos/turnos/${turno.id}`, payload);
      setTurnos((prev) =>
        prev.map((t) => (t.id === turno.id ? { ...t, activo: !t.activo } : t)),
      );
      toast.success(
        `Turno ${!turno.activo ? "activado" : "desactivado"} correctamente.`,
      );
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo actualizar el turno."));
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Turnos</h1>
          <div className="mod-heading-actions">
            <Link to="/turnos/nuevo" className="btn-primary">
              + Crear Turno
            </Link>
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Entrada</th>
                <th>Salida</th>
                <th>Días</th>
                <th>Tolerancia</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={7} className="tabla-estado">
                    Cargando turnos...
                  </td>
                </tr>
              ) : turnos.length === 0 ? (
                <tr>
                  <td colSpan={7} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.reloj} />
                    </span>
                    No hay turnos registrados todavía.
                  </td>
                </tr>
              ) : (
                turnos.map((t) => (
                  <tr key={t.id}>
                    <td data-label="Nombre">{t.nombre}</td>
                    <td data-label="Entrada">{formatHora(t.horaEntrada)}</td>
                    <td data-label="Salida">{formatHora(t.horaSalida)}</td>
                    <td data-label="Días">{diasActivos(t)}</td>
                    <td data-label="Tolerancia">{t.toleranciaMin ?? 0} min</td>
                    <td data-label="Estado">
                      <span
                        className={`badge ${t.activo ? "badge-presente" : "badge-ausente"}`}
                      >
                        {t.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td data-label="Acciones">
                      <Link to={`/turnos/${t.id}/editar`} className="btn-edit">
                        Editar
                      </Link>
                      <button
                        className={t.activo ? "btn-delete" : "btn-reactivar"}
                        onClick={() => alternarActivo(t)}
                      >
                        {t.activo ? "Desactivar" : "Activar"}
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
              onClick={() => cargarTurnos(pagina.page - 1)}
            >
              ← Anterior
            </button>
            <span>
              Página {pagina.page + 1} de {pagina.totalPages} (
              {pagina.totalElements} turnos)
            </span>
            <button
              disabled={pagina.page + 1 >= pagina.totalPages || cargando}
              onClick={() => cargarTurnos(pagina.page + 1)}
            >
              Siguiente →
            </button>
          </div>
        )}
      </div>
    </>
  );
}

export default Turnos;
