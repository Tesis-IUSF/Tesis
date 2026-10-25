// src/pages/Matricula.jsx
import { useState, useEffect, useCallback, useMemo } from "react";
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
    ruta: "/matriculas/preinscripciones",
  },
  { clave: "proceso", etiqueta: "En proceso", ruta: "/matriculas/en-proceso" },
];

const ETIQUETA_ESTADO = {
  preinscrito: "Preinscrito",
  en_proceso: "En proceso",
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
  const [filas, setFilas] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const [conteos, setConteos] = useState({ preinscritos: "—", proceso: "—" });
  const [anioNombre, setAnioNombre] = useState("—");
  const [secciones, setSecciones] = useState([]);
  const [accionId, setAccionId] = useState(null);

  const rutaActual = TABS.find((t) => t.clave === tab).ruta;

  // Totales de las dos pestañas (se piden con size=1, solo interesa totalElements)
  const cargarConteos = useCallback(async () => {
    const [pre, proc] = await Promise.allSettled([
      api.get("/matriculas/preinscripciones", { params: { page: 0, size: 1 } }),
      api.get("/matriculas/en-proceso", { params: { page: 0, size: 1 } }),
    ]);
    setConteos({
      preinscritos:
        pre.status === "fulfilled" ? pre.value.data.totalElements : "—",
      proceso:
        proc.status === "fulfilled" ? proc.value.data.totalElements : "—",
    });
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

  const nombreSeccion = useMemo(() => {
    const mapa = {};
    secciones.forEach((s) => {
      mapa[s.id] = `${s.gradoNombre ?? ""} · Sección ${s.letraSeccion}`;
    });
    return (id) => mapa[id] ?? "—";
  }, [secciones]);

  const cargar = useCallback(
    async (page = 0) => {
      setCargando(true);
      setError("");
      try {
        const { data } = await api.get(rutaActual, {
          params: { page, size: 25 },
        });
        setFilas(data.content);
        setPagina({
          page: data.page,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
        });
      } catch (err) {
        setError(
          extractErrorMessage(err, "No se pudo cargar la lista de matrículas."),
        );
        setFilas([]);
      } finally {
        setCargando(false);
      }
    },
    [rutaActual],
  );

  useEffect(() => {
    cargar(0);
  }, [cargar]);

  // ---------- Acciones ----------
  const descargarConstancia = async (f) => {
    setAccionId(f.matriculaId);
    try {
      const { data } = await api.get(
        `/matriculas/${f.matriculaId}/constancia-asignacion-cupo`,
        { responseType: "blob" },
      );
      descargarArchivo(data, `constancia-aceptacion-cupo-${f.matriculaId}.pdf`);
    } catch (err) {
      toast.error(
        await mensajeErrorBlob(err, "No se pudo descargar la constancia."),
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

      <div className="stats-row">
        <div className="stat-card stat-card--azul">
          <div className="stat-value">{conteos.preinscritos}</div>
          <div className="stat-label">Preinscritos</div>
        </div>
        <div className="stat-card stat-card--naranja">
          <div className="stat-value">{conteos.proceso}</div>
          <div className="stat-label">En proceso de inscripción</div>
        </div>
        <div className="stat-card stat-card--dorado">
          <div className="stat-value mat-anio">{anioNombre}</div>
          <div className="stat-label">Año escolar activo</div>
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

      {error && <p className="error-msg">{error}</p>}

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Estudiante</th>
              <th>Sección</th>
              <th>Solicitud</th>
              <th>Requisitos</th>
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
                  {tab === "preinscritos"
                    ? "No hay preinscripciones pendientes. Registra una con «Nueva preinscripción»."
                    : "No hay inscripciones en proceso."}
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
                    <td data-label="Requisitos">
                      {total > 0 ? (
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
                    <td data-label="Acciones">
                      <Link
                        to={`/matricula/${f.matriculaId}/preinscripcion/editar`}
                        className="btn-edit"
                      >
                        Editar datos
                      </Link>
                      {f.estadoMatricula === "preinscrito" ? (
                        <button
                          type="button"
                          className="btn-edit"
                          disabled={ocupada}
                          onClick={() => iniciarInscripcion(f)}
                        >
                          {ocupada ? "Abriendo..." : "Iniciar inscripción"}
                        </button>
                      ) : (
                        <Link
                          to={`/matricula/${f.matriculaId}/inscripcion`}
                          className="btn-edit"
                        >
                          Continuar
                        </Link>
                      )}
                      <button
                        type="button"
                        className="btn-reactivar"
                        disabled={ocupada}
                        onClick={() => descargarConstancia(f)}
                      >
                        Constancia de cupo
                      </button>
                    </td>
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
