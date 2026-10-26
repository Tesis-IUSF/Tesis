// src/pages/Inscripcion.jsx
import { useState, useEffect, useCallback, useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import { descargarArchivo, mensajeErrorBlob } from "../utils/descargas";
import ModalFormulario from "../components/ModalFormulario";
import Preinscripcion from "./Preinscripcion";
import "./Inscripcion.css";

const ETIQUETA_ESTADO = {
  preinscrito: "Preinscrito",
  en_proceso: "En proceso",
  completada: "Formalizada",
  retirada: "Retirada",
};

const formatFechaISO = (v) => {
  if (!v) return "—";
  const [a, m, d] = String(v).split("-");
  return `${d}/${m}/${a}`;
};

const formatFechaHora = (v) => {
  if (!v) return "—";
  const f = new Date(v);
  return Number.isNaN(f.getTime())
    ? "—"
    : f.toLocaleString("es-VE", { dateStyle: "short", timeStyle: "short" });
};

const o = (v) => (v && v.trim() ? v.trim() : null);

function Inscripcion() {
  const { id } = useParams();
  const toast = useToast();

  const [data, setData] = useState(null);
  const [expediente, setExpediente] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [editandoFicha, setEditandoFicha] = useState(false);
  const [guardandoFicha, setGuardandoFicha] = useState(false);
  const [checklistBorrador, setChecklistBorrador] = useState({});
  const [historial, setHistorial] = useState([]);
  const [modalHistorial, setModalHistorial] = useState(false);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);
  const [errorHistorial, setErrorHistorial] = useState("");

  const [guardandoId, setGuardandoId] = useState(null);
  const [abiertos, setAbiertos] = useState(() => new Set());

  const [modalFormalizar, setModalFormalizar] = useState(false);
  const [numeroConstancia, setNumeroConstancia] = useState("");
  const [formalizando, setFormalizando] = useState(false);
  const [descargando, setDescargando] = useState("");

  const cargar = useCallback(async ({ silencioso = false } = {}) => {
    if (!silencioso) setCargando(true);
    setError("");
    try {
      const [seguimientoResponse, expedienteResponse] = await Promise.all([
        api.get(`/matriculas/${id}/checklist`),
        api.get(`/matriculas/${id}/expediente`),
      ]);
      let seguimiento = seguimientoResponse.data;
      const datosExpediente = expedienteResponse.data;
      // Sin requisitos todavía: el backend los crea según nivel y tipo de ingreso
      if (
        seguimiento.checklist.length === 0 &&
        ["preinscrito", "en_proceso"].includes(seguimiento.estadoMatricula)
      ) {
        ({ data: seguimiento } = await api.post(`/matriculas/${id}/checklist`));
      }
      setData(seguimiento);
      setExpediente(datosExpediente);
    } catch (err) {
      setError(extractErrorMessage(err, "No se pudo cargar la inscripción."));
    } finally {
      setCargando(false);
    }
  }, [id]);

  useEffect(() => {
    void Promise.resolve().then(cargar);
  }, [cargar]);

  const editable =
    !!data && ["preinscrito", "en_proceso"].includes(data.estadoMatricula);
  const fichaEditable =
    !!data &&
    ["preinscrito", "en_proceso", "completada"].includes(data.estadoMatricula);
  const formalizada = data?.estadoMatricula === "completada";
  const checklistEditable = editable && editandoFicha;

  const items = useMemo(() => {
    if (!data) return [];
    return [...data.checklist].sort(
      (a, b) =>
        Number(b.obligatorio) - Number(a.obligatorio) ||
        a.nombreRequisito.localeCompare(b.nombreRequisito, "es"),
    );
  }, [data]);

  const total = data?.requisitosObligatorios ?? 0;
  const hechos = data?.requisitosObligatoriosCumplidos ?? 0;
  const faltan = Math.max(total - hechos, 0);
  const pct = total > 0 ? Math.round((hechos / total) * 100) : 100;

  const alGuardarFicha = async ({ fichaModificada }) => {
    try {
      const checklistModificado = await guardarChecklistBorrador();
      await cargar({ silencioso: true });
      setEditandoFicha(false);
      setChecklistBorrador({});
      if (fichaModificada || checklistModificado) {
        toast.success(
          checklistModificado && fichaModificada
            ? "Ficha y requisitos guardados."
            : checklistModificado
              ? "Requisitos guardados."
              : "Ficha actualizada.",
        );
      } else {
        toast.info("No se realizaron cambios.");
      }
    } catch (err) {
      await cargar({ silencioso: true });
      const mensaje = extractErrorMessage(
        err,
        "No se pudieron guardar los cambios.",
      );
      toast.error(
        fichaModificada
          ? `${mensaje} La ficha se actualizó, pero los requisitos no; revisa el expediente antes de volver a guardar.`
          : mensaje,
      );
    }
  };

  const alternarDetalle = (requisitoId) => {
    setAbiertos((prev) => {
      const sig = new Set(prev);
      if (sig.has(requisitoId)) sig.delete(requisitoId);
      else sig.add(requisitoId);
      return sig;
    });
  };

  const iniciarEdicion = () => {
    setChecklistBorrador(
      Object.fromEntries(
        (data?.checklist ?? []).map((item) => [
          item.requisitoId,
          {
            cumplido: Boolean(item.cumplido),
            notas: item.notas ?? "",
            archivoUrl: item.archivoUrl ?? "",
          },
        ]),
      ),
    );
    setEditandoFicha(true);
  };

  const descartarEdicion = () => {
    setChecklistBorrador({});
    setEditandoFicha(false);
  };

  const cambiarRequisitoBorrador = (requisitoId, cumplido) => {
    setChecklistBorrador((actual) => ({
      ...actual,
      [requisitoId]: { ...actual[requisitoId], cumplido },
    }));
  };

  const guardarChecklistBorrador = async () => {
    const cambios = (data?.checklist ?? [])
      .filter(
        (item) =>
          checklistBorrador[item.requisitoId]?.cumplido !==
            Boolean(item.cumplido) ||
          (checklistBorrador[item.requisitoId]?.notas ?? "") !==
            (item.notas ?? "") ||
          (checklistBorrador[item.requisitoId]?.archivoUrl ?? "") !==
            (item.archivoUrl ?? ""),
      )
      .map((item) => ({
        requisitoId: item.requisitoId,
        cumplido:
          checklistBorrador[item.requisitoId]?.cumplido ??
          Boolean(item.cumplido),
        notas: checklistBorrador[item.requisitoId]?.notas ?? item.notas ?? null,
        archivoUrl:
          checklistBorrador[item.requisitoId]?.archivoUrl ??
          item.archivoUrl ??
          null,
      }));

    if (cambios.length === 0) {
      return false;
    }

    setGuardandoId("checklist");
    try {
      const { data: respuesta } = await api.put(
        `/matriculas/${id}/checklist`,
        { items: cambios },
      );
      setData(respuesta);
      return true;
    } finally {
      setGuardandoId(null);
    }
  };

  const abrirHistorialAcademico = async () => {
    setModalHistorial(true);
    setCargandoHistorial(true);
    setErrorHistorial("");
    try {
      const { data: respuesta } = await api.get(
        `/matriculas/${id}/historial-academico`,
      );
      setHistorial(respuesta);
    } catch (err) {
      setErrorHistorial(
        extractErrorMessage(err, "No se pudo cargar el historial académico."),
      );
    } finally {
      setCargandoHistorial(false);
    }
  };

  // ---------- Formalizar ----------
  const formalizar = async (ev) => {
    ev.preventDefault();
    if (numeroConstancia.trim().length > 50) {
      toast.error("El número de constancia admite máximo 50 caracteres.");
      return;
    }
    setFormalizando(true);
    try {
      const { data: respuesta } = await api.post(
        `/matriculas/${id}/formalizar`,
        { numeroConstancia: o(numeroConstancia) },
      );
      setData(respuesta);
      setModalFormalizar(false);
      toast.success("Inscripción formalizada correctamente.");
    } catch (err) {
      // 409: "Faltan requisitos obligatorios: ..." u otro motivo
      toast.error(
        extractErrorMessage(err, "No se pudo formalizar la inscripción."),
      );
    } finally {
      setFormalizando(false);
    }
  };

  const descargar = async (tipo) => {
    const ruta =
      tipo === "inscripcion"
        ? "constancia-inscripcion"
        : "constancia-asignacion-cupo";
    const nombre =
      tipo === "inscripcion"
        ? `constancia-inscripcion-${id}.pdf`
        : `constancia-aceptacion-cupo-${id}.pdf`;
    setDescargando(tipo);
    try {
      const { data: blob } = await api.get(`/matriculas/${id}/${ruta}`, {
        responseType: "blob",
      });
      descargarArchivo(blob, nombre);
    } catch (err) {
      toast.error(
        await mensajeErrorBlob(err, "No se pudo descargar la constancia."),
      );
    } finally {
      setDescargando("");
    }
  };

  // ---------- Render ----------
  if (cargando) {
    return (
      <div className="mod-main">
        <p>Cargando inscripción...</p>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Inscripción</h1>
        </div>
        <p className="error-msg">{error || "No se encontró la matrícula."}</p>
        <div className="ins-acciones">
          <Link to="/matricula" className="btn-secondary">
            Volver a Matrícula
          </Link>
          <button type="button" className="btn-primary" onClick={cargar}>
            Reintentar
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>Expediente de matrícula</h1>
        <div className="mod-heading-actions">
          {!editandoFicha && (
            <button
              type="button"
              className="btn-secondary"
              onClick={abrirHistorialAcademico}
            >
              Ver historial académico
            </button>
          )}
          {fichaEditable &&
            (editandoFicha ? (
              <>
                <button
                  type="submit"
                  form={`preinscripcion-${id}`}
                  className="btn-primary"
                  disabled={Boolean(guardandoId) || guardandoFicha}
                >
                  Guardar
                </button>
                <button
                  type="button"
                  className="btn-secondary"
                  disabled={Boolean(guardandoId) || guardandoFicha}
                  onClick={descartarEdicion}
                >
                  Descartar
                </button>
              </>
            ) : (
            <button
              type="button"
              className="btn-primary"
              onClick={iniciarEdicion}
            >
              Editar
            </button>
            ))}
          {!editandoFicha && (
            <Link to="/matricula" className="btn-secondary">
              <Icon icon={ICONOS.volver} className="icono-inline" /> Volver a
              Matrícula
            </Link>
          )}
        </div>
      </div>

      {/* Datos del estudiante */}
      <section className="ins-tarjeta ins-cabecera">
        <div>
          <h2>{data.estudianteNombreCompleto}</h2>
          <p className="ins-sub">
            {expediente?.nivelEducativoNombre} · {expediente?.gradoNombre} ·
            {" Sección "}
            {expediente?.letraSeccion} · Año escolar {data.anioEscolar}
          </p>
        </div>
        <div className="ins-cabecera-datos">
          <span className={`badge mat-estado--${data.estadoMatricula}`}>
            {ETIQUETA_ESTADO[data.estadoMatricula] ?? data.estadoMatricula}
          </span>
          <small>Solicitud: {formatFechaISO(data.fechaSolicitud)}</small>
        </div>
      </section>

      {editandoFicha ? (
        <Preinscripcion
          key={id}
          matriculaId={id}
          embedded
          formId={`preinscripcion-${id}`}
          onCancel={descartarEdicion}
          onSaved={alGuardarFicha}
          onSubmitting={setGuardandoFicha}
        />
      ) : (
      <>
      <section className="ins-tarjeta">
        <h3 className="ins-seccion-titulo">Datos del estudiante</h3>
        <dl className="ins-datos-grid">
          <div><dt>Cédula</dt><dd>{expediente?.estudiante?.cedula || "—"}</dd></div>
          <div><dt>Fecha de nacimiento</dt><dd>{formatFechaISO(expediente?.estudiante?.fechaNacimiento)}</dd></div>
          <div><dt>Sexo</dt><dd>{expediente?.estudiante?.sexo || "—"}</dd></div>
          <div><dt>Correo</dt><dd>{expediente?.estudiante?.correo || "—"}</dd></div>
          <div><dt>Teléfono</dt><dd>{expediente?.estudiante?.telefono || "—"}</dd></div>
          <div><dt>N.º de expediente</dt><dd>{expediente?.estudiante?.numeroExpediente || "—"}</dd></div>
          <div className="ins-dato-completo"><dt>Observaciones</dt><dd>{expediente?.estudiante?.observaciones || "—"}</dd></div>
        </dl>
      </section>

      <section className="ins-tarjeta">
        <h3 className="ins-seccion-titulo">Representantes vinculados</h3>
        {expediente?.representantes?.length ? (
          <div className="ins-representantes">
            {expediente.representantes.map((representante) => (
              <article className="ins-representante" key={representante.id}>
                <h4>
                  {representante.nombre} {representante.apellido}
                  {representante.relacionPrincipal && (
                    <span className="ins-principal">Principal</span>
                  )}
                </h4>
                <dl className="ins-datos-grid">
                  <div><dt>Cédula</dt><dd>{representante.cedula || "—"}</dd></div>
                  <div><dt>Parentesco</dt><dd>{representante.parentesco || "—"}</dd></div>
                  <div><dt>Correo</dt><dd>{representante.correo || "—"}</dd></div>
                  <div><dt>Teléfono</dt><dd>{representante.telefono || "—"}</dd></div>
                  <div><dt>Teléfono secundario</dt><dd>{representante.telefonoSecundario || "—"}</dd></div>
                  <div><dt>Ocupación</dt><dd>{representante.ocupacion || "—"}</dd></div>
                  <div className="ins-dato-completo"><dt>Dirección</dt><dd>{representante.direccion || "—"}</dd></div>
                </dl>
              </article>
            ))}
          </div>
        ) : (
          <p>No hay representantes activos vinculados a este estudiante.</p>
        )}
      </section>

      <section className="ins-tarjeta">
        <h3 className="ins-seccion-titulo">Datos de matrícula</h3>
        <dl className="ins-datos-grid">
          <div><dt>Tipo de ingreso</dt><dd>{expediente?.tipoIngreso || "—"}</dd></div>
          <div><dt>Institución de procedencia</dt><dd>{expediente?.institucionProcedencia || "—"}</dd></div>
          <div><dt>Fecha de solicitud</dt><dd>{formatFechaISO(data.fechaSolicitud)}</dd></div>
          <div><dt>Fecha de formalización</dt><dd>{formatFechaISO(data.fechaFormalizacion)}</dd></div>
          <div><dt>Constancia</dt><dd>{data.numeroConstancia || "—"}</dd></div>
        </dl>
      </section>
      </>
      )}

      {/* Progreso */}
      <section className="ins-tarjeta">
        <div className="ins-progreso-cab">
          <h3>Requisitos de inscripción</h3>
          <strong>
            {hechos} de {total} obligatorios
          </strong>
        </div>
        <div
          className="ins-barra"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={pct}
          aria-label="Requisitos obligatorios cumplidos"
        >
          <span style={{ width: `${pct}%` }} />
        </div>

        {items.length === 0 ? (
          <p className="ins-vacio">
            No hay requisitos configurados para el nivel y el tipo de ingreso de
            este estudiante. Pide al administrador que los registre en el
            catálogo de requisitos.
          </p>
        ) : (
          <ul className="ins-lista">
            {items.map((item) => {
              const abierto = abiertos.has(item.requisitoId);
              const b = checklistEditable
                ? checklistBorrador[item.requisitoId] ?? {
                    cumplido: Boolean(item.cumplido),
                    notas: item.notas ?? "",
                    archivoUrl: item.archivoUrl ?? "",
                  }
                : item;
              const cumplidoMostrado = checklistEditable
                ? Boolean(b.cumplido)
                : Boolean(item.cumplido);
              const ocupado = Boolean(guardandoId) || guardandoFicha;
              return (
                <li
                  key={item.requisitoId}
                  className={`ins-item${cumplidoMostrado ? " cumplido" : ""}`}
                >
                  <div className="ins-item-fila">
                    <label className="ins-check">
                      <input
                        type="checkbox"
                        checked={
                          checklistEditable
                            ? checklistBorrador[item.requisitoId]?.cumplido ??
                              item.cumplido ??
                              false
                            : item.cumplido ?? false
                        }
                        disabled={!checklistEditable || ocupado}
                        onChange={() =>
                          cambiarRequisitoBorrador(
                            item.requisitoId,
                            !(
                              checklistBorrador[item.requisitoId]?.cumplido ??
                              item.cumplido ??
                              false
                            ),
                          )
                        }
                      />
                      <span className="ins-item-texto">
                        <strong>{item.nombreRequisito}</strong>
                        {item.descripcionRequisito && (
                          <small>{item.descripcionRequisito}</small>
                        )}
                        {item.cumplido && item.fechaVerificacion && (
                          <small className="ins-verificado">
                            Verificado el{" "}
                            {formatFechaHora(item.fechaVerificacion)}
                          </small>
                        )}
                      </span>
                    </label>

                    <span
                      className={`ins-etiqueta${item.obligatorio ? " obligatorio" : ""}`}
                    >
                      {item.obligatorio ? "Obligatorio" : "Opcional"}
                    </span>

                    <button
                      type="button"
                      className="btn-edit"
                      onClick={() => alternarDetalle(item.requisitoId)}
                      aria-expanded={abierto}
                    >
                      {abierto ? "Ocultar" : "Detalle"}
                    </button>
                  </div>

                  {abierto && b && (
                    <div className="ins-detalle">
                      <label>
                        Notas
                        <textarea
                          rows={2}
                          value={b.notas}
                          disabled={!checklistEditable}
                          onChange={(e) =>
                            setChecklistBorrador((actual) => ({
                              ...actual,
                              [item.requisitoId]: {
                                ...b,
                                notas: e.target.value,
                              },
                            }))
                          }
                          placeholder="Observaciones sobre este requisito"
                        />
                      </label>
                      <label>
                        Enlace del documento
                        <input
                          value={b.archivoUrl}
                          disabled={!checklistEditable}
                          onChange={(e) =>
                            setChecklistBorrador((actual) => ({
                              ...actual,
                              [item.requisitoId]: {
                                ...b,
                                archivoUrl: e.target.value,
                              },
                            }))
                          }
                          placeholder="https://..."
                        />
                      </label>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {/* Cierre */}
      {formalizada ? (
        <section className="ins-tarjeta ins-exito">
          <span className="ins-exito-icono" aria-hidden="true">
            <Icon icon={ICONOS.exito} width="36" height="36" />
          </span>
          <div className="ins-exito-texto">
            <h3>Inscripción formalizada</h3>
            <p>
              Formalizada el {formatFechaISO(data.fechaFormalizacion)}
              {data.numeroConstancia
                ? ` · Constancia N.º ${data.numeroConstancia}`
                : ""}
              .
            </p>
          </div>
          <div className="ins-acciones">
            <button
              type="button"
              className="btn-primary"
              disabled={descargando === "inscripcion"}
              onClick={() => descargar("inscripcion")}
            >
              <Icon icon={ICONOS.descargar} className="icono-inline" />{" "}
              {descargando === "inscripcion"
                ? "Descargando..."
                : "Constancia de inscripción"}
            </button>
          </div>
        </section>
      ) : editable ? (
        <section className="ins-tarjeta ins-cierre">
          <p className={faltan > 0 ? "ins-pendiente" : "ins-listo"}>
            {faltan > 0
              ? `Faltan ${faltan} requisito${faltan === 1 ? "" : "s"} obligatorio${faltan === 1 ? "" : "s"} para poder formalizar.`
              : "Todos los requisitos obligatorios están cumplidos."}
          </p>
          <div className="ins-acciones">
            {data.estadoMatricula === "preinscrito" && (
              <button
                type="button"
                className="btn-secondary"
                disabled={descargando === "cupo"}
                onClick={() => descargar("cupo")}
              >
                {descargando === "cupo"
                  ? "Descargando..."
                  : "Constancia de cupo"}
              </button>
            )}
            <button
              type="button"
              className="btn-primary"
              disabled={faltan > 0}
              onClick={() => {
                setNumeroConstancia("");
                setModalFormalizar(true);
              }}
            >
              Formalizar inscripción
            </button>
          </div>
        </section>
      ) : null}

      {modalFormalizar && !editandoFicha && (
        <ModalFormulario
          titulo="Formalizar inscripción"
          onCerrar={() => setModalFormalizar(false)}
          onSubmit={formalizar}
          guardando={formalizando}
          etiquetaGuardar="Formalizar"
        >
          <p className="ins-modal-texto">
            Se inscribirá de forma definitiva a{" "}
            <strong>{data.estudianteNombreCompleto}</strong>. Después de
            formalizar no se pueden modificar los requisitos.
          </p>
          <label>
            Número de constancia (opcional)
            <input
              value={numeroConstancia}
              onChange={(e) => setNumeroConstancia(e.target.value)}
              maxLength={50}
              autoFocus
            />
          </label>
        </ModalFormulario>
      )}
      {modalHistorial && (
        <div
          className="ins-historial-overlay"
          role="presentation"
          onClick={() => !cargandoHistorial && setModalHistorial(false)}
        >
          <section
            className="ins-historial-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="ins-historial-titulo"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="ins-historial-cabecera">
              <h2 id="ins-historial-titulo">Historial académico</h2>
              <button
                type="button"
                className="btn-secondary"
                disabled={cargandoHistorial}
                onClick={() => setModalHistorial(false)}
              >
                Cerrar
              </button>
            </div>
            {cargandoHistorial ? (
              <p>Cargando historial académico...</p>
            ) : errorHistorial ? (
              <p className="error-msg">{errorHistorial}</p>
            ) : historial.length === 0 ? (
              <p>No hay matrículas académicas registradas.</p>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Año escolar</th>
                      <th>Nivel y grado</th>
                      <th>Sección</th>
                      <th>Estado</th>
                      <th>Solicitud</th>
                      <th>Formalización</th>
                    </tr>
                  </thead>
                  <tbody>
                    {historial.map((matricula) => (
                      <tr key={matricula.matriculaId}>
                        <td>{matricula.anioEscolar}</td>
                        <td>
                          {matricula.nivelEducativoNombre} ·{" "}
                          {matricula.gradoNombre}
                        </td>
                        <td>{matricula.letraSeccion}</td>
                        <td>
                          {ETIQUETA_ESTADO[matricula.estadoMatricula] ??
                            matricula.estadoMatricula}
                        </td>
                        <td>{formatFechaISO(matricula.fechaSolicitud)}</td>
                        <td>{formatFechaISO(matricula.fechaFormalizacion)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

export default Inscripcion;
