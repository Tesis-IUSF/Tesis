import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import "./Dashboard.css";

const LISTA_VACIA = [];
const SERIES_ASISTENCIA = [
  { clave: "A tiempo", clase: "a-tiempo" },
  { clave: "Tardanzas", clase: "tardanzas" },
  { clave: "Salida anticipada", clase: "salida" },
  { clave: "Ausencias", clase: "ausencias" },
];

const fechaISO = (fecha) =>
  `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, "0")}-${String(fecha.getDate()).padStart(2, "0")}`;

function obtenerRangoSemana(fecha = new Date()) {
  const lunes = new Date(fecha);
  lunes.setDate(fecha.getDate() - ((fecha.getDay() + 6) % 7));
  return { desde: fechaISO(lunes), hasta: fechaISO(fecha) };
}

function listaValida(data) {
  return Array.isArray(data) ? data : null;
}

function crearDatosDiarios(desde, hasta, registros, ausencias) {
  const dias = [];
  const cursor = new Date(`${desde}T12:00:00`);
  const fin = new Date(`${hasta}T12:00:00`);

  while (cursor <= fin) {
    const fecha = fechaISO(cursor);
    const diario = registros.filter((registro) => registro.fecha === fecha);
    const ausentesRegistrados = new Set(
      diario
        .filter((registro) => registro.estado === "ausente")
        .map((registro) => registro.empleadoId),
    );
    const ausentesSinDuplicar = new Set([
      ...ausentesRegistrados,
      ...ausencias
        .filter((ausencia) => ausencia.fecha === fecha)
        .map((ausencia) => ausencia.empleadoId),
    ]);

    dias.push({
      fecha,
      dia: cursor.toLocaleDateString("es-VE", { weekday: "short" }),
      numero: cursor.toLocaleDateString("es-VE", { day: "numeric", month: "short" }),
      "A tiempo": diario.filter((r) => r.estado === "presente").length,
      Tardanzas: diario.filter((r) => r.estado === "tardanza").length,
      "Salida anticipada": diario.filter((r) => r.estado === "salida_anticipada").length,
      Ausencias: ausentesSinDuplicar.size,
    });
    cursor.setDate(cursor.getDate() + 1);
  }

  return dias;
}

function MetricCard({ icon, label, value, tone, helper }) {
  return (
    <article className={`dash-metric dash-metric--${tone}`}>
      <div className="dash-metric-top">
        <span className="dash-metric-icon" aria-hidden="true">
          <Icon icon={icon} />
        </span>
        <span className="dash-metric-label">{label}</span>
      </div>
      <strong className="dash-metric-value">{value ?? "—"}</strong>
      <span className="dash-metric-helper">{helper}</span>
    </article>
  );
}

