// src/pages/Matricula.jsx
import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import { descargarArchivo, mensajeErrorBlob } from "../utils/descargas";
import "./Matricula.css";

const TABS = [
  {
    clave: "preinscritos",
    etiqueta: "Preinscritos",
    estado: "preinscrito",
    vacio:
      "No hay preinscripciones pendientes. Registra una con «Nueva preinscripción».",
  },
  {
    clave: "proceso",
    etiqueta: "En proceso",
    estado: "en_proceso",
    vacio: "No hay inscripciones en proceso.",
  },
  {
    clave: "formalizadas",
    etiqueta: "Formalizadas",
    estado: "completada",
    vacio: "Todavía no hay inscripciones formalizadas.",
  },
  {
    clave: "retiradas",
    etiqueta: "Retiradas",
    estado: "retirada",
    vacio: "No hay matrículas retiradas.",
  },
];

const ETIQUETA_ESTADO = {
  preinscrito: "Preinscrito",
  en_proceso: "En proceso",
  completada: "Formalizada",
  retirada: "Retirada",
};

// "2026-10-22" -> "22/10/2026" sin pasar por Date (evita desfase de zona horaria)
const formatFechaISO = (v) => {
  if (!v) return "—";
  const [a, m, d] = String(v).split("-");
  return `${d}/${m}/${a}`;
};

