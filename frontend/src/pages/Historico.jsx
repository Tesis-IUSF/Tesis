// src/pages/Historico.jsx
import { useState, useEffect, useCallback } from "react";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import { descargarArchivo, mensajeErrorBlob } from "../utils/descargas";
import { useCargos, useDepartamentos, useTurnos } from "../hooks/useCatalogos";
import "./Historico.css";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";

function fechaLocal(fecha) {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, "0");
  const dia = String(fecha.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
}

function errorRangoFechas(desde, hasta, hoy) {
  if (!desde || !hasta) return "Selecciona ambas fechas para consultar.";
  if (desde > hasta) return "La fecha desde no puede ser posterior a la fecha hasta.";
  if (hasta > hoy) return "La fecha hasta no puede ser posterior a hoy.";
  const dias = (Date.parse(`${hasta}T00:00:00Z`) - Date.parse(`${desde}T00:00:00Z`))
    / (24 * 60 * 60 * 1000);
  if (dias > 366) return "El rango máximo permitido es de 366 días.";
  return "";
}

function rangoHoy() {
  const hoy = new Date();
  const fecha = fechaLocal(hoy);
  return { desde: fecha, hasta: fecha };
}

function rangoSemana() {
  const hoy = new Date();
  const lunes = new Date(hoy);
  const diasDesdeLunes = (hoy.getDay() + 6) % 7;
  lunes.setDate(hoy.getDate() - diasDesdeLunes);
  return { desde: fechaLocal(lunes), hasta: fechaLocal(hoy) };
}

function rangoMes() {
  const hoy = new Date();
  return {
    desde: fechaLocal(new Date(hoy.getFullYear(), hoy.getMonth(), 1)),
    hasta: fechaLocal(hoy),
  };
}

