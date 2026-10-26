// src/pages/Preinscripcion.jsx
import { useState, useEffect, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import { subirFoto } from "../utils/fotos";
import FotoCarnet from "../components/FotoCarnet";
import "./Preinscripcion.css";

const PARENTESCOS = ["Padre", "Madre", "Tutor", "Abuelo", "Otro"];
const SEXOS = [
  { valor: "M", etiqueta: "Masculino" },
  { valor: "F", etiqueta: "Femenino" },
  { valor: "Otro", etiqueta: "Otro" },
];
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const CEDULA_IDENTIDAD = /^V-(?:[6-9]\d{6}|[1-9]\d{7})$/;
const CEDULA_EXTRANJERA = /^E-[89]\d{7}$/;

const hoyISO = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

// "2026-09-01" -> "01/09/2026" sin pasar por Date (evita desfase de zona horaria)
const formatFechaISO = (v) => {
  if (!v) return "—";
  const [a, m, d] = String(v).split("-");
  return `${d}/${m}/${a}`;
};

const VACIO = {
  est: {
    tieneCedulaIdentidad: false,
    cedulaTipo: "V",
    cedulaNumero: "",
    nacionalidad: "V",
    ordenNacimiento: "1",
    nombre: "",
    apellido: "",
    fechaNacimiento: "",
    sexo: "",
    correo: "",
    telefono: "",
    foto: null, // File elegido; se sube al guardar
    fotoCarnetUrl: "", // foto ya guardada
  },
  rep: {
    id: null,
    cedulaTipo: "V",
    cedulaNumero: "",
    nombre: "",
    apellido: "",
    parentesco: "",
    correo: "",
    telefono: "",
    telefonoSecundario: "",
    direccion: "",
    ocupacion: "",
    foto: null, // File elegido; se sube al guardar
    fotoUrl: "", // foto ya guardada
  },
  institucionProcedencia: "",
  anioEscolar: null,
  seccionId: "",
  relacionPrincipal: true,
  autorizadoRetirar: true,
  recibeComunicados: true,
};

const o = (v) => (v.trim() ? v.trim() : null);
const serializarEstable = (valor) =>
  JSON.stringify(valor, (clave, dato) =>
    dato && typeof dato === "object" && !Array.isArray(dato)
      ? Object.fromEntries(
          Object.entries(dato).sort(([a], [b]) => a.localeCompare(b)),
        )
      : dato,
  );

// urls: URL de fotos recién subidas { fotoCarnetUrl, fotoUrl } (opcional)
const crearPayloadPreinscripcion = (formulario, id, anio, urls = {}) => {
  const { est, rep } = formulario;
  return {
    estudiante: {
      cedula: est.tieneCedulaIdentidad
        ? `${est.cedulaTipo}-${est.cedulaNumero.trim()}`.toUpperCase()
        : id && String(est.cedulaActual ?? "").startsWith("CE-")
          ? est.cedulaActual
          : null,
      tieneCedulaIdentidad: est.tieneCedulaIdentidad,
      nacionalidad: est.nacionalidad,
      ordenNacimiento: est.ordenNacimiento ? Number(est.ordenNacimiento) : null,
      nombre: est.nombre.trim(),
      apellido: est.apellido.trim(),
      fechaNacimiento: est.fechaNacimiento,
      sexo: est.sexo,
      correo: o(est.correo),
      telefono: o(est.telefono),
      fotoCarnetUrl: urls.fotoCarnetUrl ?? (est.fotoCarnetUrl || undefined),
    },
    representante: {
      id: rep.id,
      cedula: `${rep.cedulaTipo}-${rep.cedulaNumero.trim()}`.toUpperCase(),
      nombre: rep.nombre.trim(),
      apellido: rep.apellido.trim(),
      parentesco: rep.parentesco,
      correo: rep.correo.trim(),
      telefono: o(rep.telefono),
      telefonoSecundario: o(rep.telefonoSecundario),
      direccion: o(rep.direccion),
      ocupacion: o(rep.ocupacion),
      fotoUrl: urls.fotoUrl ?? (rep.fotoUrl || undefined),
    },
    seccionId: Number(formulario.seccionId),
    anioEscolar: Number(id ? formulario.anioEscolar : anio),
    institucionProcedencia: o(formulario.institucionProcedencia),
    relacionPrincipal: formulario.relacionPrincipal,
    autorizadoRetirar: formulario.autorizadoRetirar,
    recibeComunicados: formulario.recibeComunicados,
  };
};

const separarCedulaIdentidad = (cedula = "") => {
  const coincidencia = String(cedula)
    .trim()
    .toUpperCase()
    .replace(/[.\s]/g, "")
    .match(/^([VE])-(\d+)$/);
  return coincidencia
    ? { cedulaTipo: coincidencia[1], cedulaNumero: coincidencia[2] }
    : { cedulaTipo: "V", cedulaNumero: "" };
};

const separarCedulaRepresentante = (cedula = "") => {
  const coincidencia = String(cedula)
    .trim()
    .toUpperCase()
    .replace(/[.\s]/g, "")
    .match(/^([VE])-(\d+)$/);
  return coincidencia
    ? { cedulaTipo: coincidencia[1], cedulaNumero: coincidencia[2] }
    : { cedulaTipo: "V", cedulaNumero: "" };
};

function Preinscripcion({
  matriculaId,
  embedded = false,
  formId,
  onCancel,
  onSaved,
  onSubmitting,
}) {
  const toast = useToast();
  const navigate = useNavigate();
  const { id: routeId } = useParams();
  const id = matriculaId ?? routeId;
  const [form, setForm] = useState(VACIO);
  const [formOriginal, setFormOriginal] = useState(null);
  const [errores, setErrores] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState("");
  const [modoRepresentante, setModoRepresentante] = useState(
    id ? "seleccionado" : "buscar",
  );
  const [busquedaRepresentante, setBusquedaRepresentante] = useState("");
  const [representantesEncontrados, setRepresentantesEncontrados] = useState(
    [],
  );
  const [paginaRepresentantes, setPaginaRepresentantes] = useState({
    page: 0,
    totalPages: 0,
  });
  const [buscandoRepresentantes, setBuscandoRepresentantes] = useState(false);
  const [idEdicionCargada, setIdEdicionCargada] = useState(null);
  const cargandoEdicion = Boolean(id) && idEdicionCargada !== id;

  const [calendario, setCalendario] = useState(null);
  const [secciones, setSecciones] = useState([]);
  const [cargandoDatos, setCargandoDatos] = useState(true);
  const [errorDatos, setErrorDatos] = useState("");
  const [modalSeccionAbierto, setModalSeccionAbierto] = useState(false);
  const [gradoModalId, setGradoModalId] = useState("");
  const [seccionModalId, setSeccionModalId] = useState("");

  useEffect(() => {
    let activo = true;
    Promise.allSettled([
      api.get("/matriculas/calendario"),
      api.get("/catalogos/secciones", { params: { page: 0, size: 100 } }),
    ]).then(([cal, sec]) => {
      if (!activo) return;
      if (cal.status === "fulfilled") setCalendario(cal.value.data);
      if (sec.status === "fulfilled") setSecciones(sec.value.data.content);
      if (cal.status === "rejected" || sec.status === "rejected") {
        setErrorDatos(
          "No se pudieron cargar el calendario o las secciones. Recarga la página.",
        );
      }
      setCargandoDatos(false);
    });
    return () => {
      activo = false;
    };
  }, []);

  useEffect(() => {
    if (!id) return undefined;
    let activo = true;
    api
      .get(`/matriculas/${id}/preinscripcion`)
      .then(({ data }) => {
        if (!activo) return;
        const { estudiante, representante } = data;
        const tieneCedulaIdentidad = !String(estudiante.cedula ?? "")
          .toUpperCase()
          .startsWith("CE-");
        const cedulaIdentidad = separarCedulaIdentidad(estudiante.cedula);
        const formularioCargado = {
          est: {
            ...VACIO.est,
            tieneCedulaIdentidad,
            ...cedulaIdentidad,
            cedulaActual: estudiante.cedula ?? "",
            nombre: estudiante.nombre ?? "",
            apellido: estudiante.apellido ?? "",
            fechaNacimiento: estudiante.fechaNacimiento ?? "",
            sexo: estudiante.sexo ?? "",
            correo: estudiante.correo ?? "",
            telefono: estudiante.telefono ?? "",
            fotoCarnetUrl: estudiante.fotoCarnetUrl ?? "",
          },
          rep: {
            ...VACIO.rep,
            ...representante,
            ...separarCedulaRepresentante(representante.cedula),
            foto: null,
            fotoUrl: representante.fotoUrl ?? "",
          },
          institucionProcedencia: data.institucionProcedencia ?? "",
          anioEscolar: data.anioEscolar ?? null,
          seccionId: String(data.seccionId ?? ""),
          relacionPrincipal: data.relacionPrincipal ?? true,
          autorizadoRetirar: data.autorizadoRetirar ?? true,
          recibeComunicados: data.recibeComunicados ?? true,
        };
        setForm(formularioCargado);
        setFormOriginal(JSON.stringify(formularioCargado));
      })
      .catch((error) => {
        if (!activo) return;
        setErrorDatos(
          extractErrorMessage(
            error,
            "No se pudieron cargar los datos para editar.",
          ),
        );
      })
      .finally(() => {
        if (activo) setIdEdicionCargada(id);
      });
    return () => {
      activo = false;
    };
  }, [id]);

  useEffect(() => {
    if (
      modoRepresentante !== "buscar" ||
      busquedaRepresentante.trim().length < 2
    ) {
      return undefined;
    }
    let activo = true;
    const temporizador = setTimeout(() => {
      setBuscandoRepresentantes(true);
      api
        .get("/representantes/buscar", {
          params: {
            q: busquedaRepresentante.trim(),
            page: paginaRepresentantes.page,
            size: 8,
          },
        })
        .then(({ data }) => {
          if (!activo) return;
          setRepresentantesEncontrados(data.content ?? []);
          setPaginaRepresentantes({
            page: data.page ?? 0,
            totalPages: data.totalPages ?? 0,
          });
        })
        .catch((error) => {
          if (!activo) return;
          setRepresentantesEncontrados([]);
          setErrorEnvio(
            extractErrorMessage(error, "No se pudo buscar representantes."),
          );
        })
        .finally(() => {
          if (activo) setBuscandoRepresentantes(false);
        });
    }, 250);
    return () => {
      activo = false;
      clearTimeout(temporizador);
    };
  }, [modoRepresentante, busquedaRepresentante, paginaRepresentantes.page]);

  const anio = id
    ? (form.anioEscolar ?? calendario?.anioActivo?.anio ?? null)
    : (calendario?.anioActivo?.anio ?? null);
  const anioNombre =
    id && form.anioEscolar
      ? String(form.anioEscolar)
      : (calendario?.anioActivo?.nombre ?? (anio ? String(anio) : "—"));

  const periodoVigente = useMemo(
    () =>
      (calendario?.periodos ?? []).find(
        (p) => p.vigente && String(p.tipo).toLowerCase().includes("preinscri"),
      ) ?? null,
    [calendario],
  );

  // Secciones activas del año escolar, agrupadas por grado
  const grupos = useMemo(() => {
    if (anio == null) return [];
    const lista = secciones
      .filter((s) => s.activo && Number(s.anioEscolar) === Number(anio))
      .sort(
        (a, b) =>
          (a.gradoNombre ?? "").localeCompare(b.gradoNombre ?? "", "es", {
            numeric: true,
          }) || (a.letraSeccion ?? "").localeCompare(b.letraSeccion ?? ""),
      );
    const mapa = new Map();
    lista.forEach((s) => {
      const clave = String(s.gradoId);
      if (!mapa.has(clave)) {
        mapa.set(clave, {
          nombre: s.gradoNombre ?? "Sin grado",
          secciones: [],
        });
      }
      mapa.get(clave).secciones.push(s);
    });
    return [...mapa.entries()];
  }, [secciones, anio]);

  const seccionElegida = useMemo(
    () => secciones.find((s) => String(s.id) === String(form.seccionId)),
    [secciones, form.seccionId],
  );

  const seccionesModal = useMemo(
    () =>
      secciones.filter(
        (seccion) =>
          seccion.activo &&
          Number(seccion.anioEscolar) === Number(anio) &&
          String(seccion.gradoId) === String(gradoModalId),
      ),
    [secciones, anio, gradoModalId],
  );

  const abrirModalSeccion = () => {
    setGradoModalId(
      seccionElegida?.gradoId?.toString() ?? grupos[0]?.[0] ?? "",
    );
    setSeccionModalId(form.seccionId);
    setModalSeccionAbierto(true);
  };

  const guardarSeccionModal = () => {
    setRaiz("seccionId", seccionModalId);
    setModalSeccionAbierto(false);
  };

  // ---------- Edición de campos ----------
  const setEst = (campo, valor) => {
    setForm((p) => ({ ...p, est: { ...p.est, [campo]: valor } }));
    setErrores((p) => ({ ...p, [`est.${campo}`]: undefined }));
  };
  const setRep = (campo, valor) => {
    setForm((p) => ({ ...p, rep: { ...p.rep, [campo]: valor } }));
    setErrores((p) => ({ ...p, [`rep.${campo}`]: undefined }));
  };
  const setRaiz = (campo, valor) => {
    setForm((p) => ({ ...p, [campo]: valor }));
    setErrores((p) => ({ ...p, [campo]: undefined }));
  };

  const err = (clave) =>
    errores[clave] ? (
      <span className="campo-error">{errores[clave]}</span>
    ) : null;

  // ---------- Validación por paso ----------
  const validarPaso = (n) => {
    const e = {};
    const { est, rep } = form;
    if (n === 0) {
      if (est.tieneCedulaIdentidad) {
        const cedula =
          `${est.cedulaTipo}-${est.cedulaNumero.trim()}`.toUpperCase();
        const valida =
          est.cedulaTipo === "E"
            ? CEDULA_EXTRANJERA.test(cedula)
            : CEDULA_IDENTIDAD.test(cedula);
        if (!est.cedulaNumero.trim())
          e["est.cedula"] = "La cédula de identidad es obligatoria.";
        else if (!valida)
          e["est.cedula"] =
            "Formato inválido o fuera de rango. Usa V-6000000 o E-80000000 como mínimo.";
      } else {
        if (!est.nacionalidad)
          e["est.nacionalidad"] = "Selecciona la nacionalidad.";
        if (
          !est.ordenNacimiento ||
          !Number.isInteger(Number(est.ordenNacimiento)) ||
          Number(est.ordenNacimiento) < 1 ||
          Number(est.ordenNacimiento) > 9
        )
          e["est.ordenNacimiento"] =
            "El orden de nacimiento debe estar entre 1 y 9.";
      }
      if (!est.nombre.trim()) e["est.nombre"] = "El nombre es obligatorio.";
      if (!est.apellido.trim())
        e["est.apellido"] = "El apellido es obligatorio.";
      if (!est.fechaNacimiento)
        e["est.fechaNacimiento"] = "La fecha de nacimiento es obligatoria.";
      else if (est.fechaNacimiento >= hoyISO())
        e["est.fechaNacimiento"] = "Debe ser una fecha pasada.";
      if (!est.sexo) e["est.sexo"] = "Selecciona el sexo.";
      if (est.correo.trim() && !EMAIL.test(est.correo.trim()))
        e["est.correo"] = "Correo no válido.";
      if (est.telefono.trim().length > 20)
        e["est.telefono"] = "Máximo 20 caracteres.";
      if (form.institucionProcedencia.length > 150)
        e.institucionProcedencia = "Máximo 150 caracteres.";
    }
    if (n === 1) {
      if (modoRepresentante === "buscar") {
        e["rep.seleccion"] =
          "Selecciona un representante o registra uno nuevo.";
      } else {
        const cedula =
          `${rep.cedulaTipo}-${rep.cedulaNumero.trim()}`.toUpperCase();
        const cedulaValida =
          rep.cedulaTipo === "E"
            ? CEDULA_EXTRANJERA.test(cedula)
            : CEDULA_IDENTIDAD.test(cedula);
        if (!rep.cedulaNumero.trim())
          e["rep.cedula"] = "La cédula es obligatoria.";
        else if (!cedulaValida)
          e["rep.cedula"] =
            "Formato inválido o fuera de rango para el prefijo seleccionado.";
        if (!rep.nombre.trim()) e["rep.nombre"] = "El nombre es obligatorio.";
        if (!rep.apellido.trim())
          e["rep.apellido"] = "El apellido es obligatorio.";
        if (!rep.parentesco) e["rep.parentesco"] = "Selecciona el parentesco.";
        if (!rep.correo.trim()) e["rep.correo"] = "El correo es obligatorio.";
        else if (!EMAIL.test(rep.correo.trim()))
          e["rep.correo"] = "Correo no válido.";
        if (!rep.telefono.trim())
          e["rep.telefono"] = "El teléfono principal es obligatorio.";
        else if (rep.telefono.trim().length > 20)
          e["rep.telefono"] = "Máximo 20 caracteres.";
        if (rep.telefonoSecundario.trim().length > 20)
          e["rep.telefonoSecundario"] = "Máximo 20 caracteres.";
      }
    }
    if (n === 2) {
      if (!form.seccionId) e.seccionId = "Selecciona una sección.";
    }
    setErrores((p) => ({ ...p, ...e }));
    return Object.keys(e).length === 0;
  };

  // ---------- Envío ----------
  const enviar = async () => {
    const hayFotoNueva = Boolean(form.est.foto || form.rep.foto);
    const payloadActual = crearPayloadPreinscripcion(form, id, anio);
    if (
      id &&
      formOriginal &&
      !hayFotoNueva &&
      serializarEstable(payloadActual) ===
        serializarEstable(
          crearPayloadPreinscripcion(JSON.parse(formOriginal), id, anio),
        )
    ) {
      if (embedded && onSaved) {
        await onSaved({ fichaModificada: false });
      } else {
        toast.info("No se realizaron cambios.");
        if (onCancel) onCancel();
        else navigate(`/matricula/${id}/inscripcion`);
      }
      return;
    }

    const estudianteValido = validarPaso(0);
    const representanteValido = validarPaso(1);
    const seccionValida = validarPaso(2);
    if (!estudianteValido || !representanteValido || !seccionValida) {
      return;
    }
    if (anio == null) {
      setErrorEnvio(
        "No hay un año escolar activo en el calendario de matrícula.",
      );
      return;
    }
    setEnviando(true);
    onSubmitting?.(true);
    setErrorEnvio("");
    try {
      // Las fotos nuevas se suben primero; las ya guardadas se conservan
      const urls = {};
      if (form.est.foto) urls.fotoCarnetUrl = await subirFoto(form.est.foto);
      if (form.rep.foto) urls.fotoUrl = await subirFoto(form.rep.foto);
      const payload = crearPayloadPreinscripcion(form, id, anio, urls);

      const { data } = id
        ? await api.put(`/matriculas/${id}/preinscripcion`, payload)
        : await api.post("/matriculas/preinscripciones", payload);
      if (id) {
        if (embedded && onSaved) {
          await onSaved({ fichaModificada: true, respuesta: data });
        } else {
          toast.success("Ficha de matrícula actualizada correctamente.");
          navigate(`/matricula/${id}/inscripcion`);
        }
        return;
      }
      toast.success("Preinscripción registrada correctamente.");
      navigate("/matricula");
    } catch (error) {
      // 400 edad/año/foto, 409 cédula, cupo o matrícula duplicada: mensaje del backend
      const mensaje = extractErrorMessage(
        error,
        "No se pudo registrar la preinscripción.",
      );
      setErrorEnvio(mensaje);
      toast.error(mensaje);
    } finally {
      setEnviando(false);
      onSubmitting?.(false);
    }
  };

  const seleccionarRepresentante = (representante) => {
    setForm((actual) => ({
      ...actual,
      rep: {
        ...actual.rep,
        ...representante,
        id: representante.id,
        ...separarCedulaRepresentante(representante.cedula),
        nombre: representante.nombre ?? "",
        apellido: representante.apellido ?? "",
        parentesco: representante.parentesco ?? "",
        correo: representante.correo ?? "",
        telefono: representante.telefono ?? "",
        telefonoSecundario: representante.telefonoSecundario ?? "",
        direccion: representante.direccion ?? "",
        ocupacion: representante.ocupacion ?? "",
        foto: null,
        fotoUrl: representante.fotoUrl ?? "",
      },
    }));
    setErrores({});
    setModoRepresentante("seleccionado");
    setErrorEnvio("");
  };

  const registrarNuevoRepresentante = () => {
    setForm((actual) => ({ ...actual, rep: { ...VACIO.rep } }));
    setModoRepresentante("nuevo");
    setBusquedaRepresentante("");
    setErrores({});
    setErrorEnvio("");
  };

  const buscarOtroRepresentante = () => {
    setForm((actual) => ({ ...actual, rep: { ...VACIO.rep } }));
    setModoRepresentante("buscar");
    setBusquedaRepresentante("");
    setErrores({});
    setErrorEnvio("");
  };

  if (cargandoEdicion) {
    return embedded ? (
      <p className="wiz-info">Cargando ficha...</p>
    ) : (
      <div className="mod-main">
        <p>Cargando ficha...</p>
      </div>
    );
  }

  const { est, rep } = form;
  const conservaCiEscolar =
    Boolean(id) &&
    !est.tieneCedulaIdentidad &&
    String(est.cedulaActual ?? "")
      .toUpperCase()
      .startsWith("CE-");

  return (
    <div className={embedded ? "wiz-inline" : "mod-main"}>
      {!embedded && (
        <div className="mod-heading">
          <h1>{id ? "Editar ficha de matrícula" : "Crear matrícula"}</h1>
        </div>
      )}

      <form
        id={formId}
        className="empleado-form wiz wiz-form"
        onSubmit={(event) => {
          event.preventDefault();
          void enviar();
        }}
        noValidate
      >
        {errorDatos && <p className="error-msg">{errorDatos}</p>}
        {!cargandoDatos && calendario && !periodoVigente && (
          <p className="wiz-aviso">
            <Icon icon={ICONOS.aviso} className="icono-inline" /> No se encontró
            un periodo de preinscripción vigente en el calendario. El sistema
            podría rechazar el registro.
          </p>
        )}
        {periodoVigente && (
          <p className="wiz-info">
            Periodo de preinscripción vigente hasta el{" "}
            {formatFechaISO(periodoVigente.fechaFin)}.
          </p>
        )}

        <section className="ins-tarjeta wiz-card">
          <h2 className="ins-seccion-titulo">Datos del estudiante</h2>
          <div className="wiz-seccion-actual">
            <div>
              <strong>
                {seccionElegida
                  ? `${seccionElegida.gradoNombre} · Sección ${seccionElegida.letraSeccion}`
                  : "Sección sin seleccionar"}
              </strong>
              <small>Año escolar {anioNombre}</small>
            </div>
            <button
              type="button"
              className="btn-secondary"
              onClick={abrirModalSeccion}
              disabled={grupos.length === 0}
            >
              {seccionElegida ? "Cambiar sección" : "Seleccionar sección"}
            </button>
          </div>
          {err("seccionId")}
          <div className="wiz-con-foto">
            <FotoCarnet
              etiqueta="Foto del estudiante"
              archivo={est.foto}
              urlActual={est.fotoCarnetUrl || null}
              onChange={(f) => setEst("foto", f)}
              disabled={enviando}
            />
            <div className="form-grid-2">
              <label className="wiz-check wiz-identidad-toggle">
                <input
                  type="checkbox"
                  checked={est.tieneCedulaIdentidad}
                  onChange={(e) =>
                    setEst("tieneCedulaIdentidad", e.target.checked)
                  }
                />
                ¿Tiene cédula de identidad?
              </label>
              {est.tieneCedulaIdentidad ? (
                <>
                  <label>
                    Prefijo de cédula *
                    <select
                      value={est.cedulaTipo}
                      onChange={(e) => setEst("cedulaTipo", e.target.value)}
                    >
                      <option value="V">Venezolana (V-)</option>
                      <option value="E">Extranjera (E-)</option>
                    </select>
                  </label>
                  <label>
                    Número de cédula *
                    <input
                      inputMode="numeric"
                      value={est.cedulaNumero}
                      onChange={(e) =>
                        setEst(
                          "cedulaNumero",
                          e.target.value.replace(/\D/g, "").slice(0, 8),
                        )
                      }
                      maxLength={8}
                      placeholder={
                        est.cedulaTipo === "E" ? "80000000" : "6000000"
                      }
                      autoFocus
                    />
                    {err("est.cedula")}
                  </label>
                </>
              ) : (
                <>
                  <label>
                    Nacionalidad para CI escolar *
                    <select
                      value={est.nacionalidad}
                      onChange={(e) => setEst("nacionalidad", e.target.value)}
                    >
                      <option value="V">Venezolano (V)</option>
                      <option value="E">Extranjero (E)</option>
                    </select>
                    {err("est.nacionalidad")}
                  </label>
                  <label>
                    Orden de nacimiento *
                    <input
                      type="number"
                      min="1"
                      max="9"
                      step="1"
                      value={est.ordenNacimiento}
                      onChange={(e) =>
                        setEst("ordenNacimiento", e.target.value)
                      }
                    />
                    {err("est.ordenNacimiento")}
                  </label>
                  {conservaCiEscolar && (
                    <p className="wiz-info wiz-identidad-ayuda">
                      El estudiante conserva su CI escolar. Marca la opción
                      anterior para sustituirla por una cédula real.
                    </p>
                  )}
                </>
              )}
              <label>
                Fecha de nacimiento *
                <input
                  type="date"
                  max={hoyISO()}
                  value={est.fechaNacimiento}
                  onChange={(e) => setEst("fechaNacimiento", e.target.value)}
                />
                {err("est.fechaNacimiento")}
              </label>
              <label>
                Nombres *
                <input
                  value={est.nombre}
                  onChange={(e) => setEst("nombre", e.target.value)}
                  maxLength={100}
                />
                {err("est.nombre")}
              </label>
              <label>
                Apellidos *
                <input
                  value={est.apellido}
                  onChange={(e) => setEst("apellido", e.target.value)}
                  maxLength={100}
                />
                {err("est.apellido")}
              </label>
              <label>
                Sexo *
                <select
                  value={est.sexo}
                  onChange={(e) => setEst("sexo", e.target.value)}
                >
                  <option value="">Selecciona...</option>
                  {SEXOS.map((s) => (
                    <option key={s.valor} value={s.valor}>
                      {s.etiqueta}
                    </option>
                  ))}
                </select>
                {err("est.sexo")}
              </label>
              <label>
                Teléfono
                <input
                  value={est.telefono}
                  onChange={(e) => setEst("telefono", e.target.value)}
                  maxLength={20}
                  placeholder="04121234567"
                />
                {err("est.telefono")}
              </label>
              <label>
                Correo
                <input
                  type="email"
                  value={est.correo}
                  onChange={(e) => setEst("correo", e.target.value)}
                  placeholder="Opcional"
                />
                {err("est.correo")}
              </label>
              <label>
                Institución de procedencia
                <input
                  value={form.institucionProcedencia}
                  onChange={(e) =>
                    setRaiz("institucionProcedencia", e.target.value)
                  }
                  maxLength={150}
                  placeholder="Solo si viene de otra institución"
                />
                {err("institucionProcedencia")}
              </label>
            </div>
          </div>
        </section>

        <section className="ins-tarjeta wiz-card">
          <h2 className="ins-seccion-titulo">Representantes vinculados</h2>
          <div className="wiz-representante-opciones">
            {modoRepresentante === "buscar" ? (
              <>
                <label className="wiz-ancho">
                  Buscar representante activo por nombre o cédula
                  <input
                    value={busquedaRepresentante}
                    onChange={(e) => {
                      setBusquedaRepresentante(e.target.value);
                      setRepresentantesEncontrados([]);
                      setBuscandoRepresentantes(false);
                      setPaginaRepresentantes({ page: 0, totalPages: 0 });
                      setErrorEnvio("");
                    }}
                    placeholder="Escribe al menos 2 caracteres"
                    autoFocus
                  />
                </label>
                {buscandoRepresentantes && <p>Buscando representantes...</p>}
                {!buscandoRepresentantes &&
                  busquedaRepresentante.trim().length >= 2 &&
                  representantesEncontrados.length === 0 && (
                    <p>No se encontraron representantes activos.</p>
                  )}
                {representantesEncontrados.length > 0 && (
                  <div className="wiz-representantes-resultados" role="list">
                    {representantesEncontrados.map((representante) => (
                      <button
                        key={representante.id}
                        type="button"
                        className="wiz-representante-opcion"
                        onClick={() => seleccionarRepresentante(representante)}
                      >
                        <strong>
                          {representante.nombre} {representante.apellido}
                        </strong>
                        <span>{representante.cedula}</span>
                        <span>{representante.telefono || "Sin teléfono"}</span>
                      </button>
                    ))}
                  </div>
                )}
                {paginaRepresentantes.totalPages > 1 && (
                  <div className="wiz-representantes-paginacion">
                    <button
                      type="button"
                      className="btn-secondary"
                      disabled={
                        paginaRepresentantes.page === 0 ||
                        buscandoRepresentantes
                      }
                      onClick={() =>
                        setPaginaRepresentantes((p) => ({
                          ...p,
                          page: p.page - 1,
                        }))
                      }
                    >
                      Anterior
                    </button>
                    <span>
                      Página {paginaRepresentantes.page + 1} de{" "}
                      {paginaRepresentantes.totalPages}
                    </span>
                    <button
                      type="button"
                      className="btn-secondary"
                      disabled={
                        paginaRepresentantes.page + 1 >=
                          paginaRepresentantes.totalPages ||
                        buscandoRepresentantes
                      }
                      onClick={() =>
                        setPaginaRepresentantes((p) => ({
                          ...p,
                          page: p.page + 1,
                        }))
                      }
                    >
                      Siguiente
                    </button>
                  </div>
                )}
                <button
                  type="button"
                  className="btn-secondary wiz-representante-cambiar"
                  onClick={registrarNuevoRepresentante}
                >
                  Registrar nuevo representante
                </button>
              </>
            ) : (
              <>
                {modoRepresentante === "seleccionado" && (
                  <p className="wiz-info">
                    Representante seleccionado:{" "}
                    <strong>
                      {rep.nombre} {rep.apellido} ·{" "}
                      {rep.id ? `ID ${rep.id}` : ""}
                    </strong>
                  </p>
                )}
                <button
                  type="button"
                  className="btn-secondary wiz-representante-cambiar"
                  onClick={buscarOtroRepresentante}
                >
                  Buscar representante existente
                </button>
              </>
            )}
            {err("rep.seleccion")}
            {errorEnvio && <p className="error-msg">{errorEnvio}</p>}
          </div>
          {modoRepresentante !== "buscar" && (
            <div className="wiz-con-foto">
              <FotoCarnet
                etiqueta="Foto del representante"
                archivo={rep.foto}
                urlActual={rep.fotoUrl || null}
                onChange={(f) => setRep("foto", f)}
                disabled={enviando}
              />
              <div className="form-grid-2">
                <label>
                  Prefijo de cédula *
                  <select
                    value={rep.cedulaTipo}
                    onChange={(e) => setRep("cedulaTipo", e.target.value)}
                  >
                    <option value="V">Venezolana (V-)</option>
                    <option value="E">Extranjera (E-)</option>
                  </select>
                </label>
                <label>
                  Número de cédula *
                  <input
                    inputMode="numeric"
                    value={rep.cedulaNumero}
                    onChange={(e) =>
                      setRep(
                        "cedulaNumero",
                        e.target.value.replace(/\D/g, "").slice(0, 8),
                      )
                    }
                    maxLength={8}
                    placeholder={
                      rep.cedulaTipo === "E" ? "80000000" : "6000000"
                    }
                  />
                  {err("rep.cedula")}
                </label>
                <label>
                  Parentesco *
                  <select
                    value={rep.parentesco}
                    onChange={(e) => setRep("parentesco", e.target.value)}
                  >
                    <option value="">Selecciona...</option>
                    {PARENTESCOS.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                  {err("rep.parentesco")}
                </label>
                <label>
                  Nombres *
                  <input
                    value={rep.nombre}
                    onChange={(e) => setRep("nombre", e.target.value)}
                    maxLength={100}
                  />
                  {err("rep.nombre")}
                </label>
                <label>
                  Apellidos *
                  <input
                    value={rep.apellido}
                    onChange={(e) => setRep("apellido", e.target.value)}
                    maxLength={100}
                  />
                  {err("rep.apellido")}
                </label>
                <label>
                  Correo *
                  <input
                    type="email"
                    value={rep.correo}
                    onChange={(e) => setRep("correo", e.target.value)}
                    placeholder="representante@correo.com"
                  />
                  {err("rep.correo")}
                </label>
                <label>
                  Teléfono principal *
                  <input
                    value={rep.telefono}
                    onChange={(e) => setRep("telefono", e.target.value)}
                    maxLength={20}
                    placeholder="04121234567"
                    required
                  />
                  {err("rep.telefono")}
                </label>
                <label>
                  Teléfono secundario
                  <input
                    value={rep.telefonoSecundario}
                    onChange={(e) =>
                      setRep("telefonoSecundario", e.target.value)
                    }
                    maxLength={20}
                  />
                  {err("rep.telefonoSecundario")}
                </label>
                <label>
                  Ocupación
                  <input
                    value={rep.ocupacion}
                    onChange={(e) => setRep("ocupacion", e.target.value)}
                    maxLength={100}
                  />
                </label>
                <label className="wiz-ancho">
                  Dirección
                  <input
                    value={rep.direccion}
                    onChange={(e) => setRep("direccion", e.target.value)}
                    placeholder="Urbanización, calle, casa"
                  />
                </label>
              </div>
            </div>
          )}

          {modoRepresentante !== "buscar" && (
            <div className="wiz-checks">
              <label className="wiz-check">
                <input
                  type="checkbox"
                  checked={form.relacionPrincipal}
                  onChange={(e) =>
                    setRaiz("relacionPrincipal", e.target.checked)
                  }
                />
                Es el representante principal
              </label>
              <label className="wiz-check">
                <input
                  type="checkbox"
                  checked={form.autorizadoRetirar}
                  onChange={(e) =>
                    setRaiz("autorizadoRetirar", e.target.checked)
                  }
                />
                Autorizado para retirar al estudiante
              </label>
              <label className="wiz-check">
                <input
                  type="checkbox"
                  checked={form.recibeComunicados}
                  onChange={(e) =>
                    setRaiz("recibeComunicados", e.target.checked)
                  }
                />
                Recibe comunicados del plantel
              </label>
            </div>
          )}
        </section>

        {errorEnvio && <p className="error-msg">{errorEnvio}</p>}
        {!embedded && (
          <div className="form-actions wiz-form-actions">
            {id && (
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  if (onCancel) onCancel();
                  else navigate(`/matricula/${id}/inscripcion`);
                }}
                disabled={enviando}
              >
                Cancelar
              </button>
            )}
            <button
              type="submit"
              className="btn-primary"
              disabled={enviando || Boolean(errorDatos)}
            >
              {enviando ? "Guardando..." : id ? "Actualizar" : "Guardar"}
            </button>
          </div>
        )}
      </form>
      {modalSeccionAbierto && (
        <div
          className="wiz-modal-overlay"
          role="presentation"
          onClick={() => setModalSeccionAbierto(false)}
        >
          <section
            className="wiz-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="wiz-modal-seccion-titulo"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id="wiz-modal-seccion-titulo">
              {seccionElegida ? "Cambiar sección" : "Seleccionar sección"}
            </h2>
            <div className="empleado-form wiz-modal-form">
              <div className="form-grid-2">
                <label>
                  Grado *
                  <select
                    value={gradoModalId}
                    onChange={(event) => {
                      setGradoModalId(event.target.value);
                      setSeccionModalId("");
                    }}
                  >
                    <option value="">Selecciona un grado...</option>
                    {grupos.map(([gradoId, grupo]) => (
                      <option key={gradoId} value={gradoId}>
                        {grupo.nombre}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Sección *
                  <select
                    value={seccionModalId}
                    disabled={!gradoModalId}
                    onChange={(event) => setSeccionModalId(event.target.value)}
                  >
                    <option value="">Selecciona una sección...</option>
                    {seccionesModal.map((seccion) => {
                      const agotada =
                        typeof seccion.cuposDisponibles === "number" &&
                        seccion.cuposDisponibles <= 0 &&
                        String(seccion.id) !== String(form.seccionId);
                      return (
                        <option
                          key={seccion.id}
                          value={seccion.id}
                          disabled={agotada}
                        >
                          Sección {seccion.letraSeccion} —{" "}
                          {agotada
                            ? "Sin cupos"
                            : `${seccion.cuposDisponibles ?? seccion.capacidadMaxima ?? "—"} cupos`}
                        </option>
                      );
                    })}
                  </select>
                </label>
              </div>
              {err("seccionId")}
              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setModalSeccionAbierto(false)}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  disabled={!seccionModalId}
                  onClick={guardarSeccionModal}
                >
                  Usar sección
                </button>
              </div>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

export default Preinscripcion;