function Dashboard() {
  const nombre = localStorage.getItem("nombre") || "";
  const { desde, hasta } = useMemo(() => obtenerRangoSemana(), []);
  const [cargando, setCargando] = useState(true);
  const [actualizado, setActualizado] = useState(null);
  const [errores, setErrores] = useState([]);
  const [personal, setPersonal] = useState(null);
  const [asistenciasHoy, setAsistenciasHoy] = useState(null);
  const [historico, setHistorico] = useState(null);
  const [ausencias, setAusencias] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    const resultados = await Promise.allSettled([
      api.get("/empleados", { params: { page: 0, size: 1, activo: true } }),
      api.get("/asistencias/hoy"),
      api.get("/asistencias/historico", { params: { desde, hasta } }),
      api.get("/asistencias/ausencias", { params: { desde, hasta } }),
    ]);
    const nombres = [
      "personal",
      "asistencia de hoy",
      "histórico semanal",
      "ausencias",
    ];
    const fallos = [];
    const datos = resultados.map((resultado, indice) => {
      if (resultado.status === "rejected") {
        fallos.push(
          `No se pudo cargar ${nombres[indice]}: ${extractErrorMessage(resultado.reason)}`,
        );
        return null;
      }
      return resultado.value.data;
    });

    if (datos[0] && !Number.isFinite(datos[0].totalElements)) {
      fallos.push("La respuesta de personal no incluye el total de registros.");
      datos[0] = null;
    }

    for (const indice of [1, 2, 3]) {
      if (datos[indice] !== null && !listaValida(datos[indice])) {
        fallos.push(`La respuesta de ${nombres[indice]} no tiene un formato válido.`);
        datos[indice] = null;
      }
    }

    setPersonal(datos[0]?.totalElements ?? null);
    setAsistenciasHoy(datos[1]);
    setHistorico(datos[2]);
    setAusencias(datos[3]);
    setErrores(fallos);
    setActualizado(new Date());
    setCargando(false);
  }, [desde, hasta]);

  useEffect(() => {
    const timer = window.setTimeout(cargar, 0);
    return () => window.clearTimeout(timer);
  }, [cargar]);

  const registrosHoy = asistenciasHoy ?? LISTA_VACIA;
  const registrosSemana = historico ?? LISTA_VACIA;
  const ausenciasSemana = ausencias ?? LISTA_VACIA;
  const fechaHoy = fechaISO(new Date());
  const ausenciasHoy = new Set([
    ...registrosSemana
      .filter((registro) => registro.fecha === fechaHoy && registro.estado === "ausente")
      .map((registro) => registro.empleadoId),
    ...ausenciasSemana
      .filter((ausencia) => ausencia.fecha === fechaHoy)
      .map((ausencia) => ausencia.empleadoId),
  ]).size;
  const conteoHoy = (estado) =>
    registrosHoy.filter((registro) => registro.estado === estado).length;
  const datosGrafico = useMemo(
    () =>
      crearDatosDiarios(
        desde,
        hasta,
        registrosSemana,
        ausenciasSemana,
      ),
    [desde, hasta, registrosSemana, ausenciasSemana],
  );
  const maximoDia = Math.max(
    1,
    ...datosGrafico.map((dia) =>
      SERIES_ASISTENCIA.reduce((total, serie) => total + dia[serie.clave], 0),
    ),
  );
  const fechaTexto = new Intl.DateTimeFormat("es-VE", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date());

  return (
    <main className="dash-main">
      <header className="dash-header">
        <div>
          <p className="dash-eyebrow">Resumen institucional</p>
          <h1>{nombre ? `Hola, ${nombre}` : "Panel principal"}</h1>
          <p className="dash-subtitle">
            Seguimiento de asistencia del personal del plantel.
          </p>
        </div>
        <div className="dash-header-actions">
          <p className="dash-date">
            <Icon icon={ICONOS.historico} aria-hidden="true" />
            <span>{fechaTexto}</span>
          </p>
          <button
            type="button"
            className="dash-refresh"
            onClick={cargar}
            disabled={cargando}
          >
            <Icon icon="mdi:refresh" className={cargando ? "is-spinning" : ""} />
            {cargando ? "Actualizando…" : "Actualizar"}
          </button>
        </div>
      </header>

      {errores.length > 0 && (
        <div className="dash-error" role="alert">
          <Icon icon="mdi:alert-circle-outline" aria-hidden="true" />
          <div>
            <strong>Algunos datos no están disponibles.</strong>
            <ul>
              {errores.map((error) => <li key={error}>{error}</li>)}
            </ul>
          </div>
        </div>
      )}

      <section className="dash-section" aria-labelledby="dash-hoy-titulo">
        <div className="dash-section-heading">
          <div>
            <p className="dash-eyebrow">Estado del día</p>
            <h2 id="dash-hoy-titulo">Asistencia de hoy</h2>
          </div>
          <Link to="/asistencia/hoy" className="dash-text-link">
            Ver registros <span aria-hidden="true">→</span>
          </Link>
        </div>

        <div className="dash-metrics">
          <MetricCard
            icon="mdi:account-group-outline"
            label="Personal registrado"
            value={personal}
            tone="blue"
            helper="Empleados activos"
          />
          <MetricCard
            icon="mdi:account-check-outline"
            label="A tiempo"
            value={asistenciasHoy === null ? null : conteoHoy("presente")}
            tone="green"
            helper="Registros de entrada"
          />
          <MetricCard
            icon="mdi:clock-alert-outline"
            label="Tardanzas"
            value={asistenciasHoy === null ? null : conteoHoy("tardanza")}
            tone="amber"
            helper="Entradas fuera de horario"
          />
          <MetricCard
            icon="mdi:exit-run"
            label="Salidas anticipadas"
            value={asistenciasHoy === null ? null : conteoHoy("salida_anticipada")}
            tone="violet"
            helper="Registros del día"
          />
          <MetricCard
            icon="mdi:account-alert-outline"
            label="Ausencias"
            value={historico === null || ausencias === null ? null : ausenciasHoy}
            tone="red"
            helper="Según asignaciones activas"
          />
        </div>
      </section>

      <section className="dash-chart-card" aria-labelledby="dash-semana-titulo">
        <div className="dash-section-heading">
          <div>
            <p className="dash-eyebrow">Tendencia</p>
            <h2 id="dash-semana-titulo">Asistencia durante la semana</h2>
          </div>
          <Link to="/asistencia/historico" className="dash-text-link">
            Consultar histórico <span aria-hidden="true">→</span>
          </Link>
        </div>

        {historico === null || ausencias === null ? (
          <div className="dash-chart-message">
            {cargando ? "Cargando tendencia semanal…" : "No se pudo cargar la tendencia semanal."}
          </div>
        ) : datosGrafico.every(
          (dia) =>
            dia["A tiempo"] === 0 &&
            dia.Tardanzas === 0 &&
            dia["Salida anticipada"] === 0 &&
            dia.Ausencias === 0,
        ) ? (
          <div className="dash-chart-message">
            No hay registros de asistencia para mostrar esta semana.
          </div>
        ) : (
          <div
            className="dash-chart"
            role="img"
            aria-label="Comparación diaria de asistencia, tardanzas, salidas anticipadas y ausencias"
          >
            <div className="dash-chart-legend" aria-hidden="true">
              {SERIES_ASISTENCIA.map((serie) => (
                <span key={serie.clave}>
                  <i className={`dash-legend-dot dash-legend-dot--${serie.clase}`} />
                  {serie.clave}
                </span>
              ))}
            </div>
            <div className="dash-chart-days">
              {datosGrafico.map((dia) => {
                const total = SERIES_ASISTENCIA.reduce(
                  (suma, serie) => suma + dia[serie.clave],
                  0,
                );
                const detalle = SERIES_ASISTENCIA
                  .map((serie) => `${serie.clave}: ${dia[serie.clave]}`)
                  .join(", ");
                return (
                  <div
                    className="dash-chart-row"
                    key={dia.fecha}
                    aria-label={`${dia.dia} ${dia.numero}: ${detalle}`}
                  >
                    <span className="dash-chart-day">
                      <strong>{dia.dia}</strong>
                      <small>{dia.numero}</small>
                    </span>
                    <div className="dash-chart-track" aria-hidden="true">
                      {SERIES_ASISTENCIA.map((serie) => {
                        const valor = dia[serie.clave];
                        return valor > 0 ? (
                          <span
                            key={serie.clave}
                            className={`dash-chart-segment dash-chart-segment--${serie.clase}`}
                            style={{ width: `${(valor / maximoDia) * 100}%` }}
                            title={`${serie.clave}: ${valor}`}
                          />
                        ) : null;
                      })}
                    </div>
                    <strong className="dash-chart-total">{total}</strong>
                  </div>
                );
              })}
            </div>
          </div>
        )}
        <p className="dash-updated">
          {actualizado
            ? `Datos actualizados a las ${actualizado.toLocaleTimeString("es-VE", { hour: "2-digit", minute: "2-digit" })}`
            : "Preparando datos…"}
        </p>
      </section>
    </main>
  );
}

export default Dashboard;