function Matricula() {
  const toast = useToast();
  const navigate = useNavigate();

  const [tab, setTab] = useState("preinscritos");
  const [busqueda, setBusqueda] = useState("");
  const [termino, setTermino] = useState("");
  const [filas, setFilas] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const [conteos, setConteos] = useState({
    preinscritos: "—",
    proceso: "—",
    formalizadas: "—",
    retiradas: "—",
  });
  const [anioNombre, setAnioNombre] = useState("—");
  const [secciones, setSecciones] = useState([]);
  const [accionId, setAccionId] = useState(null);
  const peticion = useRef(0);

  const tabActual = TABS.find((t) => t.clave === tab);
  const esCerrada = ["completada", "retirada"].includes(tabActual.estado);

  // Totales de cada pestaña (size=1: solo interesa totalElements)
  const cargarConteos = useCallback(async () => {
    const respuestas = await Promise.allSettled(
      TABS.map((t) =>
        api.get("/matriculas/listado", {
          params: { estado: t.estado, page: 0, size: 1 },
        }),
      ),
    );
    const nuevos = {};
    TABS.forEach((t, i) => {
      nuevos[t.clave] =
        respuestas[i].status === "fulfilled"
          ? respuestas[i].value.data.totalElements
          : "—";
    });
    setConteos(nuevos);
  }, []);

  useEffect(() => {
    cargarConteos();
    api
      .get("/matriculas/calendario")
      .then(({ data }) => {
        const a = data.anioActivo;
        setAnioNombre(a?.nombre ?? (a?.anio ? String(a.anio) : "—"));
      })
      .catch(() => setAnioNombre("—"));
    api
      .get("/catalogos/secciones", { params: { page: 0, size: 100 } })
      .then(({ data }) => setSecciones(data.content))
      .catch(() => setSecciones([]));
  }, [cargarConteos]);

  // Búsqueda con retardo para no consultar en cada tecla
  useEffect(() => {
    const id = setTimeout(() => setTermino(busqueda.trim()), 400);
    return () => clearTimeout(id);
  }, [busqueda]);

  const nombreSeccion = useMemo(() => {
    const mapa = {};
    secciones.forEach((s) => {
      mapa[s.id] = `${s.gradoNombre ?? ""} · Sección ${s.letraSeccion}`;
    });
    return (id) => mapa[id] ?? "—";
  }, [secciones]);

  const cargar = useCallback(
    async (page = 0) => {
      const numero = ++peticion.current;
      setCargando(true);
      setError("");
      try {
        const { data } = await api.get("/matriculas/listado", {
          params: {
            estado: tabActual.estado,
            busqueda: termino || undefined,
            page,
            size: 25,
          },
        });
        if (numero !== peticion.current) return; // llegó una respuesta más nueva
        setFilas(data.content);
        setPagina({
          page: data.page,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
        });
      } catch (err) {
        if (numero !== peticion.current) return;
        setError(
          extractErrorMessage(err, "No se pudo cargar la lista de matrículas."),
        );
        setFilas([]);
      } finally {
        if (numero === peticion.current) setCargando(false);
      }
    },
    [tabActual.estado, termino],
  );

  useEffect(() => {
    cargar(0);
  }, [cargar]);

  // ---------- Acciones ----------
  const descargarPdf = async (f, ruta, nombre, mensajeError) => {
    setAccionId(f.matriculaId);
    try {
      const { data } = await api.get(ruta, { responseType: "blob" });
      descargarArchivo(data, nombre);
    } catch (err) {
      toast.error(await mensajeErrorBlob(err, mensajeError));
    } finally {
      setAccionId(null);
    }
  };

  const descargarConstanciaCupo = (f) =>
    descargarPdf(
      f,
      `/matriculas/${f.matriculaId}/constancia-asignacion-cupo`,
      `constancia-aceptacion-cupo-${f.matriculaId}.pdf`,
      "No se pudo descargar la constancia de cupo.",
    );

  const descargarConstanciaInscripcion = (f) =>
    descargarPdf(
      f,
      `/matriculas/${f.matriculaId}/constancia-inscripcion`,
      `constancia-inscripcion-${f.matriculaId}.pdf`,
      "No se pudo descargar la constancia de inscripción.",
    );

  // La constancia de retiro necesita el id del retiro: se toma el más reciente
  const descargarConstanciaRetiro = async (f) => {
    setAccionId(f.matriculaId);
    try {
      const { data: historial } = await api.get(
        `/matriculas/${f.matriculaId}/retiros`,
      );
      const retiros = historial.retiros ?? [];
      if (retiros.length === 0) {
        toast.error("Esta matrícula no tiene un retiro registrado.");
        return;
      }
      const ultimo = [...retiros].sort((a, b) =>
        String(b.creadoEn).localeCompare(String(a.creadoEn)),
      )[0];
      const { data } = await api.get(
        `/matriculas/${f.matriculaId}/retiros/${ultimo.retiroId}/constancia-retiro`,
        { responseType: "blob" },
      );
      descargarArchivo(data, `constancia-retiro-${ultimo.retiroId}.pdf`);
    } catch (err) {
      toast.error(
        await mensajeErrorBlob(
          err,
          "No se pudo descargar la constancia de retiro.",
        ),
      );
    } finally {
      setAccionId(null);
    }
  };

  const iniciarInscripcion = async (f) => {
    setAccionId(f.matriculaId);
    try {
      await api.post(`/matriculas/${f.matriculaId}/checklist`);
      navigate(`/matricula/${f.matriculaId}/inscripcion`);
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo iniciar la inscripción."),
      );
      setAccionId(null);
    }
  };

  const renderAcciones = (f, ocupada) => {
    const ruta = `/matricula/${f.matriculaId}/inscripcion`;
    switch (f.estadoMatricula) {
      case "preinscrito":
        return (
          <div className="mat-acciones">
            <button
              type="button"
              className="btn-edit"
              disabled={ocupada}
              onClick={() => iniciarInscripcion(f)}
            >
              {ocupada ? "Abriendo..." : "Iniciar inscripción"}
            </button>
            <button
              type="button"
              className="btn-reactivar"
              disabled={ocupada}
              onClick={() => descargarConstanciaCupo(f)}
            >
              Constancia de cupo
            </button>
          </div>
        );
      case "en_proceso":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              Continuar
            </Link>
            <button
              type="button"
              className="btn-reactivar"
              disabled={ocupada}
              onClick={() => descargarConstanciaCupo(f)}
            >
              Constancia de cupo
            </button>
          </div>
        );
      case "completada":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              Ver inscripción
            </Link>
            <button
              type="button"
              className="btn-reactivar"
              disabled={ocupada}
              onClick={() => descargarConstanciaInscripcion(f)}
            >
              Constancia
            </button>
          </div>
        );
      case "retirada":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              Ver detalle
            </Link>
            <button
              type="button"
              className="btn-reactivar"
              disabled={ocupada}
              onClick={() => descargarConstanciaRetiro(f)}
            >
              Constancia
            </button>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>Matrícula</h1>
        <div className="mod-heading-actions">
          <Link to="/matricula/preinscripcion" className="btn-primary">
            + Nueva preinscripción
          </Link>
        </div>
      </div>
      <p className="mat-subtitulo">
        Año escolar activo: <strong>{anioNombre}</strong>
      </p>

      <div className="stats-row mat-stats">
        <div className="stat-card stat-card--azul">
          <div className="stat-value">{conteos.preinscritos}</div>
          <div className="stat-label">Preinscritos</div>
        </div>
        <div className="stat-card stat-card--naranja">
          <div className="stat-value">{conteos.proceso}</div>
          <div className="stat-label">En proceso</div>
        </div>
        <div className="stat-card stat-card--verde">
          <div className="stat-value">{conteos.formalizadas}</div>
          <div className="stat-label">Formalizadas</div>
        </div>
        <div className="stat-card stat-card--rojo">
          <div className="stat-value">{conteos.retiradas}</div>
          <div className="stat-label">Retiradas</div>
        </div>
      </div>

      <div className="mat-tabs" role="tablist">
        {TABS.map((t) => (
          <button
            key={t.clave}
            type="button"
            role="tab"
            aria-selected={tab === t.clave}
            className={tab === t.clave ? "activa" : ""}
            onClick={() => setTab(t.clave)}
          >
            {t.etiqueta}
            <span className="mat-tab-cuenta">{conteos[t.clave]}</span>
          </button>
        ))}
      </div>

      <div className="filters-bar">
        <label>
          Buscar
          <input
            type="search"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Nombre, apellido o cédula"
          />
        </label>
      </div>

      {error && <p className="error-msg">{error}</p>}

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Estudiante</th>
              <th>Sección</th>
              <th>Solicitud</th>
              <th>{esCerrada ? "Formalización" : "Requisitos"}</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={6} className="tabla-estado">
                  Cargando matrículas...
                </td>
              </tr>
            ) : filas.length === 0 ? (
              <tr>
                <td colSpan={6} className="tabla-estado">
                  <span className="tabla-estado-icono">
                    <Icon icon={ICONOS.matricula} />
                  </span>
                  {termino
                    ? "No hay resultados para esa búsqueda."
                    : tabActual.vacio}
                </td>
              </tr>
            ) : (
              filas.map((f) => {
                const total = f.requisitosObligatorios ?? 0;
                const hechos = f.requisitosObligatoriosCumplidos ?? 0;
                const pct = total > 0 ? Math.round((hechos / total) * 100) : 0;
                const ocupada = accionId === f.matriculaId;
                return (
                  <tr key={f.matriculaId}>
                    <td data-label="Estudiante">
                      {f.estudianteNombreCompleto}
                    </td>
                    <td data-label="Sección">{nombreSeccion(f.seccionId)}</td>
                    <td data-label="Solicitud">
                      {formatFechaISO(f.fechaSolicitud)}
                    </td>
                    <td data-label={esCerrada ? "Formalización" : "Requisitos"}>
                      {esCerrada ? (
                        <span className="mat-formal">
                          {formatFechaISO(f.fechaFormalizacion)}
                          {f.numeroConstancia && (
                            <small className="mat-constancia">
                              N.º {f.numeroConstancia}
                            </small>
                          )}
                        </span>
                      ) : total > 0 ? (
                        <span className="mat-progreso">
                          <span className="mat-barra" aria-hidden="true">
                            <span style={{ width: `${pct}%` }} />
                          </span>
                          <small>
                            {hechos}/{total}
                          </small>
                        </span>
                      ) : (
                        "—"
                      )}
                    </td>
                    <td data-label="Estado">
                      <span
                        className={`badge mat-estado--${f.estadoMatricula}`}
                      >
                        {ETIQUETA_ESTADO[f.estadoMatricula] ??
                          f.estadoMatricula}
                      </span>
                    </td>
                    <td data-label="Acciones">{renderAcciones(f, ocupada)}</td>
                  </tr>
                );
              })
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
            {pagina.totalElements} registros)
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
  );
}

export default Matricula;
