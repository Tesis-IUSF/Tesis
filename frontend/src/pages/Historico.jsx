// src/pages/Historico.jsx
import { useState, useEffect, useCallback } from "react";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import "./Historico.css";
import Topbar from "../components/Topbar";

function hace30Dias() {
  const hoy = new Date();
  const hace30 = new Date(hoy);
  hace30.setDate(hoy.getDate() - 30);
  const fmt = (d) => d.toISOString().split("T")[0];
  return { desde: fmt(hace30), hasta: fmt(hoy) };
}

const BADGES = {
  presente: { clase: "badge-presente", etiqueta: "Presente" },
  tardanza: { clase: "badge-tardanza", etiqueta: "Tardanza" },
  salida_anticipada: {
    clase: "badge-salida-anticipada",
    etiqueta: "Salida anticipada",
  },
  ausente: { clase: "badge-ausente", etiqueta: "Ausente" },
  permiso: { clase: "badge-permiso", etiqueta: "Permiso" },
  feriado: { clase: "badge-feriado", etiqueta: "Feriado" },
  libre: { clase: "badge-libre", etiqueta: "Libre" },
};

function badgeEstado(estado, minutosTardanza) {
  const info = BADGES[estado?.toLowerCase()] || {
    clase: "",
    etiqueta: estado || "—",
  };
  const etiqueta =
    estado?.toLowerCase() === "tardanza" && minutosTardanza > 0
      ? `Tardanza (${minutosTardanza} min)`
      : info.etiqueta;
  return <span className={`badge ${info.clase}`}>{etiqueta}</span>;
}

function calcularHorasTrabajadas(horaEntrada, horaSalida) {
  if (!horaEntrada) return "—";
  if (!horaSalida) return "En curso";

  const [he, me] = horaEntrada.split(":").map(Number);
  const [hs, ms] = horaSalida.split(":").map(Number);
  let minutos = hs * 60 + ms - (he * 60 + me);
  if (minutos < 0) minutos += 24 * 60; // por si cruza medianoche

  const horas = Math.floor(minutos / 60);
  const mins = minutos % 60;
  return `${horas}:${String(mins).padStart(2, "0")}`;
}

function formatHora(hora) {
  return hora ? hora.substring(0, 5) : "—";
}

function Historico() {
  const [{ desde: desdeInicial, hasta: hastaInicial }] = useState(hace30Dias);
  const [desde, setDesde] = useState(desdeInicial);
  const [hasta, setHasta] = useState(hastaInicial);
  const [registros, setRegistros] = useState([]);
  const [inasistencias, setInasistencias] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const consultar = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      const [historicoRes, ausenciasRes] = await Promise.all([
        api.get("/asistencias/historico", { params: { desde, hasta } }),
        api.get("/asistencias/ausencias", { params: { desde, hasta } }),
      ]);

      const ordenado = [...historicoRes.data].sort((a, b) =>
        b.fecha.localeCompare(a.fecha),
      );
      setRegistros(ordenado);
      setInasistencias(ausenciasRes.data.length);
    } catch (err) {
      setError(extractErrorMessage(err, "No se pudo consultar el histórico."));
      setRegistros([]);
      setInasistencias(0);
    } finally {
      setCargando(false);
    }
  }, [desde, hasta]);

  useEffect(() => {
    consultar();
  }, [consultar]);

  const handleConsultar = (e) => {
    e.preventDefault();
    consultar();
  };

  const diasTrabajados = registros.filter((r) => r.horaEntrada).length;
  const tardanzas = registros.filter(
    (r) => r.estado?.toLowerCase() === "tardanza",
  ).length;
  const minutosTotales = registros.reduce((acc, r) => {
    if (!r.horaEntrada || !r.horaSalida) return acc;
    const [he, me] = r.horaEntrada.split(":").map(Number);
    const [hs, ms] = r.horaSalida.split(":").map(Number);
    let minutos = hs * 60 + ms - (he * 60 + me);
    if (minutos < 0) minutos += 24 * 60;
    return acc + minutos;
  }, 0);
  const horasTotales = `${Math.floor(minutosTotales / 60)}:${String(minutosTotales % 60).padStart(2, "0")}`;

  return (
    <>
      <Topbar mostrarVolver />
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Histórico de Asistencia</h1>
        </div>

        <form className="filters-panel" onSubmit={handleConsultar}>
          <div className="filters-grid">
            <label>
              Fecha desde
              <input
                type="date"
                value={desde}
                onChange={(e) => setDesde(e.target.value)}
                required
              />
            </label>
            <label>
              Fecha hasta
              <input
                type="date"
                value={hasta}
                onChange={(e) => setHasta(e.target.value)}
                required
              />
            </label>
            <button type="submit" className="btn-consultar" disabled={cargando}>
              {cargando ? "Consultando…" : "Consultar"}
            </button>
          </div>
        </form>

        {error && <p className="error-msg">{error}</p>}

        <div className="stats-row">
          <div className="stat-card stat-card--verde">
            <div className="stat-value">{diasTrabajados}</div>
            <div className="stat-label">Días trabajados</div>
          </div>
          <div className="stat-card stat-card--rojo">
            <div className="stat-value">{inasistencias}</div>
            <div className="stat-label">Inasistencias</div>
          </div>
          <div className="stat-card stat-card--naranja">
            <div className="stat-value">{tardanzas}</div>
            <div className="stat-label">Tardanzas</div>
          </div>
          <div className="stat-card stat-card--azul">
            <div className="stat-value">{horasTotales}</div>
            <div className="stat-label">Horas totales</div>
          </div>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Hora Entrada</th>
                <th>Hora Salida</th>
                <th>Horas Trabajadas</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={5} className="tabla-estado">
                    Cargando histórico...
                  </td>
                </tr>
              ) : registros.length === 0 ? (
                <tr>
                  <td colSpan={5} className="tabla-estado">
                    <span className="tabla-estado-icono">📋</span>
                    No hay registros en ese período.
                  </td>
                </tr>
              ) : (
                registros.map((r) => (
                  <tr key={r.id}>
                    <td data-label="Fecha">{r.fecha}</td>
                    <td data-label="Hora Entrada">
                      {formatHora(r.horaEntrada)}
                    </td>
                    <td data-label="Hora Salida">{formatHora(r.horaSalida)}</td>
                    <td data-label="Horas Trabajadas">
                      {calcularHorasTrabajadas(r.horaEntrada, r.horaSalida)}
                    </td>
                    <td data-label="Estado">
                      {badgeEstado(r.estado, r.minutosTardanza)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

export default Historico;
