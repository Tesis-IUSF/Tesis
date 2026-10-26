// src/pages/Matricula.jsx
import { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import { descargarArchivo, mensajeErrorBlob } from "../utils/descargas";
import ModalFormulario from "../components/ModalFormulario";
import "./Matricula.css";

const TABS = [
  { clave: "preinscritos", etiqueta: "Preinscritos", estado: "preinscrito" },
  { clave: "proceso", etiqueta: "En proceso", estado: "en_proceso" },
  { clave: "completadas", etiqueta: "Completadas", estado: "completada" },
  { clave: "retiradas", etiqueta: "Retiradas", estado: "retirada" },
  { clave: "graduados", etiqueta: "Posibles graduados", graduados: true },
];

const MENSAJE_VACIO = {
  preinscritos: "No hay preinscripciones pendientes con los filtros seleccionados.",
  proceso: "No hay inscripciones en proceso con los filtros seleccionados.",
  completadas: "No hay matrículas completadas con los filtros seleccionados.",
  retiradas: "No hay matrículas retiradas con los filtros seleccionados.",
};

const FILTROS_VACIOS = { anioEscolar: "", seccionId: "" };

const fechaLocalISO = () => {
  const fecha = new Date();
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, "0");
  const dia = String(fecha.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
};

// "2026-10-22" -> "22/10/2026" sin pasar por Date (evita desfase de zona horaria)
const formatFechaISO = (v) => {
  if (!v) return "—";
  const [a, m, d] = String(v).split("-");
  return `${d}/${m}/${a}`;
};

function Matricula({ soloConsulta = false, permitirRetiros = false }) {
  const toast = useToast();

  const [tab, setTab] = useState("preinscritos");
  const [filas, setFilas] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [paginaSolicitada, setPaginaSolicitada] = useState(0);
  const [recargar, setRecargar] = useState(0);
  const [recargarConteos, setRecargarConteos] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const [errorCatalogos, setErrorCatalogos] = useState("");
  const [catalogosCargados, setCatalogosCargados] = useState(false);
  const [conteos, setConteos] = useState(
    Object.fromEntries(TABS.map((t) => [t.clave, "—"])),
  );
  const [anioActivo, setAnioActivo] = useState(null);
  const [anios, setAnios] = useState([]);
  const [secciones, setSecciones] = useState([]);
  const [accionId, setAccionId] = useState(null);
  const [retiroMatricula, setRetiroMatricula] = useState(null);
  const [representantesAutorizados, setRepresentantesAutorizados] = useState([]);
  const [tipoSolicitante, setTipoSolicitante] = useState("representante");
  const [representanteRetiroId, setRepresentanteRetiroId] = useState("");
  const [datosSolicitante, setDatosSolicitante] = useState({
    nombre: "",
    apellido: "",
    cedula: "",
    motivo: "",
    fechaRetiro: fechaLocalISO(),
  });
  const [retirando, setRetirando] = useState(false);
  const [busqueda, setBusqueda] = useState("");
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [filtrosAplicados, setFiltrosAplicados] = useState(FILTROS_VACIOS);

  const tabActual = TABS.find((t) => t.clave === tab) ?? TABS[0];
  const mostrarRequisitos = tabActual.clave !== "preinscritos";

  useEffect(() => {
    let vigente = true;
    Promise.allSettled(
      TABS.map((t) =>
        api.get(
          t.graduados
            ? "/matriculas/posibles-graduados"
            : "/matriculas/listado",
          {
            params: {
              ...(t.estado ? { estado: t.estado } : {}),
              page: 0,
              size: 1,
            },
          },
        ),
      ),
    ).then((respuestas) => {
      if (!vigente) return;
      setConteos(
        Object.fromEntries(
          TABS.map((t, index) => [
            t.clave,
            respuestas[index].status === "fulfilled"
              ? respuestas[index].value.data.totalElements
              : "—",
          ]),
        ),
      );
    });
    return () => {
      vigente = false;
    };
  }, [recargarConteos]);

  useEffect(() => {
    let vigente = true;
    Promise.allSettled([
      api.get("/matriculas/gestion/anios"),
      api.get("/catalogos/secciones", { params: { page: 0, size: 100 } }),
      api.get("/matriculas/calendario"),
    ]).then(([respuestaAnios, respuestaSecciones, calendario]) => {
      if (!vigente) return;
      const errores = [];
      if (respuestaAnios.status === "fulfilled") {
        setAnios(respuestaAnios.value.data);
      } else {
        errores.push("años escolares");
      }
      if (respuestaSecciones.status === "fulfilled") {
        setSecciones(respuestaSecciones.value.data.content);
      } else {
        errores.push("secciones");
      }
      if (calendario.status === "fulfilled") {
        const activo = calendario.value.data.anioActivo;
        setAnioActivo(activo);
        const anioActivoSeleccionado = activo?.anio
          ? String(activo.anio)
          : "";
        const filtrosIniciales = {
          anioEscolar: anioActivoSeleccionado,
          seccionId: "",
        };
        setFiltros(filtrosIniciales);
        setFiltrosAplicados(filtrosIniciales);
      } else {
        errores.push("año escolar activo");
      }
      if (errores.length > 0) {
        setErrorCatalogos(
          `No se pudieron cargar los filtros de ${errores.join(", ")}.`,
        );
      }
      setCatalogosCargados(true);
    });
    return () => {
      vigente = false;
    };
  }, []);

  const nombreSeccion = useMemo(() => {
    const mapa = {};
    secciones.forEach((s) => {
      mapa[s.id] = `${s.gradoNombre ?? ""} · Sección ${s.letraSeccion}`;
    });
    return (id) => mapa[id] ?? "—";
  }, [secciones]);

  const anioActivoNumero = anioActivo?.anio ? String(anioActivo.anio) : "";
  const anioNombre =
    anioActivo?.nombre ??
    (anioActivo?.anio ? String(anioActivo.anio) : "—");

  const seccionesFiltradas = useMemo(() => {
    const anio = tabActual.graduados
      ? anioActivoNumero
      : filtros.anioEscolar;
    return secciones.filter(
      (seccion) =>
        (!anio || String(seccion.anioEscolar) === anio),
    );
  }, [
    anioActivoNumero,
    filtros.anioEscolar,
    secciones,
    tabActual.graduados,
  ]);

  useEffect(() => {
    if (!catalogosCargados) return undefined;
    let vigente = true;
    const params = { page: paginaSolicitada, size: 25 };
    if (busquedaAplicada) params.q = busquedaAplicada;
    if (tabActual.graduados) {
      if (filtrosAplicados.seccionId) {
        params.seccionId = filtrosAplicados.seccionId;
      }
    } else {
      params.estado = tabActual.estado;
      if (filtrosAplicados.anioEscolar) {
        params.anioEscolar = filtrosAplicados.anioEscolar;
      }
      if (filtrosAplicados.seccionId) {
        params.seccionId = filtrosAplicados.seccionId;
      }
    }
    const ruta = tabActual.graduados
      ? "/matriculas/posibles-graduados"
      : "/matriculas/listado";
    api
      .get(ruta, { params })
      .then(({ data }) => {
        if (!vigente) return;
        setFilas(data.content);
        setPagina({
          page: data.page,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
        });
        setError("");
      })
      .catch((err) => {
        if (!vigente) return;
        setError(
          extractErrorMessage(err, "No se pudo cargar la lista de matrículas."),
        );
        setFilas([]);
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });
    return () => {
      vigente = false;
    };
  }, [
    busquedaAplicada,
    catalogosCargados,
    filtrosAplicados,
    paginaSolicitada,
    recargar,
    tabActual,
  ]);

  const aplicarFiltros = (event) => {
    event.preventDefault();
    setCargando(true);
    setError("");
    setBusquedaAplicada(busqueda.trim());
    setFiltrosAplicados(filtros);
    setPaginaSolicitada(0);
    setRecargar((version) => version + 1);
  };

  const limpiarFiltros = () => {
    setCargando(true);
    setError("");
    setBusqueda("");
    setBusquedaAplicada("");
    setFiltros(FILTROS_VACIOS);
    setFiltrosAplicados(FILTROS_VACIOS);
    setPaginaSolicitada(0);
    setRecargar((version) => version + 1);
  };

  const cambiarTab = (clave) => {
    setCargando(true);
    setError("");
    setTab(clave);
    setPaginaSolicitada(0);
  };

  const cambiarPagina = (page) => {
    setCargando(true);
    setError("");
    setPaginaSolicitada(page);
  };

  // ---------- Acciones ----------
  const abrirRetiro = async (matricula) => {
    if (!permitirRetiros) return;
    setAccionId(matricula.matriculaId);
    try {
      const { data } = await api.get(
        `/matriculas/${matricula.matriculaId}/retiros/representantes-autorizados`,
      );
      setRepresentantesAutorizados(data);
      setRepresentanteRetiroId(data[0] ? String(data[0].id) : "");
      setTipoSolicitante(data.length > 0 ? "representante" : "externo");
      setDatosSolicitante({
        nombre: "",
        apellido: "",
        cedula: "",
        motivo: "",
        fechaRetiro: fechaLocalISO(),
      });
      setRetiroMatricula(matricula);
    } catch (err) {
      toast.error(
        extractErrorMessage(
          err,
          "No se pudieron cargar los representantes autorizados.",
        ),
      );
    } finally {
      setAccionId(null);
    }
  };

  const enviarRetiro = async (event) => {
    event.preventDefault();
    if (!retiroMatricula) return;
    const camposSolicitanteValidos =
      tipoSolicitante === "representante"
        ? Boolean(representanteRetiroId)
        : Boolean(
            datosSolicitante.nombre.trim() &&
              datosSolicitante.apellido.trim() &&
              datosSolicitante.cedula.trim(),
          );
    if (
      !datosSolicitante.motivo.trim() ||
      !datosSolicitante.fechaRetiro ||
      !camposSolicitanteValidos
    ) {
      toast.error("Completa los datos del solicitante, la fecha y el motivo.");
      return;
    }
    setRetirando(true);
    try {
      const request = {
        motivo: datosSolicitante.motivo.trim(),
        fechaRetiro: datosSolicitante.fechaRetiro,
        ...(tipoSolicitante === "representante"
          ? { representanteId: Number(representanteRetiroId) }
          : {
              solicitanteNombre: datosSolicitante.nombre.trim(),
              solicitanteApellido: datosSolicitante.apellido.trim(),
              solicitanteCedula: datosSolicitante.cedula.trim(),
            }),
      };
      await api.post(
        `/matriculas/${retiroMatricula.matriculaId}/retiros`,
        request,
      );
      toast.success("Retiro registrado correctamente.");
      setRetiroMatricula(null);
      setCargando(true);
      setRecargar((version) => version + 1);
      setRecargarConteos((version) => version + 1);
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo registrar el retiro."));
    } finally {
      setRetirando(false);
    }
  };

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

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>Matrícula</h1>
        {!soloConsulta && (
          <div className="mod-heading-actions">
            <Link to="/matricula/preinscripcion" className="btn-primary">
              + Nueva preinscripción
            </Link>
          </div>
        )}
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

      <form className="mat-filtros" onSubmit={aplicarFiltros}>
        <label>
          Buscar estudiante
          <input
            type="search"
            value={busqueda}
            onChange={(event) => setBusqueda(event.target.value)}
            placeholder="Nombre completo o cédula"
          />
        </label>
        <label>
          Año escolar
          {tabActual.graduados ? (
            <input
              value={anioActivo?.nombre ?? anioActivoNumero ?? ""}
              disabled
              aria-label="Año escolar activo para posibles graduados"
            />
          ) : (
            <select
              value={filtros.anioEscolar}
              onChange={(event) =>
                setFiltros((actual) => ({
                  ...actual,
                  anioEscolar: event.target.value,
                  seccionId: "",
                }))
              }
            >
              <option value="">Todos los años</option>
              {anios.map((anio) => (
                <option key={anio.anio} value={anio.anio}>
                  {anio.nombre}
                </option>
              ))}
            </select>
          )}
        </label>
        <label>
          Grado - sección
          <select
            value={filtros.seccionId}
            onChange={(event) =>
              setFiltros((actual) => ({
                ...actual,
                seccionId: event.target.value,
              }))
            }
          >
            <option value="">Todos los grados y secciones</option>
            {seccionesFiltradas.map((seccion) => (
              <option key={seccion.id} value={seccion.id}>
                {seccion.gradoNombre} - {seccion.letraSeccion}
                {!filtros.anioEscolar &&
                  ` ${anios.find(
                    (anio) => String(anio.anio) === String(seccion.anioEscolar),
                  )?.nombre ?? seccion.anioEscolar}`}
              </option>
            ))}
          </select>
        </label>
        <div className="mat-filtros-acciones">
          <button type="submit" className="btn-primary">
            Buscar
          </button>
          <button
            type="button"
            className="btn-secondary"
            onClick={limpiarFiltros}
          >
            Limpiar
          </button>
        </div>
      </form>

      <div className="mat-tabs" role="tablist">
        {TABS.map((t) => (
          <button
            key={t.clave}
            type="button"
            role="tab"
            aria-selected={tab === t.clave}
            className={tab === t.clave ? "activa" : ""}
            onClick={() => cambiarTab(t.clave)}
          >
            {t.etiqueta}
            <span className="mat-tab-cuenta">{conteos[t.clave]}</span>
          </button>
        ))}
      </div>

      {error && <p className="error-msg">{error}</p>}
      {errorCatalogos && <p className="error-msg">{errorCatalogos}</p>}

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Cédula</th>
              <th>Estudiante</th>
              <th>Sección</th>
              <th>Solicitud</th>
              {mostrarRequisitos && <th>Requisitos</th>}
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={mostrarRequisitos ? 6 : 5} className="tabla-estado">
                  Cargando matrículas...
                </td>
              </tr>
            ) : filas.length === 0 ? (
              <tr>
                <td colSpan={mostrarRequisitos ? 6 : 5} className="tabla-estado">
                  <span className="tabla-estado-icono">
                    <Icon icon={ICONOS.matricula} />
                  </span>
                  {tabActual.graduados
                    ? "No hay posibles graduados con los filtros seleccionados."
                    : MENSAJE_VACIO[tabActual.clave]}
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
                    <td className="mat-cedula" data-label="Cédula">
                      {f.estudianteCedula || "—"}
                    </td>
                    <td data-label="Estudiante">
                      {f.estudianteNombreCompleto}
                    </td>
                    <td data-label="Sección">{nombreSeccion(f.seccionId)}</td>
                    <td data-label="Solicitud">
                      {formatFechaISO(f.fechaSolicitud)}
                    </td>
                    {mostrarRequisitos && (
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
                    )}
                    <td data-label="Acciones">
                      <Link
                        to={`/matricula/${f.matriculaId}/inscripcion`}
                        className="btn-edit"
                      >
                        Ver expediente
                      </Link>
                      {permitirRetiros &&
                        f.estadoMatricula === "completada" && (
                        <button
                          type="button"
                          className="btn-reactivar"
                          disabled={ocupada}
                          onClick={() => abrirRetiro(f)}
                        >
                          {ocupada ? "Cargando..." : "Retirar"}
                        </button>
                        )}
                      {f.estadoMatricula === "preinscrito" && (
                          <button
                            type="button"
                            className="btn-reactivar"
                            disabled={ocupada}
                            onClick={() => descargarConstancia(f)}
                          >
                            Constancia de cupo
                          </button>
                      )}
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
            onClick={() => cambiarPagina(pagina.page - 1)}
          >
            ← Anterior
          </button>
          <span>
            Página {pagina.page + 1} de {pagina.totalPages} (
            {pagina.totalElements} registros)
          </span>
          <button
            disabled={pagina.page + 1 >= pagina.totalPages || cargando}
            onClick={() => cambiarPagina(pagina.page + 1)}
          >
            Siguiente →
          </button>
        </div>
      )}

      {permitirRetiros && retiroMatricula && (
        <ModalFormulario
          titulo="Registrar retiro de matrícula"
          onCerrar={() => setRetiroMatricula(null)}
          onSubmit={enviarRetiro}
          guardando={retirando}
          etiquetaGuardar="Registrar retiro"
        >
          <p>
            Matrícula de <strong>{retiroMatricula.estudianteNombreCompleto}</strong>
            {" · Año escolar "}
            {retiroMatricula.anioEscolar}
          </p>
          <label>
            Solicitante
            <select
              value={tipoSolicitante}
              onChange={(event) => setTipoSolicitante(event.target.value)}
            >
              {representantesAutorizados.length > 0 && (
                <option value="representante">
                  Representante vinculado y autorizado
                </option>
              )}
              <option value="externo">Solicitante externo</option>
            </select>
          </label>
          {tipoSolicitante === "representante" ? (
            <label>
              Representante autorizado
              <select
                value={representanteRetiroId}
                onChange={(event) => setRepresentanteRetiroId(event.target.value)}
                required
              >
                {representantesAutorizados.map((representante) => (
                  <option key={representante.id} value={representante.id}>
                    {representante.nombreCompleto} · {representante.cedula}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <>
              <label>
                Nombre
                <input
                  value={datosSolicitante.nombre}
                  onChange={(event) =>
                    setDatosSolicitante((actual) => ({
                      ...actual,
                      nombre: event.target.value,
                    }))
                  }
                  maxLength={100}
                  required
                />
              </label>
              <label>
                Apellido
                <input
                  value={datosSolicitante.apellido}
                  onChange={(event) =>
                    setDatosSolicitante((actual) => ({
                      ...actual,
                      apellido: event.target.value,
                    }))
                  }
                  maxLength={100}
                  required
                />
              </label>
              <label>
                Cédula
                <input
                  value={datosSolicitante.cedula}
                  onChange={(event) =>
                    setDatosSolicitante((actual) => ({
                      ...actual,
                      cedula: event.target.value,
                    }))
                  }
                  maxLength={20}
                  required
                />
              </label>
            </>
          )}
          <label>
            Fecha de retiro
            <input
              type="date"
              value={datosSolicitante.fechaRetiro}
              max={fechaLocalISO()}
              onChange={(event) =>
                setDatosSolicitante((actual) => ({
                  ...actual,
                  fechaRetiro: event.target.value,
                }))
              }
              required
            />
          </label>
          <label>
            Motivo
            <textarea
              value={datosSolicitante.motivo}
              onChange={(event) =>
                setDatosSolicitante((actual) => ({
                  ...actual,
                  motivo: event.target.value,
                }))
              }
              maxLength={500}
              required
            />
          </label>
        </ModalFormulario>
      )}
    </div>
  );
}

export default Matricula;
