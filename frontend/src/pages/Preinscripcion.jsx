// src/pages/Preinscripcion.jsx
import { useState, useEffect, useMemo } from "react";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import { descargarArchivo, mensajeErrorBlob } from "../utils/descargas";
import "./Preinscripcion.css";

const PASOS = ["Estudiante", "Representante", "Sección"];
const PARENTESCOS = ["Padre", "Madre", "Tutor", "Abuelo", "Otro"];
const SEXOS = [
  { valor: "M", etiqueta: "Masculino" },
  { valor: "F", etiqueta: "Femenino" },
  { valor: "Otro", etiqueta: "Otro" },
];
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

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
    cedula: "",
    nombre: "",
    apellido: "",
    fechaNacimiento: "",
    sexo: "",
    correo: "",
    telefono: "",
  },
  rep: {
    cedula: "",
    nombre: "",
    apellido: "",
    parentesco: "",
    correo: "",
    telefono: "",
    telefonoSecundario: "",
    direccion: "",
    ocupacion: "",
  },
  institucionProcedencia: "",
  seccionId: "",
  relacionPrincipal: true,
  autorizadoRetirar: true,
  recibeComunicados: true,
};

const o = (v) => (v.trim() ? v.trim() : null);

function Preinscripcion() {
  const toast = useToast();
  const [form, setForm] = useState(VACIO);
  const [paso, setPaso] = useState(0);
  const [errores, setErrores] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState("");
  const [resultado, setResultado] = useState(null);
  const [descargando, setDescargando] = useState(false);

  const [calendario, setCalendario] = useState(null);
  const [secciones, setSecciones] = useState([]);
  const [cargandoDatos, setCargandoDatos] = useState(true);
  const [errorDatos, setErrorDatos] = useState("");

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

  const anio = calendario?.anioActivo?.anio ?? null;
  const anioNombre =
    calendario?.anioActivo?.nombre ?? (anio ? String(anio) : "—");

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
      const clave = s.gradoNombre ?? "Sin grado";
      if (!mapa.has(clave)) mapa.set(clave, []);
      mapa.get(clave).push(s);
    });
    return [...mapa.entries()];
  }, [secciones, anio]);

  const seccionElegida = useMemo(
    () => secciones.find((s) => String(s.id) === String(form.seccionId)),
    [secciones, form.seccionId],
  );

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
      if (!est.cedula.trim()) e["est.cedula"] = "La cédula es obligatoria.";
      else if (est.cedula.trim().length > 20)
        e["est.cedula"] = "Máximo 20 caracteres.";
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
      if (!rep.cedula.trim()) e["rep.cedula"] = "La cédula es obligatoria.";
      else if (rep.cedula.trim().length > 20)
        e["rep.cedula"] = "Máximo 20 caracteres.";
      if (!rep.nombre.trim()) e["rep.nombre"] = "El nombre es obligatorio.";
      if (!rep.apellido.trim())
        e["rep.apellido"] = "El apellido es obligatorio.";
      if (!rep.parentesco) e["rep.parentesco"] = "Selecciona el parentesco.";
      if (!rep.correo.trim()) e["rep.correo"] = "El correo es obligatorio.";
      else if (!EMAIL.test(rep.correo.trim()))
        e["rep.correo"] = "Correo no válido.";
      if (rep.telefono.trim().length > 20)
        e["rep.telefono"] = "Máximo 20 caracteres.";
      if (rep.telefonoSecundario.trim().length > 20)
        e["rep.telefonoSecundario"] = "Máximo 20 caracteres.";
    }
    if (n === 2) {
      if (!form.seccionId) e.seccionId = "Selecciona una sección.";
    }
    setErrores((p) => ({ ...p, ...e }));
    return Object.keys(e).length === 0;
  };

  const siguiente = () => {
    if (validarPaso(paso)) setPaso((p) => Math.min(p + 1, PASOS.length - 1));
  };
  const anterior = () => setPaso((p) => Math.max(p - 1, 0));

  // ---------- Envío ----------
  const enviar = async () => {
    if (!validarPaso(2)) return;
    if (anio == null) {
      setErrorEnvio(
        "No hay un año escolar activo en el calendario de matrícula.",
      );
      return;
    }
    const { est, rep } = form;
    const payload = {
      estudiante: {
        cedula: est.cedula.trim(),
        nombre: est.nombre.trim(),
        apellido: est.apellido.trim(),
        fechaNacimiento: est.fechaNacimiento,
        sexo: est.sexo,
        correo: o(est.correo),
        telefono: o(est.telefono),
      },
      representante: {
        cedula: rep.cedula.trim(),
        nombre: rep.nombre.trim(),
        apellido: rep.apellido.trim(),
        parentesco: rep.parentesco,
        correo: rep.correo.trim(),
        telefono: o(rep.telefono),
        telefonoSecundario: o(rep.telefonoSecundario),
        direccion: o(rep.direccion),
        ocupacion: o(rep.ocupacion),
      },
      seccionId: Number(form.seccionId),
      anioEscolar: Number(anio),
      institucionProcedencia: o(form.institucionProcedencia),
      relacionPrincipal: form.relacionPrincipal,
      autorizadoRetirar: form.autorizadoRetirar,
      recibeComunicados: form.recibeComunicados,
    };

    setEnviando(true);
    setErrorEnvio("");
    try {
      const { data } = await api.post("/matriculas/preinscripciones", payload);
      setResultado(data);
      toast.success("Preinscripción registrada correctamente.");
    } catch (error) {
      // 400 edad/año, 409 cédula, cupo o matrícula duplicada: mensaje del backend
      const mensaje = extractErrorMessage(
        error,
        "No se pudo registrar la preinscripción.",
      );
      setErrorEnvio(mensaje);
      toast.error(mensaje);
    } finally {
      setEnviando(false);
    }
  };

  const manejarSubmit = (ev) => {
    ev.preventDefault();
    if (paso < PASOS.length - 1) siguiente();
    else enviar();
  };

  const descargarConstancia = async () => {
    setDescargando(true);
    try {
      const { data } = await api.get(
        `/matriculas/${resultado.matriculaId}/constancia-asignacion-cupo`,
        { responseType: "blob" },
      );
      descargarArchivo(
        data,
        `constancia-aceptacion-cupo-${resultado.matriculaId}.pdf`,
      );
    } catch (error) {
      toast.error(
        await mensajeErrorBlob(error, "No se pudo descargar la constancia."),
      );
    } finally {
      setDescargando(false);
    }
  };

  const nueva = () => {
    setForm(VACIO);
    setErrores({});
    setErrorEnvio("");
    setResultado(null);
    setPaso(0);
  };

  // ---------- Pantalla de resultado ----------
  if (resultado) {
    return (
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Preinscripción</h1>
        </div>

        <div className="empleado-form wiz wiz-exito">
          <span className="wiz-exito-icono" aria-hidden="true">
            <Icon icon={ICONOS.exito} width="44" height="44" />
          </span>
          <h2>Preinscripción registrada</h2>
          <p className="wiz-exito-texto">
            Se asignó el cupo a{" "}
            <strong>{resultado.estudianteNombreCompleto}</strong>.
          </p>

          <dl className="wiz-resumen">
            <div>
              <dt>Número de solicitud</dt>
              <dd>{resultado.matriculaId}</dd>
            </div>
            <div>
              <dt>Estado</dt>
              <dd>{resultado.estadoMatricula}</dd>
            </div>
            <div>
              <dt>Representante</dt>
              <dd>{resultado.representanteNombreCompleto}</dd>
            </div>
            <div>
              <dt>Sección</dt>
              <dd>
                {seccionElegida
                  ? `${seccionElegida.gradoNombre ?? ""} · Sección ${seccionElegida.letraSeccion}`
                  : resultado.seccionId}
              </dd>
            </div>
            <div>
              <dt>Año escolar</dt>
              <dd>{anioNombre}</dd>
            </div>
            <div>
              <dt>Tipo de ingreso</dt>
              <dd>{resultado.tipoIngreso}</dd>
            </div>
            <div>
              <dt>Cupos restantes en la sección</dt>
              <dd>{resultado.cuposDisponibles}</dd>
            </div>
          </dl>

          <div className="form-actions wiz-acciones-final">
            <button type="button" className="btn-secondary" onClick={nueva}>
              Registrar otra preinscripción
            </button>
            <button
              type="button"
              className="btn-primary"
              onClick={descargarConstancia}
              disabled={descargando}
            >
              <Icon icon={ICONOS.descargar} className="icono-inline" />{" "}
              {descargando ? "Descargando..." : "Descargar constancia de cupo"}
            </button>
          </div>
        </div>
      </div>
    );
  }

  const { est, rep } = form;

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>Preinscripción</h1>
      </div>

      <form className="empleado-form wiz" onSubmit={manejarSubmit} noValidate>
        {/* Indicador de progreso */}
        <ol className="wiz-pasos" aria-label="Progreso de la preinscripción">
          {PASOS.map((titulo, i) => (
            <li
              key={titulo}
              className={`wiz-paso${i === paso ? " activo" : ""}${i < paso ? " hecho" : ""}`}
              aria-current={i === paso ? "step" : undefined}
            >
              <button
                type="button"
                onClick={() => setPaso(i)}
                disabled={i > paso}
              >
                <span className="wiz-paso-num">
                  {i < paso ? <Icon icon={ICONOS.check} /> : i + 1}
                </span>
                <span className="wiz-paso-titulo">{titulo}</span>
              </button>
            </li>
          ))}
        </ol>
        <p className="wiz-contador">
          Paso {paso + 1} de {PASOS.length} · Año escolar {anioNombre}
        </p>

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

        {/* Paso 1 */}
        {paso === 0 && (
          <div className="wiz-seccion-form">
            <div className="form-grid-2">
              <label>
                Cédula *
                <input
                  value={est.cedula}
                  onChange={(e) => setEst("cedula", e.target.value)}
                  maxLength={20}
                  placeholder="12345678"
                  autoFocus
                />
                {err("est.cedula")}
              </label>
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
        )}

        {/* Paso 2 */}
        {paso === 1 && (
          <div className="wiz-seccion-form">
            <div className="form-grid-2">
              <label>
                Cédula *
                <input
                  value={rep.cedula}
                  onChange={(e) => setRep("cedula", e.target.value)}
                  maxLength={20}
                  placeholder="12345678"
                  autoFocus
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
                Teléfono
                <input
                  value={rep.telefono}
                  onChange={(e) => setRep("telefono", e.target.value)}
                  maxLength={20}
                  placeholder="04121234567"
                />
                {err("rep.telefono")}
              </label>
              <label>
                Teléfono secundario
                <input
                  value={rep.telefonoSecundario}
                  onChange={(e) => setRep("telefonoSecundario", e.target.value)}
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
          </div>
        )}

        {/* Paso 3 */}
        {paso === 2 && (
          <div className="wiz-seccion-form">
            {cargandoDatos ? (
              <p>Cargando secciones...</p>
            ) : grupos.length === 0 ? (
              <p className="wiz-aviso">
                <Icon icon={ICONOS.aviso} className="icono-inline" /> No hay
                secciones activas para el año escolar {anioNombre}.
              </p>
            ) : (
              <fieldset className="wiz-grupos">
                <legend>Selecciona la sección *</legend>
                {grupos.map(([grado, lista]) => (
                  <div key={grado} className="wiz-grado">
                    <h3>{grado}</h3>
                    <div className="wiz-secciones">
                      {lista.map((s) => {
                        const conCupos = typeof s.cuposDisponibles === "number";
                        const agotada = conCupos && s.cuposDisponibles <= 0;
                        return (
                          <label
                            key={s.id}
                            className={`wiz-seccion${agotada ? " agotada" : ""}`}
                          >
                            <input
                              type="radio"
                              name="seccionId"
                              value={s.id}
                              checked={String(form.seccionId) === String(s.id)}
                              disabled={agotada}
                              onChange={() =>
                                setRaiz("seccionId", String(s.id))
                              }
                            />
                            <span className="wiz-seccion-nombre">
                              Sección {s.letraSeccion}
                            </span>
                            <span className="wiz-seccion-cupos">
                              {conCupos
                                ? agotada
                                  ? "Sin cupos"
                                  : `${s.cuposDisponibles} cupos disponibles`
                                : `Capacidad: ${s.capacidadMaxima ?? "—"}`}
                            </span>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))}
                {err("seccionId")}
              </fieldset>
            )}

            {seccionElegida && (
              <dl className="wiz-resumen">
                <div>
                  <dt>Estudiante</dt>
                  <dd>
                    {est.nombre} {est.apellido} ({est.cedula})
                  </dd>
                </div>
                <div>
                  <dt>Representante</dt>
                  <dd>
                    {rep.nombre} {rep.apellido} · {rep.parentesco}
                  </dd>
                </div>
                <div>
                  <dt>Sección</dt>
                  <dd>
                    {seccionElegida.gradoNombre} · Sección{" "}
                    {seccionElegida.letraSeccion}
                  </dd>
                </div>
              </dl>
            )}

            {errorEnvio && <p className="error-msg">{errorEnvio}</p>}
          </div>
        )}

        <div className="form-actions">
          {paso > 0 && (
            <button
              type="button"
              className="btn-secondary"
              onClick={anterior}
              disabled={enviando}
            >
              Anterior
            </button>
          )}
          {paso < PASOS.length - 1 ? (
            <button type="submit" className="btn-primary">
              Siguiente
            </button>
          ) : (
            <button
              type="submit"
              className="btn-primary"
              disabled={enviando || !form.seccionId}
            >
              {enviando ? "Registrando..." : "Registrar preinscripción"}
            </button>
          )}
        </div>
      </form>
    </div>
  );
}

export default Preinscripcion;
