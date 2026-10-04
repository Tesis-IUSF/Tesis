// src/pages/Carnets.jsx
import { useState, useEffect, useCallback } from "react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import Topbar from "../components/Topbar";
import { useCargos, useDepartamentos } from "../hooks/useCatalogos";
import "./Empleados.css"; // estilos compartidos (tabla, filtros, botones, paginación)
import "./Carnets.css";

const MAX_LOTE = 100; // mismo límite que CarnetLoteService

const ESTADOS = [
  { valor: "", etiqueta: "Todos" },
  { valor: "sin_carnet", etiqueta: "Sin carnet" },
  { valor: "vigente", etiqueta: "Vigente" },
  { valor: "por_vencer", etiqueta: "Por vencer" },
  { valor: "vencido", etiqueta: "Vencido" },
];

const ETIQUETA_ESTADO = Object.fromEntries(
  ESTADOS.filter((e) => e.valor).map((e) => [e.valor, e.etiqueta]),
);

const DIAS_AVISO = [15, 30, 60, 90];

function formatFecha(valor) {
  if (!valor) return "—";
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime())
    ? "—"
    : fecha.toLocaleDateString("es-VE");
}

function descargarArchivo(blob, nombre) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}

// Con responseType "blob" los errores también llegan como Blob: hay que leerlos.
async function mensajeError(err, porDefecto) {
  const data = err?.response?.data;
  if (data instanceof Blob) {
    try {
      const json = JSON.parse(await data.text());
      return json.message || json.error || porDefecto;
    } catch {
      return porDefecto;
    }
  }
  return extractErrorMessage(err, porDefecto);
}

