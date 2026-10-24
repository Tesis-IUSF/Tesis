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
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [secciones, setSecciones] = useState([]);

  const [guardandoId, setGuardandoId] = useState(null);
  const [abiertos, setAbiertos] = useState(() => new Set());
  const [borradores, setBorradores] = useState({});

  const [modalFormalizar, setModalFormalizar] = useState(false);
  const [formalizando, setFormalizando] = useState(false);
  const [descargando, setDescargando] = useState("");

  const cargar = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      let { data: seguimiento } = await api.get(`/matriculas/${id}/checklist`);
      // Sin requisitos todavía: el backend los crea según nivel y tipo de ingreso
      if (
        seguimiento.checklist.length === 0 &&
        ["preinscrito", "en_proceso"].includes(seguimiento.estadoMatricula)
      ) {
        ({ data: seguimiento } = await api.post(`/matriculas/${id}/checklist`));
      }
      setData(seguimiento);
    } catch (err) {
      setError(extractErrorMessage(err, "No se pudo cargar la inscripción."));
    } finally {
      setCargando(false);
    }
  }, [id]);

  useEffect(() => {
    cargar();
    api
      .get("/catalogos/secciones", { params: { page: 0, size: 100 } })
      .then(({ data: r }) => setSecciones(r.content))
      .catch(() => setSecciones([]));
  }, [cargar]);

  const seccion = useMemo(
    () => secciones.find((s) => s.id === data?.seccionId),
    [secciones, data],
  );

  const editable =
    !!data && ["preinscrito", "en_proceso"].includes(data.estadoMatricula);
  const formalizada = data?.estadoMatricula === "completada";

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

  // ---------- Requisitos ----------
  // El PATCH sobrescribe notas y archivo: siempre se reenvían los valores guardados.
  const actualizar = async (item, cambios) => {
    setGuardandoId(item.requisitoId);
    try {
      const { data: respuesta } = await api.patch(
        `/matriculas/${id}/checklist/${item.requisitoId}`,
        {
          cumplido: item.cumplido,
          notas: item.notas ?? null,
          archivoUrl: item.archivoUrl ?? null,
          ...cambios,
        },
      );
      setData(respuesta);
      return true;
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo actualizar el requisito."),
      );
      return false;
    } finally {
      setGuardandoId(null);
    }
  };

  const alternarDetalle = (item) => {
    setAbiertos((prev) => {
      const sig = new Set(prev);
      if (sig.has(item.requisitoId)) sig.delete(item.requisitoId);
      else sig.add(item.requisitoId);
      return sig;
    });
    setBorradores((prev) =>
      prev[item.requisitoId]
        ? prev
        : {
            ...prev,
            [item.requisitoId]: {
              notas: item.notas ?? "",
              archivoUrl: item.archivoUrl ?? "",
            },
          },
    );
  };

  const cambiarBorrador = (requisitoId, campo, valor) =>
    setBorradores((prev) => ({
      ...prev,
      [requisitoId]: { ...prev[requisitoId], [campo]: valor },
    }));

  const guardarDetalle = async (item) => {
    const b = borradores[item.requisitoId];
    if ((b.archivoUrl ?? "").trim().length > 500) {
      toast.error("El enlace del documento admite máximo 500 caracteres.");
      return;
    }
    const ok = await actualizar(item, {
      notas: o(b.notas),
      archivoUrl: o(b.archivoUrl),
    });
    if (ok) toast.success("Detalle guardado.");
  };

  // ---------- Formalizar ----------
  const formalizar = async (ev) => {
    ev.preventDefault();
    setFormalizando(true);
    try {
      const { data: respuesta } = await api.post(
        `/matriculas/${id}/formalizar`,
        { numeroConstancia: null },
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
        <h1>Inscripción</h1>
        <div className="mod-heading-actions">
          <Link to="/matricula" className="btn-secondary">
            <Icon icon={ICONOS.volver} className="icono-inline" /> Volver a
            Matrícula
          </Link>
        </div>
      </div>

      {/* Datos del estudiante */}
      <section className="ins-tarjeta ins-cabecera">
        <div>
          <h2>{data.estudianteNombreCompleto}</h2>
          <p className="ins-sub">
            {seccion
              ? `${seccion.gradoNombre ?? ""} · Sección ${seccion.letraSeccion}`
              : "Sección —"}{" "}
            · Año escolar {data.anioEscolar}
          </p>
        </div>
        <div className="ins-cabecera-datos">
          <span className={`badge mat-estado--${data.estadoMatricula}`}>
            {ETIQUETA_ESTADO[data.estadoMatricula] ?? data.estadoMatricula}
          </span>
          <small>Solicitud: {formatFechaISO(data.fechaSolicitud)}</small>
        </div>
      </section>

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
              const b = borradores[item.requisitoId];
              const ocupado = guardandoId === item.requisitoId;
              return (
                <li
                  key={item.requisitoId}
                  className={`ins-item${item.cumplido ? " cumplido" : ""}`}
                >
                  <div className="ins-item-fila">
                    <label className="ins-check">
                      <input
                        type="checkbox"
                        checked={!!item.cumplido}
                        disabled={!editable || ocupado}
                        onChange={() =>
                          actualizar(item, { cumplido: !item.cumplido })
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
                      onClick={() => alternarDetalle(item)}
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
                          disabled={!editable}
                          onChange={(e) =>
                            cambiarBorrador(
                              item.requisitoId,
                              "notas",
                              e.target.value,
                            )
                          }
                          placeholder="Observaciones sobre este requisito"
                        />
                      </label>
                      <label>
                        Enlace del documento
                        <input
                          value={b.archivoUrl}
                          disabled={!editable}
                          onChange={(e) =>
                            cambiarBorrador(
                              item.requisitoId,
                              "archivoUrl",
                              e.target.value,
                            )
                          }
                          placeholder="https://..."
                        />
                      </label>
                      {editable && (
                        <div className="ins-detalle-acciones">
                          <button
                            type="button"
                            className="btn-primary"
                            disabled={ocupado}
                            onClick={() => guardarDetalle(item)}
                          >
                            {ocupado ? "Guardando..." : "Guardar detalle"}
                          </button>
                        </div>
                      )}
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
            <p>Formalizada el {formatFechaISO(data.fechaFormalizacion)}.</p>
          </div>
          <div className="ins-acciones">
            <button
              type="button"
              className="btn-secondary"
              disabled={descargando === "cupo"}
              onClick={() => descargar("cupo")}
            >
              {descargando === "cupo" ? "Descargando..." : "Constancia de cupo"}
            </button>
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
            <button
              type="button"
              className="btn-secondary"
              disabled={descargando === "cupo"}
              onClick={() => descargar("cupo")}
            >
              {descargando === "cupo" ? "Descargando..." : "Constancia de cupo"}
            </button>
            <button
              type="button"
              className="btn-primary"
              disabled={faltan > 0}
              onClick={() => setModalFormalizar(true)}
            >
              Formalizar inscripción
            </button>
          </div>
        </section>
      ) : null}

      {modalFormalizar && (
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
        </ModalFormulario>
      )}
    </div>
  );
}

export default Inscripcion;
