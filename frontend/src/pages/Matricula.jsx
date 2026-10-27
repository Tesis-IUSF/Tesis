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
  { clave: "todos", etiqueta: "Todos", estado: null },
  { clave: "preinscritos", etiqueta: "Preinscritos", estado: "preinscrito" },
  { clave: "proceso", etiqueta: "En proceso", estado: "en_proceso" },
  { clave: "completadas", etiqueta: "Completadas", estado: "completada" },
  { clave: "retiradas", etiqueta: "Retiradas", estado: "retirada" },
  { clave: "graduados", etiqueta: "Posibles graduados", graduados: true },
];

const ESTADOS_NOMINA = [
  { valor: "preinscrito", etiqueta: "Preinscritos" },
  { valor: "en_proceso", etiqueta: "En proceso" },
  { valor: "completada", etiqueta: "Completadas" },
  { valor: "retirada", etiqueta: "Retiradas" },
  { valor: "anulada", etiqueta: "Anuladas" },
];

const COLUMNAS_NOMINA = [
  { valor: "nombre", etiqueta: "Nombre y apellido" },
  { valor: "cedula", etiqueta: "Cédula" },
  { valor: "fechaNacimiento", etiqueta: "Fecha de nacimiento" },
  { valor: "edad", etiqueta: "Edad calculada" },
  { valor: "sexo", etiqueta: "Sexo" },
  { valor: "nivelGradoSeccion", etiqueta: "Nivel, grado y sección" },
  { valor: "tipoIngreso", etiqueta: "Tipo de ingreso" },
  { valor: "representanteNombre", etiqueta: "Nombre del representante principal" },
  { valor: "representanteCedula", etiqueta: "Cédula del representante" },
  { valor: "representanteTelefono", etiqueta: "Teléfono del representante" },
];

const VALORES_NOMINA_PREDETERMINADOS = {
  estados: ESTADOS_NOMINA.map((estado) => estado.valor),
  columnas: COLUMNAS_NOMINA.map((columna) => columna.valor),
};