function Carnets() {
  const toast = useToast();
  const { items: cargos } = useCargos();
  const { items: departamentos } = useDepartamentos();

  const [filas, setFilas] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const [q, setQ] = useState("");
  const [estadoQr, setEstadoQr] = useState("");
  const [filtroCargoId, setFiltroCargoId] = useState("");
  const [filtroDepartamentoId, setFiltroDepartamentoId] = useState("");
  const [diasAviso, setDiasAviso] = useState(30);

  // Selección: id -> { nombre, estadoQr }. Se conserva al cambiar de página.
  const [seleccion, setSeleccion] = useState(() => new Map());
  const [modalAbierto, setModalAbierto] = useState(false);
  const [formato, setFormato] = useState("pdf");
  const [entiendo, setEntiendo] = useState(false);
  const [generando, setGenerando] = useState(false);
  const [generandoId, setGenerandoId] = useState(null);

  const cargar = useCallback(
    async (page = 0) => {
      setCargando(true);
      setError("");
      try {
        const { data } = await api.get("/empleados/carnets", {
          params: {
            page,
            size: 25,
            q: q.trim() || undefined,
            estadoQr: estadoQr || undefined,
            cargoId: filtroCargoId || undefined,
            departamentoId: filtroDepartamentoId || undefined,
            diasAviso,
          },
        });
        setFilas(data.content);
        setPagina({
          page: data.page,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
        });
      } catch (err) {
        setError(
          extractErrorMessage(err, "No se pudo cargar la lista de carnets."),
        );
      } finally {
        setCargando(false);
      }
    },
    [q, estadoQr, filtroCargoId, filtroDepartamentoId, diasAviso],
  );

  // Recarga al cambiar filtros (con una pequeña espera al escribir en la búsqueda)
  useEffect(() => {
    const espera = setTimeout(() => cargar(0), 300);
    return () => clearTimeout(espera);
  }, [cargar]);

  const hayFiltros = q || estadoQr || filtroCargoId || filtroDepartamentoId;

  const limpiarFiltros = () => {
    setQ("");
    setEstadoQr("");
    setFiltroCargoId("");
    setFiltroDepartamentoId("");
  };

  // ---------- Selección ----------
  const infoSeleccion = (fila) => ({
    nombre: fila.nombreCompleto,
    estadoQr: fila.estadoQr,
  });

  const alternarUno = (fila) => {
    setSeleccion((prev) => {
      const siguiente = new Map(prev);
      if (siguiente.has(fila.empleadoId)) {
        siguiente.delete(fila.empleadoId);
      } else if (siguiente.size >= MAX_LOTE) {
        toast.error(`Máximo ${MAX_LOTE} empleados por lote.`);
        return prev;
      } else {
        siguiente.set(fila.empleadoId, infoSeleccion(fila));
      }
      return siguiente;
    });
  };

  const todosMarcados =
    filas.length > 0 && filas.every((f) => seleccion.has(f.empleadoId));

  const alternarPagina = () => {
    setSeleccion((prev) => {
      const siguiente = new Map(prev);
      if (todosMarcados) {
        filas.forEach((f) => siguiente.delete(f.empleadoId));
      } else {
        for (const f of filas) {
          if (siguiente.size >= MAX_LOTE) {
            toast.error(`Máximo ${MAX_LOTE} empleados por lote.`);
            break;
          }
          siguiente.set(f.empleadoId, infoSeleccion(f));
        }
      }
      return siguiente;
    });
  };

  const haySinCarnetEnPagina = filas.some((f) => f.estadoQr === "sin_carnet");

  const marcarSinCarnet = () => {
    setSeleccion((prev) => {
      const siguiente = new Map(prev);
      for (const f of filas) {
        if (f.estadoQr !== "sin_carnet") continue;
        if (siguiente.size >= MAX_LOTE) {
          toast.error(`Máximo ${MAX_LOTE} empleados por lote.`);
          break;
        }
        siguiente.set(f.empleadoId, infoSeleccion(f));
      }
      return siguiente;
    });
  };

  // ---------- Resumen para el modal ----------
  const aReemplazar = [...seleccion.entries()].filter(
    ([, info]) => info.estadoQr !== "sin_carnet",
  );
  const cantidadNuevos = seleccion.size - aReemplazar.length;

  const abrirModal = () => {
    setEntiendo(false);
    setModalAbierto(true);
  };

  // ---------- Generación ----------
  const generarLote = async () => {
    setGenerando(true);
    try {
      const { data } = await api.post(
        "/empleados/carnets/lote",
        { empleadoIds: [...seleccion.keys()], formato },
        { responseType: "blob" },
      );
      descargarArchivo(
        data,
        formato === "zip" ? "carnets-personal.zip" : "carnets-personal.pdf",
      );
      toast.success(`Se generaron ${seleccion.size} carnets.`);
      setSeleccion(new Map());
      setModalAbierto(false);
      cargar(pagina.page);
    } catch (err) {
      toast.error(
        await mensajeError(err, "No se pudieron generar los carnets."),
      );
    } finally {
      setGenerando(false);
    }
  };

  const generarIndividual = async (fila) => {
    if (
      fila.estadoQr !== "sin_carnet" &&
      !window.confirm(
        `${fila.nombreCompleto} ya tiene un carnet. Al generar uno nuevo, el anterior dejará de funcionar. ¿Continuar?`,
      )
    ) {
      return;
    }
    setGenerandoId(fila.empleadoId);
    try {
      const { data } = await api.post(
        `/empleados/${fila.empleadoId}/carnet`,
        null,
        { responseType: "blob" },
      );
      descargarArchivo(data, `carnet-empleado-${fila.empleadoId}.pdf`);
      toast.success("Carnet generado correctamente.");
      // Su estado cambió: se quita de la selección para no usar un dato viejo
      setSeleccion((prev) => {
        if (!prev.has(fila.empleadoId)) return prev;
        const siguiente = new Map(prev);
        siguiente.delete(fila.empleadoId);
        return siguiente;
      });
      cargar(pagina.page);
    } catch (err) {
      toast.error(await mensajeError(err, "No se pudo generar el carnet."));
    } finally {
      setGenerandoId(null);
    }
  };

  return (
    <>
      <Topbar mostrarVolver />
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Carnets</h1>
          <div className="mod-heading-actions">
            <button
              className="btn-primary"
              disabled={seleccion.size === 0}
              onClick={abrirModal}
            >
              Generar carnets ({seleccion.size})
            </button>
          </div>
        </div>

        <div className="filters-bar">
          <label>
            Buscar
            <input
              type="search"
              placeholder="Nombre o cédula"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
          </label>

          <label>
            Departamento
            <select
              value={filtroDepartamentoId}
              onChange={(e) => setFiltroDepartamentoId(e.target.value)}
            >
              <option value="">Todos</option>
              {departamentos.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.nombre}
                </option>
              ))}
            </select>
          </label>

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
            Estado
            <select
              value={estadoQr}
              onChange={(e) => setEstadoQr(e.target.value)}
            >
              {ESTADOS.map((e) => (
                <option key={e.valor} value={e.valor}>
                  {e.etiqueta}
                </option>
              ))}
            </select>
          </label>

          <label className="filtro-corto">
            Por vencer en
            <select
              value={diasAviso}
              onChange={(e) => setDiasAviso(Number(e.target.value))}
            >
              {DIAS_AVISO.map((d) => (
                <option key={d} value={d}>
                  {d} días
                </option>
              ))}
            </select>
          </label>

          {hayFiltros && (
            <button
              type="button"
              className="btn-secondary"
              onClick={limpiarFiltros}
            >
              Limpiar Filtros
            </button>
          )}
          {haySinCarnetEnPagina && (
            <button
              type="button"
              className="btn-secondary"
              onClick={marcarSinCarnet}
            >
              Marcar sin carnet
            </button>
          )}
          {seleccion.size > 0 && (
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setSeleccion(new Map())}
            >
              Limpiar selección
            </button>
          )}
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>
                  <input
                    type="checkbox"
                    checked={todosMarcados}
                    onChange={alternarPagina}
                    disabled={filas.length === 0}
                    aria-label="Seleccionar todos los de esta página"
                  />
                </th>
                <th>Nombre</th>
                <th>Cédula</th>
                <th>Cargo</th>
                <th>Departamento</th>
                <th>Estado</th>
                <th>Vence</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={8} className="tabla-estado">
                    Cargando carnets...
                  </td>
                </tr>
              ) : filas.length === 0 ? (
                <tr>
                  <td colSpan={8} className="tabla-estado">
                    <span className="tabla-estado-icono">🪪</span>
                    {hayFiltros
                      ? "No hay empleados con esos filtros."
                      : "No hay empleados activos todavía."}
                  </td>
                </tr>
              ) : (
                filas.map((f) => (
                  <tr key={f.empleadoId}>
                    <td data-label="Seleccionar">
                      <input
                        type="checkbox"
                        checked={seleccion.has(f.empleadoId)}
                        onChange={() => alternarUno(f)}
                        aria-label={`Seleccionar a ${f.nombreCompleto}`}
                      />
                    </td>
                    <td data-label="Nombre">{f.nombreCompleto}</td>
                    <td data-label="Cédula">{f.cedula}</td>
                    <td data-label="Cargo">{f.cargo ?? "—"}</td>
                    <td data-label="Departamento">{f.departamento ?? "—"}</td>
                    <td data-label="Estado">
                      <span className={`badge carnet-${f.estadoQr}`}>
                        {ETIQUETA_ESTADO[f.estadoQr] ?? f.estadoQr}
                      </span>
                    </td>
                    <td data-label="Vence">{formatFecha(f.expiraEn)}</td>
                    <td data-label="Acciones">
                      <button
                        className="btn-edit"
                        disabled={generandoId === f.empleadoId}
                        onClick={() => generarIndividual(f)}
                      >
                        {generandoId === f.empleadoId
                          ? "Generando..."
                          : f.estadoQr === "sin_carnet"
                            ? "Generar"
                            : "Regenerar"}
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
              onClick={() => cargar(pagina.page - 1)}
            >
              ← Anterior
            </button>
            <span>
              Página {pagina.page + 1} de {pagina.totalPages} (
              {pagina.totalElements} empleados)
            </span>
            <button
              disabled={pagina.page + 1 >= pagina.totalPages || cargando}
              onClick={() => cargar(pagina.page + 1)}
            >
              Siguiente →
            </button>
          </div>
        )}
      </div>

      {modalAbierto && (
        <div
          className="carnets-overlay"
          onClick={() => !generando && setModalAbierto(false)}
        >
          <div
            className="carnets-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="carnets-modal-titulo"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="carnets-modal-titulo">Generar carnets</h2>

            <ul className="carnets-resumen">
              <li>
                <strong>{cantidadNuevos}</strong>{" "}
                {cantidadNuevos === 1 ? "carnet nuevo" : "carnets nuevos"}
              </li>
              <li>
                <strong>{aReemplazar.length}</strong>{" "}
                {aReemplazar.length === 1
                  ? "carnet que reemplaza a uno existente"
                  : "carnets que reemplazan a uno existente"}
              </li>
            </ul>

            {aReemplazar.length > 0 && (
              <div className="carnets-alerta" role="alert">
                <p>
                  <strong>Atención:</strong> al reemplazar un carnet, el código
                  QR anterior se invalida. Si ya lo imprimiste o lo entregaste,
                  esa persona no podrá registrar asistencia con él.
                </p>
                <ul className="carnets-lista">
                  {aReemplazar.map(([id, info]) => (
                    <li key={id}>
                      {info.nombre}
                      <span className={`badge carnet-${info.estadoQr}`}>
                        {ETIQUETA_ESTADO[info.estadoQr] ?? info.estadoQr}
                      </span>
                    </li>
                  ))}
                </ul>
                <label className="carnets-confirmar">
                  <input
                    type="checkbox"
                    checked={entiendo}
                    onChange={(e) => setEntiendo(e.target.checked)}
                    disabled={generando}
                  />
                  Entiendo que los carnets anteriores dejarán de funcionar
                </label>
              </div>
            )}

            <fieldset className="carnets-formatos" disabled={generando}>
              <legend>Formato de descarga</legend>
              <label>
                <input
                  type="radio"
                  name="formato"
                  value="pdf"
                  checked={formato === "pdf"}
                  onChange={() => setFormato("pdf")}
                />
                <span>
                  <strong>Un solo PDF</strong>
                  <small>Todos los carnets juntos, listo para imprimir.</small>
                </span>
              </label>
              <label>
                <input
                  type="radio"
                  name="formato"
                  value="zip"
                  checked={formato === "zip"}
                  onChange={() => setFormato("zip")}
                />
                <span>
                  <strong>ZIP con un PDF por empleado</strong>
                  <small>Incluye un resultado.csv con el resumen.</small>
                </span>
              </label>
            </fieldset>

            <div className="carnets-modal-acciones">
              <button
                className="btn-secondary"
                disabled={generando}
                onClick={() => setModalAbierto(false)}
              >
                Cancelar
              </button>
              <button
                className="btn-primary"
                disabled={generando || (aReemplazar.length > 0 && !entiendo)}
                onClick={generarLote}
              >
                {generando ? "Generando..." : "Generar y descargar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default Carnets;
