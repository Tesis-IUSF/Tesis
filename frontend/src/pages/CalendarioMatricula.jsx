import { useCallback, useEffect, useState } from "react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { soloDigitos } from "../utils/validacionesEntrada";
import ModalFormulario from "../components/ModalFormulario";
import ConfirmDialog from "../components/ConfirmDialog";
import "./CalendarioMatricula.css";

const TIPOS_PERIODO = [
  { value: "preinscripcion", label: "Preinscripción" },
  { value: "inscripcion", label: "Inscripción" },
  { value: "extemporanea", label: "Extemporánea" },
];

const ANIO_VACIO = {
  anio: "",
  nombre: "",
  fechaInicio: "",
  fechaFin: "",
};

const PERIODO_VACIO = {
  tipo: "preinscripcion",
  fechaInicio: "",
  fechaFin: "",
};

const fechaLocalISO = () => {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}-${String(hoy.getDate()).padStart(2, "0")}`;
};

const formatearFecha = (fecha) => {
  if (!fecha) return "—";
  const [anio, mes, dia] = String(fecha).split("-");
  return `${dia}/${mes}/${anio}`;
};

const nombreTipoPeriodo = (tipo) =>
  TIPOS_PERIODO.find((periodo) => periodo.value === tipo)?.label ?? tipo;

function CalendarioMatricula() {
  const toast = useToast();
  const [anios, setAnios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [modalAnio, setModalAnio] = useState(null);
  const [formAnio, setFormAnio] = useState(ANIO_VACIO);
  const [modalPeriodo, setModalPeriodo] = useState(null);
  const [formPeriodo, setFormPeriodo] = useState(PERIODO_VACIO);
  const [periodoAEliminar, setPeriodoAEliminar] = useState(null);
  const [anioAActivar, setAnioAActivar] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [accionId, setAccionId] = useState(null);

  const cargarAnios = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      const { data } = await api.get("/matriculas/gestion/anios");
      setAnios(data);
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudieron cargar los años escolares."),
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void cargarAnios(), 0);
    return () => window.clearTimeout(timer);
  }, [cargarAnios]);

  const abrirCrearAnio = () => {
    setFormAnio(ANIO_VACIO);
    setModalAnio({ modo: "crear" });
  };

  const abrirEditarAnio = (anio) => {
    setFormAnio({
      anio: String(anio.anio),
      nombre: anio.nombre,
      fechaInicio: anio.fechaInicio,
      fechaFin: anio.fechaFin,
    });
    setModalAnio({ modo: "editar", anio: anio.anio });
  };

  const guardarAnio = async (event) => {
    event.preventDefault();
    const anio = Number(formAnio.anio);
    if (!Number.isInteger(anio) || anio < 1900 || anio > 2200) {
      toast.error("El año escolar debe ser un entero entre 1900 y 2200.");
      return;
    }
    if (
      !formAnio.nombre.trim() ||
      !formAnio.fechaInicio ||
      !formAnio.fechaFin ||
      formAnio.fechaFin < formAnio.fechaInicio
    ) {
      toast.error("Completa los campos y verifica el rango de fechas del año.");
      return;
    }

    setGuardando(true);
    const payload = {
      anio,
      nombre: formAnio.nombre.trim(),
      fechaInicio: formAnio.fechaInicio,
      fechaFin: formAnio.fechaFin,
    };
    try {
      if (modalAnio.modo === "editar") {
        await api.put(
          `/matriculas/gestion/anios/${modalAnio.anio}`,
          payload,
        );
        toast.success("Año escolar actualizado correctamente.");
      } else {
        const { data } = await api.post("/matriculas/gestion/anios", payload);
        toast.success(
          data.sugeridoDesdeAnio
            ? `Año escolar creado. Se copiaron períodos del año ${data.sugeridoDesdeAnio}; revisa sus fechas.`
            : "Año escolar creado correctamente.",
        );
      }
      setModalAnio(null);
      await cargarAnios();
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo guardar el año escolar."),
      );
    } finally {
      setGuardando(false);
    }
  };

  const abrirCrearPeriodo = (anioEscolar) => {
    setFormPeriodo(PERIODO_VACIO);
    setModalPeriodo({ modo: "crear", anio: anioEscolar });
  };

  const abrirEditarPeriodo = (periodo) => {
    setFormPeriodo({
      tipo: periodo.tipo,
      fechaInicio: periodo.fechaInicio,
      fechaFin: periodo.fechaFin,
    });
    setModalPeriodo({
      modo: "editar",
      id: periodo.id,
      anio: periodo.anioEscolar,
    });
  };

  const guardarPeriodo = async (event) => {
    event.preventDefault();
    if (
      !formPeriodo.tipo ||
      !formPeriodo.fechaInicio ||
      !formPeriodo.fechaFin ||
      formPeriodo.fechaFin < formPeriodo.fechaInicio
    ) {
      toast.error("Completa los campos y verifica el rango de fechas.");
      return;
    }

    setGuardando(true);
    try {
      if (modalPeriodo.modo === "editar") {
        await api.put(
          `/matriculas/gestion/periodos/${modalPeriodo.id}`,
          formPeriodo,
        );
        toast.success("Período actualizado correctamente.");
      } else {
        await api.post(
          `/matriculas/gestion/anios/${modalPeriodo.anio}/periodos`,
          formPeriodo,
        );
        toast.success("Período creado correctamente.");
      }
      setModalPeriodo(null);
      await cargarAnios();
    } catch (err) {
      toast.error(
        extractErrorMessage(
          err,
          "No se pudo guardar el período. Revisa que no se solape con otro del mismo tipo.",
        ),
      );
    } finally {
      setGuardando(false);
    }
  };

  const activarAnio = async () => {
    if (!anioAActivar) return;
    setAccionId(`anio-${anioAActivar.anio}`);
    try {
      await api.post(
        `/matriculas/gestion/anios/${anioAActivar.anio}/activar`,
      );
      toast.success(
        `${anioAActivar.nombre} está activo. El año activo anterior quedó cerrado.`,
      );
      setAnioAActivar(null);
      await cargarAnios();
    } catch (err) {
      toast.error(
        extractErrorMessage(
          err,
          "No se pudo activar el año. Verifica sus fechas y que tenga períodos activos de cada tipo.",
        ),
      );
    } finally {
      setAccionId(null);
    }
  };

  const alternarPeriodo = async (periodo) => {
    setAccionId(`periodo-${periodo.id}`);
    try {
      const accion = periodo.activo ? "desactivar" : "activar";
      await api.patch(`/matriculas/gestion/periodos/${periodo.id}/${accion}`);
      toast.success(`Período ${periodo.activo ? "desactivado" : "activado"}.`);
      await cargarAnios();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo cambiar el estado del período."));
    } finally {
      setAccionId(null);
    }
  };

  const eliminarPeriodo = async () => {
    if (!periodoAEliminar) return;
    setAccionId(`periodo-${periodoAEliminar.id}`);
    try {
      await api.delete(`/matriculas/gestion/periodos/${periodoAEliminar.id}`);
      toast.success("Período eliminado correctamente.");
      setPeriodoAEliminar(null);
      await cargarAnios();
    } catch (err) {
      toast.error(
        extractErrorMessage(
          err,
          "Solo se pueden eliminar períodos de años futuros que aún no se hayan utilizado.",
        ),
      );
    } finally {
      setAccionId(null);
    }
  };

  return (
    <main className="mod-main calendario-matricula">
      <header className="mod-heading">
        <div>
          <h1>Calendario de matrícula</h1>
          <p className="calendario-intro">
            Administra años escolares y los períodos de preinscripción,
            inscripción y atención extemporánea.
          </p>
        </div>
        <button type="button" className="btn-primary" onClick={abrirCrearAnio}>
          + Crear año escolar
        </button>
      </header>

      <p className="calendario-nota">
        Para activar un año, debe tener al menos un período activo de cada tipo.
        Al activar otro año, el actual se desactiva automáticamente.
      </p>

      {error && <p className="error-msg" role="alert">{error}</p>}

      {cargando ? (
        <p className="tabla-estado">Cargando calendario de matrícula...</p>
      ) : anios.length === 0 && !error ? (
        <div className="calendario-vacio">
          <h2>No hay años escolares registrados</h2>
          <p>Crea el primer año escolar para configurar sus períodos.</p>
        </div>
      ) : (
        <div className="calendario-anios">
          {anios.map((anio) => (
            <section className="calendario-anio" key={anio.anio}>
              <header className="calendario-anio-cabecera">
                <div>
                  <div className="calendario-anio-titulo">
                    <h2>{anio.nombre}</h2>
                    <span
                      className={`calendario-estado ${anio.activo ? "activo" : ""}`}
                    >
                      {anio.activo ? "Activo" : "Inactivo"}
                    </span>
                  </div>
                  <p>
                    {formatearFecha(anio.fechaInicio)} —{" "}
                    {formatearFecha(anio.fechaFin)}
                  </p>
                </div>
                <div className="calendario-acciones">
                  <button
                    type="button"
                    className="btn-edit"
                    onClick={() => abrirEditarAnio(anio)}
                  >
                    Editar año
                  </button>
                  {!anio.activo && (
                    <button
                      type="button"
                      className="btn-reactivar"
                      disabled={accionId === `anio-${anio.anio}`}
                      onClick={() => setAnioAActivar(anio)}
                    >
                      {accionId === `anio-${anio.anio}` ? "Activando..." : "Activar"}
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn-primary"
                    onClick={() => abrirCrearPeriodo(anio.anio)}
                  >
                    + Agregar período
                  </button>
                </div>
              </header>

              {anio.periodos?.length ? (
                <div className="table-container">
                  <table>
                    <thead>
                      <tr>
                        <th>Tipo</th>
                        <th>Desde</th>
                        <th>Hasta</th>
                        <th>Estado</th>
                        <th>Acciones</th>
                      </tr>
                    </thead>
                    <tbody>
                      {anio.periodos.map((periodo) => {
                        const ocupado = accionId === `periodo-${periodo.id}`;
                        const puedeEliminar =
                          !anio.activo && anio.fechaInicio > fechaLocalISO();
                        return (
                          <tr key={periodo.id}>
                            <td data-label="Tipo">
                              {nombreTipoPeriodo(periodo.tipo)}
                            </td>
                            <td data-label="Desde">
                              {formatearFecha(periodo.fechaInicio)}
                            </td>
                            <td data-label="Hasta">
                              {formatearFecha(periodo.fechaFin)}
                            </td>
                            <td data-label="Estado">
                              <span
                                className={`calendario-estado ${periodo.activo ? "activo" : ""}`}
                              >
                                {periodo.activo ? "Activo" : "Inactivo"}
                              </span>
                            </td>
                            <td data-label="Acciones">
                              <button
                                type="button"
                                className="btn-edit"
                                onClick={() => abrirEditarPeriodo(periodo)}
                              >
                                Editar
                              </button>
                              <button
                                type="button"
                                className={periodo.activo ? "btn-delete" : "btn-reactivar"}
                                disabled={ocupado}
                                onClick={() => alternarPeriodo(periodo)}
                              >
                                {periodo.activo ? "Desactivar" : "Activar"}
                              </button>
                              {puedeEliminar && (
                                <button
                                  type="button"
                                  className="btn-delete"
                                  disabled={ocupado}
                                  onClick={() => setPeriodoAEliminar(periodo)}
                                >
                                  Eliminar
                                </button>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="calendario-periodos-vacios">
                  Aún no hay períodos configurados para este año.
                </p>
              )}
            </section>
          ))}
        </div>
      )}

      {modalAnio && (
        <ModalFormulario
          titulo={modalAnio.modo === "crear" ? "Crear año escolar" : "Editar año escolar"}
          onCerrar={() => setModalAnio(null)}
          onSubmit={guardarAnio}
          guardando={guardando}
          etiquetaGuardar={modalAnio.modo === "crear" ? "Crear año" : "Guardar cambios"}
        >
          <label>
            Número del año escolar *
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={formAnio.anio}
              disabled={modalAnio.modo === "editar"}
              required
              onChange={(event) =>
                setFormAnio((actual) => ({
                  ...actual,
                  anio: soloDigitos(event.target.value).slice(0, 4),
                }))
              }
            />
          </label>
          <label>
            Nombre *
            <input
              maxLength={20}
              value={formAnio.nombre}
              required
              placeholder="Ej.: 2026-2027"
              onChange={(event) =>
                setFormAnio((actual) => ({ ...actual, nombre: event.target.value }))
              }
            />
          </label>
          <label>
            Fecha de inicio *
            <input
              type="date"
              value={formAnio.fechaInicio}
              required
              onChange={(event) =>
                setFormAnio((actual) => ({
                  ...actual,
                  fechaInicio: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Fecha de cierre *
            <input
              type="date"
              value={formAnio.fechaFin}
              min={formAnio.fechaInicio || undefined}
              required
              onChange={(event) =>
                setFormAnio((actual) => ({ ...actual, fechaFin: event.target.value }))
              }
            />
          </label>
          {modalAnio.modo === "crear" && (
            <p className="calendario-modal-ayuda">
              Si existe un año anterior con períodos activos, el sistema
              propondrá copiarlos desplazando las fechas un año.
            </p>
          )}
        </ModalFormulario>
      )}

      {modalPeriodo && (
        <ModalFormulario
          titulo={modalPeriodo.modo === "crear" ? "Agregar período" : "Editar período"}
          onCerrar={() => setModalPeriodo(null)}
          onSubmit={guardarPeriodo}
          guardando={guardando}
          etiquetaGuardar={modalPeriodo.modo === "crear" ? "Agregar período" : "Guardar cambios"}
        >
          <p className="calendario-modal-ayuda">
            Año escolar: {anios.find((anio) => anio.anio === modalPeriodo.anio)?.nombre ?? modalPeriodo.anio}
          </p>
          <label>
            Tipo de período *
            <select
              value={formPeriodo.tipo}
              required
              onChange={(event) =>
                setFormPeriodo((actual) => ({ ...actual, tipo: event.target.value }))
              }
            >
              {TIPOS_PERIODO.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>
                  {tipo.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Fecha de inicio *
            <input
              type="date"
              value={formPeriodo.fechaInicio}
              required
              onChange={(event) =>
                setFormPeriodo((actual) => ({
                  ...actual,
                  fechaInicio: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Fecha de cierre *
            <input
              type="date"
              value={formPeriodo.fechaFin}
              min={formPeriodo.fechaInicio || undefined}
              required
              onChange={(event) =>
                setFormPeriodo((actual) => ({
                  ...actual,
                  fechaFin: event.target.value,
                }))
              }
            />
          </label>
          <p className="calendario-modal-ayuda">
            No se permiten períodos del mismo tipo con fechas superpuestas.
          </p>
        </ModalFormulario>
      )}

      <ConfirmDialog
        open={Boolean(anioAActivar)}
        title="Activar año escolar"
        message={
          anioAActivar
            ? `¿Activar ${anioAActivar.nombre}? El año activo actual será desactivado.`
            : ""
        }
        onConfirm={activarAnio}
        onCancel={() => setAnioAActivar(null)}
        cargando={accionId === `anio-${anioAActivar?.anio}`}
        confirmText="Sí, activar año"
        confirmLoadingText="Activando..."
      />

      <ConfirmDialog
        open={Boolean(periodoAEliminar)}
        title="Eliminar período"
        message={
          periodoAEliminar
            ? `¿Eliminar el período de ${nombreTipoPeriodo(periodoAEliminar.tipo)} del año ${periodoAEliminar.anioEscolar}? Solo se puede eliminar si el año es futuro y aún no tiene matrículas.`
            : ""
        }
        onConfirm={eliminarPeriodo}
        onCancel={() => setPeriodoAEliminar(null)}
        cargando={accionId === `periodo-${periodoAEliminar?.id}`}
      />
    </main>
  );
}

export default CalendarioMatricula;
