// src/pages/Matricula.jsx
import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import { descargarArchivo, mensajeErrorBlob } from "../utils/descargas";
import ModalFormulario from "../components/ModalFormulario";
import "./Matricula.css";

// Pestaña con la que abre la pantalla. Cuando el backend acepte listar sin
// estado ("Todos"), cambiar a "todos".
const TAB_INICIAL = "preinscritos";

const TABS = [
  {
    clave: "todos",
    etiqueta: "Todos",
    estado: null, // sin filtro de estado
    vacio: "Todavía no hay matrículas registradas.",
  },
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

// Pestañas que tienen un estado propio (para los contadores)
const TABS_CON_ESTADO = TABS.filter((t) => t.estado);

const ETIQUETA_ESTADO = {
  preinscrito: "Preinscrito",
  en_proceso: "En proceso",
  completada: "Formalizada",
  retirada: "Retirada",
};

const MOTIVOS_ANULACION = [
  "Registro duplicado",
  "Error en los datos",
  "El representante desistió",
  "Otro",
];

const MOTIVOS_RETIRO = [
  "Cambio de institución",
  "Cambio de residencia",
  "Motivos personales o familiares",
  "Otro",
];

const hoyISO = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

const ACCION_VACIA = {
  motivoTipo: "",
  motivoOtro: "",
  fecha: "",
  solicitanteNombre: "",
  solicitanteApellido: "",
  solicitanteCedula: "",
};

const o = (v) => (v && v.trim() ? v.trim() : null);

// "2026-10-22" -> "22/10/2026" sin pasar por Date (evita desfase de zona horaria)
const formatFechaISO = (v) => {
  if (!v) return "—";
  const [a, m, d] = String(v).split("-");
  return `${d}/${m}/${a}`;
};

function Matricula() {
  const toast = useToast();
  const navigate = useNavigate();

  const [tab, setTab] = useState(TAB_INICIAL);
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
    todos: "—",
    preinscritos: "—",
    proceso: "—",
    formalizadas: "—",
    retiradas: "—",
  });
  const [anioNombre, setAnioNombre] = useState("—");
  const [secciones, setSecciones] = useState([]);
  const [accionId, setAccionId] = useState(null);
  const peticion = useRef(0);

  // Detalle del estudiante (ventana con las acciones delicadas)
  const [detalle, setDetalle] = useState(null);

  // Anulación (preinscrito / en proceso) o retiro (formalizada, solo en Todos)
  const [aGestionar, setAGestionar] = useState(null); // fila elegida
  const [accion, setAccion] = useState(ACCION_VACIA);
  const [errorAccion, setErrorAccion] = useState("");
  const [procesando, setProcesando] = useState(false);

  const tabActual = TABS.find((t) => t.clave === tab);
  const estadoTab = tabActual.estado;
  const esCerrada = ["completada", "retirada"].includes(estadoTab);
  // "Todos" y "Preinscritos" muestran la cédula en la cuarta columna
  const columnaCedula = estadoTab === null || estadoTab === "preinscrito";
  const etiquetaColumna = columnaCedula
    ? "Cédula"
    : esCerrada
      ? "Formalización"
      : "Requisitos";

  const esRetiro = aGestionar?.estadoMatricula === "completada";
  const esPreinscripcion = aGestionar?.estadoMatricula === "preinscrito";

  // Totales de cada pestaña (size=1: solo interesa totalElements)
  const cargarConteos = useCallback(async () => {
    const respuestas = await Promise.allSettled(
      TABS_CON_ESTADO.map((t) =>
        api.get("/matriculas/listado", {
          params: { estado: t.estado, page: 0, size: 1 },
        }),
      ),
    );
    const nuevos = {};
    let suma = 0;
    let completo = true;
    TABS_CON_ESTADO.forEach((t, i) => {
      if (respuestas[i].status === "fulfilled") {
        nuevos[t.clave] = respuestas[i].value.data.totalElements;
        suma += respuestas[i].value.data.totalElements;
      } else {
        nuevos[t.clave] = "—";
        completo = false;
      }
    });
    nuevos.todos = completo ? suma : "—";
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
            estado: estadoTab ?? undefined,
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
    [estadoTab, termino],
  );

  useEffect(() => {
    cargar(0);
  }, [cargar]);

  // ---------- Descargas ----------
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

  // ---------- Anulación / retiro ----------
  // Qué acción delicada tiene una fila en la pestaña actual (null = ninguna).
  // El retiro de una formalizada solo se ofrece desde la pestaña "Todos".
  const etiquetaAccion = (f) => {
    if (f.estadoMatricula === "preinscrito") return "Anular preinscripción";
    if (f.estadoMatricula === "en_proceso") return "Anular inscripción";
    if (f.estadoMatricula === "completada" && tab === "todos")
      return "Retirar estudiante";
    return null;
  };

  const abrirAccion = (f) => {
    setDetalle(null);
    setAccion({ ...ACCION_VACIA, fecha: hoyISO() });
    setErrorAccion("");
    setAGestionar(f);
  };

  const cerrarAccion = () => {
    if (!procesando) setAGestionar(null);
  };

  const cambiarAccion = (campo, valor) =>
    setAccion((p) => ({ ...p, [campo]: valor }));

  const confirmarAccion = async (ev) => {
    ev.preventDefault();
    const motivo =
      accion.motivoTipo === "Otro"
        ? accion.motivoOtro.trim()
        : accion.motivoTipo;

    if (!accion.motivoTipo) {
      setErrorAccion("Selecciona el motivo.");
      return;
    }
    if (!motivo) {
      setErrorAccion("Describe el motivo.");
      return;
    }
    if (!accion.fecha) {
      setErrorAccion("La fecha es obligatoria.");
      return;
    }
    if (accion.solicitanteNombre.trim().length > 100) {
      setErrorAccion("El nombre del solicitante admite máximo 100 caracteres.");
      return;
    }
    if (accion.solicitanteApellido.trim().length > 100) {
      setErrorAccion(
        "El apellido del solicitante admite máximo 100 caracteres.",
      );
      return;
    }
    if (accion.solicitanteCedula.trim().length > 20) {
      setErrorAccion("La cédula del solicitante admite máximo 20 caracteres.");
      return;
    }

    setProcesando(true);
    setErrorAccion("");
    const retiro = esRetiro;
    const preinscripcion = esPreinscripcion;
    const matriculaId = aGestionar.matriculaId;
    try {
      // El backend decide qué hacer según el estado de la matrícula
      const { data } = await api.post(`/matriculas/${matriculaId}/retiros`, {
        motivo,
        fechaRetiro: accion.fecha,
        solicitanteNombre: o(accion.solicitanteNombre),
        solicitanteApellido: o(accion.solicitanteApellido),
        solicitanteCedula: o(accion.solicitanteCedula),
      });

      if (retiro) {
        toast.success("Retiro registrado correctamente.");
        // Se entrega la constancia de retiro de inmediato
        if (data?.retiroId) {
          try {
            const { data: pdf } = await api.get(
              `/matriculas/${matriculaId}/retiros/${data.retiroId}/constancia-retiro`,
              { responseType: "blob" },
            );
            descargarArchivo(pdf, `constancia-retiro-${data.retiroId}.pdf`);
          } catch (errPdf) {
            toast.error(
              await mensajeErrorBlob(
                errPdf,
                "El retiro se registró, pero no se pudo descargar la constancia. Búscala en la pestaña Retiradas.",
              ),
            );
          }
        }
      } else {
        toast.success(
          preinscripcion
            ? "Preinscripción anulada correctamente."
            : "Inscripción anulada correctamente.",
        );
      }

      setAGestionar(null);
      cargarConteos();
      cargar(0);
    } catch (err) {
      setErrorAccion(
        extractErrorMessage(
          err,
          retiro
            ? "No se pudo registrar el retiro."
            : "No se pudo anular el registro.",
        ),
      );
    } finally {
      setProcesando(false);
    }
  };

  // Botón compacto "⋯": solo si la fila tiene una acción disponible
  const botonMas = (f) =>
    etiquetaAccion(f) ? (
      <button
        type="button"
        className="btn-secondary mat-mas"
        title="Más acciones"
        aria-label={`Más acciones de ${f.estudianteNombreCompleto}`}
        onClick={() => setDetalle(f)}
      >
        <Icon icon="mdi:dots-horizontal" width="18" height="18" />
      </button>
    ) : null;

  // Acciones de la fila: la principal del estado + la constancia (+ "⋯")
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
              {ocupada ? "Abriendo..." : "Inscribir"}
            </button>
            <button
              type="button"
              className="btn-reactivar"
              disabled={ocupada}
              onClick={() => descargarConstanciaCupo(f)}
            >
              Constancia
            </button>
            {botonMas(f)}
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
              Constancia
            </button>
            {botonMas(f)}
          </div>
        );
      case "completada":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              Ver
            </Link>
            <button
              type="button"
              className="btn-reactivar"
              disabled={ocupada}
              onClick={() => descargarConstanciaInscripcion(f)}
            >
              Constancia
            </button>
            {botonMas(f)}
          </div>
        );
      case "retirada":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              Ver
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

  const celdaColumna = (f, total, hechos, pct) => {
    if (columnaCedula) return f.estudianteCedula ?? "—";
    if (esCerrada) return formatFechaISO(f.fechaFormalizacion);
    if (total > 0) {
      return (
        <span className="mat-progreso">
          <span className="mat-barra" aria-hidden="true">
            <span style={{ width: `${pct}%` }} />
          </span>
          <small>
            {hechos}/{total}
          </small>
        </span>
      );
    }
    return "—";
  };

  const motivos = esRetiro ? MOTIVOS_RETIRO : MOTIVOS_ANULACION;
  const tituloAccion = aGestionar ? etiquetaAccion(aGestionar) : "";

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
              <th>{etiquetaColumna}</th>
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
                      <button
                        type="button"
                        className="mat-nombre-link"
                        title="Ver datos del estudiante"
                        onClick={() => setDetalle(f)}
                      >
                        {f.estudianteNombreCompleto}
                      </button>
                    </td>
                    <td data-label="Sección">{nombreSeccion(f.seccionId)}</td>
                    <td data-label="Solicitud">
                      {formatFechaISO(f.fechaSolicitud)}
                    </td>
                    <td data-label={etiquetaColumna}>
                      {celdaColumna(f, total, hechos, pct)}
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

      {/* Detalle del estudiante */}
      {detalle && (
        <div className="mf-overlay" onClick={() => setDetalle(null)}>
          <div
            className="mf-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="mat-detalle-titulo"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="mat-detalle-titulo">{detalle.estudianteNombreCompleto}</h2>

            <dl className="mat-detalle">
              <div>
                <dt>Cédula</dt>
                <dd>{detalle.estudianteCedula ?? "—"}</dd>
              </div>
              <div>
                <dt>Sección</dt>
                <dd>{nombreSeccion(detalle.seccionId)}</dd>
              </div>
              <div>
                <dt>Año escolar</dt>
                <dd>{detalle.anioEscolar ?? "—"}</dd>
              </div>
              <div>
                <dt>Estado</dt>
                <dd>
                  <span
                    className={`badge mat-estado--${detalle.estadoMatricula}`}
                  >
                    {ETIQUETA_ESTADO[detalle.estadoMatricula] ??
                      detalle.estadoMatricula}
                  </span>
                </dd>
              </div>
              <div>
                <dt>Solicitud</dt>
                <dd>{formatFechaISO(detalle.fechaSolicitud)}</dd>
              </div>
              {detalle.fechaFormalizacion && (
                <div>
                  <dt>Formalización</dt>
                  <dd>{formatFechaISO(detalle.fechaFormalizacion)}</dd>
                </div>
              )}
              {(detalle.requisitosObligatorios ?? 0) > 0 && (
                <div>
                  <dt>Requisitos obligatorios</dt>
                  <dd>
                    {detalle.requisitosObligatoriosCumplidos ?? 0} de{" "}
                    {detalle.requisitosObligatorios}
                  </dd>
                </div>
              )}
            </dl>

            <div className="mf-acciones">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setDetalle(null)}
              >
                Cerrar
              </button>
              {etiquetaAccion(detalle) && (
                <button
                  type="button"
                  className="btn-delete"
                  onClick={() => abrirAccion(detalle)}
                >
                  {etiquetaAccion(detalle)}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Anulación o retiro */}
      {aGestionar && (
        <ModalFormulario
          titulo={tituloAccion}
          onCerrar={cerrarAccion}
          onSubmit={confirmarAccion}
          guardando={procesando}
          etiquetaGuardar={esRetiro ? "Registrar retiro" : tituloAccion}
        >
          <div className="mat-retiro-form">
            <p className="mat-retiro-texto">
              {esRetiro ? (
                <>
                  Se registrará el retiro de{" "}
                  <strong>{aGestionar.estudianteNombreCompleto}</strong>. Al
                  terminar se descargará su constancia de retiro.
                </>
              ) : (
                <>
                  Se anulará{" "}
                  {esPreinscripcion ? "la preinscripción" : "la inscripción"} de{" "}
                  <strong>{aGestionar.estudianteNombreCompleto}</strong>. Esta
                  acción no se puede deshacer.
                </>
              )}
            </p>

            {errorAccion && <p className="error-msg">{errorAccion}</p>}

            <label>
              Motivo *
              <select
                value={accion.motivoTipo}
                onChange={(e) => cambiarAccion("motivoTipo", e.target.value)}
                autoFocus
              >
                <option value="">Selecciona...</option>
                {motivos.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </label>

            {accion.motivoTipo === "Otro" && (
              <label>
                Describe el motivo *
                <textarea
                  rows={2}
                  value={accion.motivoOtro}
                  onChange={(e) => cambiarAccion("motivoOtro", e.target.value)}
                />
              </label>
            )}

            <label>
              {esRetiro ? "Fecha de retiro *" : "Fecha *"}
              <input
                type="date"
                value={accion.fecha}
                onChange={(e) => cambiarAccion("fecha", e.target.value)}
              />
            </label>

            <p className="mat-retiro-subtitulo">Quien lo solicita (opcional)</p>

            <label>
              Nombres
              <input
                value={accion.solicitanteNombre}
                onChange={(e) =>
                  cambiarAccion("solicitanteNombre", e.target.value)
                }
                maxLength={100}
              />
            </label>

            <label>
              Apellidos
              <input
                value={accion.solicitanteApellido}
                onChange={(e) =>
                  cambiarAccion("solicitanteApellido", e.target.value)
                }
                maxLength={100}
              />
            </label>

            <label>
              Cédula
              <input
                value={accion.solicitanteCedula}
                onChange={(e) =>
                  cambiarAccion("solicitanteCedula", e.target.value)
                }
                maxLength={20}
              />
            </label>
          </div>
        </ModalFormulario>
      )}
    </div>
  );
}

export default Matricula;