const MENSAJE_VACIO = {
  todos: "No hay matrículas en el año escolar activo.",
  preinscritos: "No hay preinscripciones pendientes.",
  proceso: "No hay inscripciones en proceso.",
  completadas: "No hay matrículas completadas.",
  retiradas: "No hay matrículas retiradas.",
};

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

  const [tab, setTab] = useState("todos");
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
  const [anioActivoNumero, setAnioActivoNumero] = useState("");
  const [secciones, setSecciones] = useState([]);
  const [accionId, setAccionId] = useState(null);
  const [descargandoEstadisticas, setDescargandoEstadisticas] = useState(false);
  const [nominaModal, setNominaModal] = useState(false);
  const [cargandoCatalogosNomina, setCargandoCatalogosNomina] = useState(false);
  const [descargandoNomina, setDescargandoNomina] = useState(false);
  const [catalogosNomina, setCatalogosNomina] = useState({
    anios: [],
    niveles: [],
    grados: [],
    secciones: [],
  });
  const [filtrosNomina, setFiltrosNomina] = useState({
    anioEscolar: "",
    nivelId: "",
    gradoId: "",
    seccionId: "",
    ...VALORES_NOMINA_PREDETERMINADOS,
  });
  const [retiroMatricula, setRetiroMatricula] = useState(null);
  const [representantesAutorizados, setRepresentantesAutorizados] = useState(
    [],
  );
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

  const tabActual = TABS.find((t) => t.clave === tab) ?? TABS[0];
  // La barra de requisitos solo tiene sentido mientras se completa la inscripción
  const mostrarRequisitos = tabActual.clave === "proceso";

  // Catálogos: calendario (año activo) y secciones (para nombrar la sección)
  useEffect(() => {
    let vigente = true;
    Promise.allSettled([
      api.get("/catalogos/secciones", { params: { page: 0, size: 100 } }),
      api.get("/matriculas/calendario"),
    ]).then(([respuestaSecciones, calendario]) => {
      if (!vigente) return;
      const errores = [];
      if (respuestaSecciones.status === "fulfilled") {
        setSecciones(respuestaSecciones.value.data.content);
      } else {
        errores.push("secciones");
      }
      if (calendario.status === "fulfilled") {
        const activo = calendario.value.data.anioActivo;
        setAnioActivoNumero(activo?.anio ? String(activo.anio) : "");
      } else {
        errores.push("año escolar activo");
      }
      if (errores.length > 0) {
        setErrorCatalogos(`No se pudieron cargar: ${errores.join(", ")}.`);
      }
      setCatalogosCargados(true);
    });
    return () => {
      vigente = false;
    };
  }, []);

  // Conteos de las pestañas (del año escolar activo)
  useEffect(() => {
    if (!catalogosCargados) return undefined;
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
              ...(!t.graduados && anioActivoNumero
                ? { anioEscolar: anioActivoNumero }
                : {}),
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
  }, [catalogosCargados, anioActivoNumero, recargarConteos]);

  const nombreSeccion = useMemo(() => {
    const mapa = {};
    secciones.forEach((s) => {
      mapa[s.id] = `${s.gradoNombre ?? ""} · Sección ${s.letraSeccion}`;
    });
    return (id) => mapa[id] ?? "—";
  }, [secciones]);

  // Listado de la pestaña actual
  useEffect(() => {
    if (!catalogosCargados) return undefined;
    let vigente = true;
    const params = { page: paginaSolicitada, size: 25 };
    if (busquedaAplicada) params.q = busquedaAplicada;
    if (!tabActual.graduados) {
      // En "Todos" el estado es null y no se envía el parámetro
      if (tabActual.estado) params.estado = tabActual.estado;
      if (anioActivoNumero) params.anioEscolar = anioActivoNumero;
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
    anioActivoNumero,
    busquedaAplicada,
    catalogosCargados,
    paginaSolicitada,
    recargar,
    tabActual,
  ]);

  const aplicarBusqueda = (event) => {
    event.preventDefault();
    setCargando(true);
    setError("");
    setBusquedaAplicada(busqueda.trim());
    setPaginaSolicitada(0);
    setRecargar((version) => version + 1);
  };

  const limpiarBusqueda = () => {
    setCargando(true);
    setError("");
    setBusqueda("");
    setBusquedaAplicada("");
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
    if (!datosSolicitante.motivo.trim() || !datosSolicitante.fechaRetiro) {
      toast.error("Completa los datos del solicitante, la fecha y el motivo.");
      return;
    }
    if (datosSolicitante.motivo.length > 500) {
      toast.error("El motivo del retiro no puede superar los 500 caracteres.");
      return;
    }
    if (
      retiroMatricula.fechaFormalizacion &&
      datosSolicitante.fechaRetiro < retiroMatricula.fechaFormalizacion
    ) {
      toast.error(
        "La fecha de retiro no puede ser anterior a la formalización de la matrícula.",
      );
      return;
    }
    if (!camposSolicitanteValidos) {
      toast.error("Completa los datos del solicitante.");
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
      const { data: respuesta } = await api.post(
        `/matriculas/${retiroMatricula.matriculaId}/retiros`,
        request,
      );
      toast.success(
        respuesta?.operacion === "preinscripcion_eliminada"
          ? "Preinscripción eliminada: el estudiante no tenía historial de matrícula."
          : "Retiro registrado correctamente.",
      );
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

  // tipo: "cupo" (preinscritos y en proceso) | "inscripcion" (completadas)
  const descargarConstancia = async (f, tipo) => {
    const ruta =
      tipo === "inscripcion"
        ? "constancia-inscripcion"
        : "constancia-asignacion-cupo";
    const nombre =
      tipo === "inscripcion"
        ? `constancia-inscripcion-${f.matriculaId}.pdf`
        : `constancia-aceptacion-cupo-${f.matriculaId}.pdf`;
    setAccionId(f.matriculaId);
    try {
      const { data } = await api.get(`/matriculas/${f.matriculaId}/${ruta}`, {
        responseType: "blob",
      });
      descargarArchivo(data, nombre);
    } catch (err) {
      toast.error(
        await mensajeErrorBlob(err, "No se pudo descargar la constancia."),
      );
    } finally {
      setAccionId(null);
    }
  };

  const descargarEstadisticas = async () => {
    setDescargandoEstadisticas(true);
    try {
      const { data } = await api.get("/director/estadisticas/pdf", {
        responseType: "blob",
      });
      descargarArchivo(data, "estadisticas-matricula.pdf");
    } catch (err) {
      toast.error(
        await mensajeErrorBlob(
          err,
          "No se pudo descargar el reporte de estadísticas de matrícula.",
        ),
      );
    } finally {
      setDescargandoEstadisticas(false);
    }
  };

  const abrirNominaEstudiantil = async () => {
    setNominaModal(true);
    setCargandoCatalogosNomina(true);
    try {
      const [respuestaAnios, respuestaNiveles, respuestaGrados, respuestaSecciones] =
        await Promise.all([
          api.get("/matriculas/gestion/anios"),
          api.get("/catalogos/niveles-educativos", {
            params: { page: 0, size: 100 },
          }),
          api.get("/catalogos/grados", { params: { page: 0, size: 100 } }),
          api.get("/catalogos/secciones", { params: { page: 0, size: 100 } }),
        ]);
      const anios = respuestaAnios.data;
      setCatalogosNomina({
        anios,
        niveles: respuestaNiveles.data.content,
        grados: respuestaGrados.data.content,
        secciones: respuestaSecciones.data.content,
      });
      setFiltrosNomina({
        anioEscolar:
          String(anios.find((anio) => anio.activo)?.anio ?? anioActivoNumero),
        nivelId: "",
        gradoId: "",
        seccionId: "",
        ...VALORES_NOMINA_PREDETERMINADOS,
      });
    } catch (err) {
      setNominaModal(false);
      toast.error(
        extractErrorMessage(err, "No se pudieron cargar las opciones de la nómina."),
      );
    } finally {
      setCargandoCatalogosNomina(false);
    }
  };

  const cambiarFiltroNomina = (campo, valor) => {
    setFiltrosNomina((actual) => ({
      ...actual,
      [campo]: valor,
      ...(campo === "anioEscolar" ? { gradoId: "", seccionId: "" } : {}),
      ...(campo === "nivelId" ? { gradoId: "", seccionId: "" } : {}),
      ...(campo === "gradoId" ? { seccionId: "" } : {}),
    }));
  };

  const alternarSeleccionNomina = (campo, valor) => {
    setFiltrosNomina((actual) => {
      const seleccion = new Set(actual[campo]);
      if (seleccion.has(valor)) {
        seleccion.delete(valor);
      } else {
        seleccion.add(valor);
      }
      return { ...actual, [campo]: [...seleccion] };
    });
  };

  const gradosNomina = catalogosNomina.grados.filter(
    (grado) =>
      (!filtrosNomina.nivelId ||
        String(grado.nivelEducativoId) === filtrosNomina.nivelId) &&
      catalogosNomina.secciones.some(
        (seccion) =>
          String(seccion.anioEscolar) === filtrosNomina.anioEscolar &&
          String(seccion.gradoId) === String(grado.id),
      ),
  );
  const gradosSeleccionadosPorNivel = catalogosNomina.grados
    .filter(
      (grado) =>
        !filtrosNomina.nivelId ||
        String(grado.nivelEducativoId) === filtrosNomina.nivelId,
    )
    .map((grado) => String(grado.id));
  const seccionesNomina = catalogosNomina.secciones.filter(
    (seccion) =>
      String(seccion.anioEscolar) === filtrosNomina.anioEscolar &&
      gradosSeleccionadosPorNivel.includes(String(seccion.gradoId)) &&
      (!filtrosNomina.gradoId ||
        String(seccion.gradoId) === filtrosNomina.gradoId),
  );

  const descargarNominaEstudiantil = async (event) => {
    event.preventDefault();
    if (!filtrosNomina.anioEscolar) {
      toast.error("Selecciona un año escolar para generar la nómina.");
      return;
    }
    if (filtrosNomina.estados.length === 0) {
      toast.error("Selecciona al menos un estado de matrícula.");
      return;
    }
    if (filtrosNomina.columnas.length === 0) {
      toast.error("Selecciona al menos una columna para la nómina.");
      return;
    }
    const parametros = new URLSearchParams();
    parametros.set("anioEscolar", filtrosNomina.anioEscolar);
    filtrosNomina.estados.forEach((estado) => parametros.append("estados", estado));
    filtrosNomina.columnas.forEach((columna) =>
      parametros.append("columnas", columna),
    );
    if (filtrosNomina.nivelId) {
      parametros.set("nivelId", filtrosNomina.nivelId);
    }
    if (filtrosNomina.gradoId) {
      parametros.set("gradoId", filtrosNomina.gradoId);
    }
    if (filtrosNomina.seccionId) {
      parametros.set("seccionId", filtrosNomina.seccionId);
    }

    setDescargandoNomina(true);
    try {
      const { data } = await api.get(
        `/matriculas/nomina-estudiantil/pdf?${parametros.toString()}`,
        { responseType: "blob" },
      );
      descargarArchivo(data, `nomina-estudiantil-${filtrosNomina.anioEscolar}.pdf`);
      setNominaModal(false);
    } catch (err) {
      toast.error(
        await mensajeErrorBlob(err, "No se pudo descargar la nómina estudiantil."),
      );
    } finally {
      setDescargandoNomina(false);
    }
  };

  // Acciones de cada fila según la pestaña
  const renderAcciones = (f, ocupada) => {
    const ruta = `/matricula/${f.matriculaId}/inscripcion`;
    const botonConstancia = (tipo) => (
      <button
        type="button"
        className="btn-reactivar"
        disabled={ocupada}
        onClick={() => descargarConstancia(f, tipo)}
      >
        Constancia
      </button>
    );
    // Solo el rol con permiso de retiros ve el botón (el backend también lo valida)
    const botonRetirar = permitirRetiros ? (
      <button
        type="button"
        className="btn-delete"
        disabled={ocupada}
        onClick={() => abrirRetiro(f)}
      >
        {ocupada ? "Cargando..." : "Retirar"}
      </button>
    ) : null;

    switch (tabActual.clave) {
      case "preinscritos":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              {soloConsulta ? "Ver" : "Inscribir"}
            </Link>
            {botonConstancia("cupo")}
            {botonRetirar}
          </div>
        );
      case "proceso":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              {soloConsulta ? "Ver" : "Continuar"}
            </Link>
            {botonConstancia("cupo")}
            {botonRetirar}
          </div>
        );
      case "completadas":
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              Ver
            </Link>
            {botonConstancia("inscripcion")}
          </div>
        );
      default:
        // Todos, Retiradas y Posibles graduados
        return (
          <div className="mat-acciones">
            <Link to={ruta} className="btn-edit">
              Ver
            </Link>
          </div>
        );
    }
  };

  const columnas = mostrarRequisitos ? 6 : 5;

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>Matrícula</h1>
        {soloConsulta ? (
          <div className="mod-heading-actions">
            <button
              type="button"
              className="btn-secondary"
              disabled={descargandoNomina || cargandoCatalogosNomina}
              onClick={abrirNominaEstudiantil}
            >
              <Icon icon={ICONOS.descargar} className="icono-inline" />{" "}
              Nómina estudiantil
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={descargandoEstadisticas}
              onClick={descargarEstadisticas}
            >
              <Icon icon={ICONOS.descargar} className="icono-inline" />{" "}
              {descargandoEstadisticas
                ? "Descargando reporte..."
                : "Descargar estadísticas"}
            </button>
          </div>
        ) : (
          <div className="mod-heading-actions">
            <Link to="/matricula/preinscripcion" className="btn-primary">
              + Nueva preinscripción
            </Link>
          </div>
        )}
      </div>

      <form className="mat-filtros" onSubmit={aplicarBusqueda}>
        <label>
          Buscar estudiante
          <input
            type="search"
            value={busqueda}
            onChange={(event) => setBusqueda(event.target.value)}
            placeholder="Nombre completo o cédula"
          />
        </label>
        <div className="mat-filtros-acciones">
          <button type="submit" className="btn-primary">
            Buscar
          </button>
          <button
            type="button"
            className="btn-secondary"
            onClick={limpiarBusqueda}
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
                <td colSpan={columnas} className="tabla-estado">
                  Cargando matrículas...
                </td>
              </tr>
            ) : filas.length === 0 ? (
              <tr>
                <td colSpan={columnas} className="tabla-estado">
                  <span className="tabla-estado-icono">
                    <Icon icon={ICONOS.matricula} />
                  </span>
                  {busquedaAplicada
                    ? "No hay resultados para esa búsqueda."
                    : tabActual.graduados
                      ? "No hay posibles graduados."
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
            Matrícula de{" "}
            <strong>{retiroMatricula.estudianteNombreCompleto}</strong>
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
                onChange={(event) =>
                  setRepresentanteRetiroId(event.target.value)
                }
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
              min={retiroMatricula.fechaFormalizacion || undefined}
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
      {nominaModal && (
        <ModalFormulario
          titulo="Generar nómina estudiantil"
          className="mat-nomina-modal"
          onCerrar={() => setNominaModal(false)}
          onSubmit={descargarNominaEstudiantil}
          guardando={descargandoNomina || cargandoCatalogosNomina}
          etiquetaGuardar="Descargar PDF"
        >
          {cargandoCatalogosNomina ? (
            <p>Cargando opciones de matrícula...</p>
          ) : (
            <>
              <p>
                El reporte usa estas opciones y no aplica la búsqueda ni los
                filtros de la pantalla.
              </p>
              <label>
                Año escolar
                <select
                  value={filtrosNomina.anioEscolar}
                  onChange={(event) =>
                    cambiarFiltroNomina("anioEscolar", event.target.value)
                  }
                  required
                >
                  <option value="">Selecciona un año escolar</option>
                  {catalogosNomina.anios.map((anio) => (
                    <option key={anio.anio} value={anio.anio}>
                      {anio.nombre}
                      {anio.activo ? " · Activo" : ""}
                    </option>
                  ))}
                </select>
              </label>
              <fieldset className="mat-nomina-opciones">
                <legend>Estados de matrícula</legend>
                {ESTADOS_NOMINA.map((estado) => (
                  <label key={estado.valor} className="mat-nomina-check">
                    <input
                      type="checkbox"
                      checked={filtrosNomina.estados.includes(estado.valor)}
                      onChange={() =>
                        alternarSeleccionNomina("estados", estado.valor)
                      }
                    />
                    {estado.etiqueta}
                  </label>
                ))}
              </fieldset>
              <div className="mat-nomina-filtros-academicos">
                <label>
                  Nivel educativo
                  <select
                    value={filtrosNomina.nivelId}
                    onChange={(event) =>
                      cambiarFiltroNomina("nivelId", event.target.value)
                    }
                  >
                    <option value="">Todos los niveles</option>
                    {catalogosNomina.niveles.map((nivel) => (
                      <option key={nivel.id} value={nivel.id}>
                        {nivel.nombre}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Grado
                  <select
                    value={filtrosNomina.gradoId}
                    onChange={(event) =>
                      cambiarFiltroNomina("gradoId", event.target.value)
                    }
                    disabled={!filtrosNomina.anioEscolar}
                  >
                    <option value="">Todos los grados</option>
                    {gradosNomina.map((grado) => (
                      <option key={grado.id} value={grado.id}>
                        {grado.nombreEspecial?.trim() ||
                          `${grado.numeroGrado}° grado`}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Sección
                  <select
                    value={filtrosNomina.seccionId}
                    onChange={(event) =>
                      cambiarFiltroNomina("seccionId", event.target.value)
                    }
                    disabled={!filtrosNomina.anioEscolar}
                  >
                    <option value="">Todas las secciones</option>
                    {seccionesNomina.map((seccion) => (
                      <option key={seccion.id} value={seccion.id}>
                        {seccion.gradoNombre} · Sección {seccion.letraSeccion}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <fieldset className="mat-nomina-opciones">
                <legend>Columnas del reporte</legend>
                {COLUMNAS_NOMINA.map((columna) => (
                  <label key={columna.valor} className="mat-nomina-check">
                    <input
                      type="checkbox"
                      checked={filtrosNomina.columnas.includes(columna.valor)}
                      onChange={() =>
                        alternarSeleccionNomina("columnas", columna.valor)
                      }
                    />
                    {columna.etiqueta}
                  </label>
                ))}
              </fieldset>
            </>
          )}
        </ModalFormulario>
      )}
    </div>
  );
}

export default Matricula;
