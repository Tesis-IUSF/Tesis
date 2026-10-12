// src/pages/Historico.jsx
import { useState, useEffect, useCallback } from "react";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import "./Historico.css";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";

function rangoHoy() {
  const hoy = new Date();
  const fmt = (d) => d.toISOString().split("T")[0];
  return { desde: fmt(hoy), hasta: fmt(hoy) };
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
  const [{ desde: desdeInicial, hasta: hastaInicial }] = useState(rangoHoy);
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
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Histórico de Asistencia</h1>
        </div>

        <div className="filters-panel">
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

            {(desde !== desdeInicial || hasta !== hastaInicial) && (
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  const { desde: hoyDesde, hasta: hoyHasta } = rangoHoy();
                  setDesde(hoyDesde);
                  setHasta(hoyHasta);
                }}
                disabled={cargando}
              >
                Volver a hoy
              </button>
            )}
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="hist-stats-row">
          <div className="hist-stat-card hist-stat-card--verde">
            <div className="hist-stat-value">{diasTrabajados}</div>
            <div className="hist-stat-label">Días trabajados</div>
          </div>
          <div className="hist-stat-card hist-stat-card--rojo">
            <div className="hist-stat-value">{inasistencias}</div>
            <div className="hist-stat-label">Inasistencias</div>
          </div>
          <div className="hist-stat-card hist-stat-card--naranja">
            <div className="hist-stat-value">{tardanzas}</div>
            <div className="hist-stat-label">Tardanzas</div>
          </div>
          <div className="hist-stat-card hist-stat-card--azul">
            <div className="hist-stat-value">{horasTotales}</div>
            <div className="hist-stat-label">Horas totales</div>
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
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.lista} />
                    </span>
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