function rangoUltimos30Dias() {
  const hoy = new Date();
  const desde = new Date(hoy);
  desde.setDate(hoy.getDate() - 29);
  return { desde: fechaLocal(desde), hasta: fechaLocal(hoy) };
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
  const { items: cargos, error: errorCargos } = useCargos();
  const { items: departamentos, error: errorDepartamentos } = useDepartamentos();
  const { items: turnos, error: errorTurnos } = useTurnos();
  const [desde, setDesde] = useState(desdeInicial);
  const [hasta, setHasta] = useState(hastaInicial);
  const [empleados, setEmpleados] = useState([]);
  const [empleadoId, setEmpleadoId] = useState("");
  const [departamentoId, setDepartamentoId] = useState("");
  const [cargoId, setCargoId] = useState("");
  const [turnoId, setTurnoId] = useState("");
  const [estado, setEstado] = useState("");
  const [registros, setRegistros] = useState([]);
  const [inasistencias, setInasistencias] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [descargando, setDescargando] = useState(false);
  const [error, setError] = useState("");
  const [errorEmpleados, setErrorEmpleados] = useState("");
  const hoy = fechaLocal(new Date());
  const validacionRango = errorRangoFechas(desde, hasta, hoy);

  useEffect(() => {
    let vigente = true;
    const cargarEmpleados = async () => {
      try {
        const personal = [];
        let page = 0;
        let totalPages = 1;
        while (page < totalPages) {
          const { data } = await api.get("/empleados", {
            params: { page, size: 100 },
          });
          personal.push(...data.content);
          totalPages = data.totalPages;
          page += 1;
        }
        if (vigente) setEmpleados(personal);
      } catch {
        if (vigente) {
          setErrorEmpleados("No se pudo cargar la lista de empleados para filtrar.");
        }
      }
    };
    cargarEmpleados();
    return () => {
      vigente = false;
    };
  }, []);

  const parametrosFiltro = useCallback(() => ({
    desde,
    hasta,
    ...(empleadoId ? { empleadoId: Number(empleadoId) } : {}),
    ...(departamentoId ? { departamentoId: Number(departamentoId) } : {}),
    ...(cargoId ? { cargoId: Number(cargoId) } : {}),
    ...(turnoId ? { turnoId: Number(turnoId) } : {}),
    ...(estado ? { estado } : {}),
  }), [desde, hasta, empleadoId, departamentoId, cargoId, turnoId, estado]);

  const consultar = useCallback(async () => {
    const errorFecha = errorRangoFechas(desde, hasta, fechaLocal(new Date()));
    if (errorFecha) {
      setError(errorFecha);
      setRegistros([]);
      setInasistencias(0);
      setCargando(false);
      return;
    }
    setCargando(true);
    setError("");
    try {
      const params = parametrosFiltro();
      const { estado: estadoFiltro, ...filtrosAusencias } = params;
      const [historicoRes, ausenciasRes] = await Promise.all([
        api.get("/asistencias/historico", { params }),
        api.get("/asistencias/ausencias", { params: filtrosAusencias }),
      ]);

      const datosAusencias = !estadoFiltro || estadoFiltro === "ausente"
        ? ausenciasRes.data
        : [];
      const clavesConRegistro = new Set(
        historicoRes.data.map((registro) => `${registro.empleadoId}:${registro.fecha}`),
      );
      const ausenciasSinRegistro = datosAusencias
        .filter((ausencia) => !clavesConRegistro.has(`${ausencia.empleadoId}:${ausencia.fecha}`))
        .map((ausencia) => ({
          id: `ausente-${ausencia.empleadoId}-${ausencia.fecha}`,
          empleadoId: ausencia.empleadoId,
          empleadoNombre: `${ausencia.nombre} ${ausencia.apellido}`.trim(),
          empleadoCedula: ausencia.cedula,
          fecha: ausencia.fecha,
          turnoNombre: ausencia.turnoNombre,
          horaEntrada: null,
          horaSalida: null,
          estado: "ausente",
          minutosTardanza: null,
          minutosExtra: null,
          tipoRegistro: "sin_marcaje",
        }));
      const ordenado = [...historicoRes.data, ...ausenciasSinRegistro].sort((a, b) =>
        b.fecha.localeCompare(a.fecha)
        || a.empleadoNombre.localeCompare(b.empleadoNombre, "es"),
      );
      setRegistros(ordenado);
      setInasistencias(
        !estadoFiltro || estadoFiltro === "ausente"
          ? datosAusencias.length
          : 0,
      );
    } catch (err) {
      setError(extractErrorMessage(err, "No se pudo consultar el histórico."));
      setRegistros([]);
      setInasistencias(0);
    } finally {
      setCargando(false);
    }
  }, [parametrosFiltro, desde, hasta]);

  useEffect(() => {
    const timer = window.setTimeout(consultar, 0);
    return () => window.clearTimeout(timer);
  }, [consultar]);

  const descargarReporte = async () => {
    setDescargando(true);
    try {
      const { data } = await api.get("/asistencias/historico/pdf", {
        params: parametrosFiltro(),
        responseType: "blob",
      });
      descargarArchivo(data, "reporte-asistencia.pdf");
    } catch (err) {
      setError(await mensajeErrorBlob(err, "No se pudo descargar el reporte de asistencia."));
    } finally {
      setDescargando(false);
    }
  };

  const establecerRango = (rango) => {
    setDesde(rango.desde);
    setHasta(rango.hasta);
  };

  const limpiarFiltros = () => {
    const hoy = rangoHoy();
    setDesde(hoy.desde);
    setHasta(hoy.hasta);
    setEmpleadoId("");
    setDepartamentoId("");
    setCargoId("");
    setTurnoId("");
    setEstado("");
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
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Histórico de Asistencia</h1>
          <div className="hist-heading-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={descargarReporte}
              disabled={descargando || cargando || Boolean(validacionRango)}
            >
              <Icon icon={ICONOS.descargar} className="icono-inline" />{" "}
              {descargando ? "Generando reporte..." : "Descargar reporte"}
            </button>
          </div>
        </div>

        <div className="filters-panel">
          <div className="filters-grid">
            <div className="hist-periodos" aria-label="Períodos rápidos">
              <span>Período rápido</span>
              <div>
                <button type="button" className="btn-secondary" onClick={() => establecerRango(rangoHoy())}>
                  Hoy
                </button>
                <button type="button" className="btn-secondary" onClick={() => establecerRango(rangoSemana())}>
                  Esta semana
                </button>
                <button type="button" className="btn-secondary" onClick={() => establecerRango(rangoMes())}>
                  Este mes
                </button>
                <button type="button" className="btn-secondary" onClick={() => establecerRango(rangoUltimos30Dias())}>
                  Últimos 30 días
                </button>
              </div>
            </div>
            <label>
              Fecha desde
              <input
                type="date"
                value={desde}
                max={hoy}
                onChange={(e) => setDesde(e.target.value)}
                required
              />
            </label>
            <label>
              Fecha hasta
              <input
                type="date"
                value={hasta}
                min={desde || undefined}
                max={hoy}
                onChange={(e) => setHasta(e.target.value)}
                required
              />
            </label>
            <label>
              Empleado
              <select value={empleadoId} onChange={(e) => setEmpleadoId(e.target.value)}>
                <option value="">Todos</option>
                {empleados.map((empleado) => (
                  <option key={empleado.id} value={empleado.id}>
                    {empleado.apellido}, {empleado.nombre} · {empleado.cedula}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Departamento
              <select value={departamentoId} onChange={(e) => setDepartamentoId(e.target.value)}>
                <option value="">Todos</option>
                {departamentos.map((departamento) => (
                  <option key={departamento.id} value={departamento.id}>
                    {departamento.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Cargo
              <select value={cargoId} onChange={(e) => setCargoId(e.target.value)}>
                <option value="">Todos</option>
                {cargos.map((cargo) => (
                  <option key={cargo.id} value={cargo.id}>
                    {cargo.nombreCargo}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Turno
              <select value={turnoId} onChange={(e) => setTurnoId(e.target.value)}>
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
              <select value={estado} onChange={(e) => setEstado(e.target.value)}>
                <option value="">Todos</option>
                {Object.entries(BADGES).map(([valor, detalle]) => (
                  <option key={valor} value={valor}>{detalle.etiqueta}</option>
                ))}
              </select>
            </label>

            {(desde !== desdeInicial || hasta !== hastaInicial || empleadoId
              || departamentoId || cargoId || turnoId || estado) && (
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={limpiarFiltros}
                  disabled={cargando}
                >
                  Limpiar filtros
                </button>
              )}
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}
        {(errorEmpleados || errorCargos || errorDepartamentos || errorTurnos) && (
          <p className="error-msg">
            {errorEmpleados || errorCargos || errorDepartamentos || errorTurnos}
          </p>
        )}

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
                  <th>Empleado</th>
                  <th>Cédula</th>
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
                  <td colSpan={7} className="tabla-estado">
                    Cargando histórico...
                  </td>
                </tr>
              ) : registros.length === 0 ? (
                <tr>
                  <td colSpan={7} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.lista} />
                    </span>
                    No hay registros en ese período.
                  </td>
                </tr>
              ) : (
                registros.map((r) => (
                  <tr key={r.id}>
                    <td data-label="Empleado">{r.empleadoNombre || "—"}</td>
                    <td data-label="Cédula">{r.empleadoCedula || "—"}</td>
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
