import { useCallback, useEffect, useMemo, useState } from "react";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { soloDigitos } from "../utils/validacionesEntrada";
import { ICONOS } from "../utils/iconos";
import ModalFormulario from "../components/ModalFormulario";

const VACIO = {
  gradoId: "",
  letraSeccion: "",
  capacidadMaxima: "40",
  docentePrincipalId: "",
  turnoId: "",
  anioEscolar: "",
  activo: true,
};

function nombreGrado(grado) {
  return grado.nombreEspecial?.trim() || `${grado.numeroGrado}° grado`;
}

function Secciones() {
  const toast = useToast();
  const [secciones, setSecciones] = useState([]);
  const [grados, setGrados] = useState([]);
  const [anios, setAnios] = useState([]);
  const [turnos, setTurnos] = useState([]);
  const [docentes, setDocentes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [filtroGrado, setFiltroGrado] = useState("");
  const [filtroAnio, setFiltroAnio] = useState("");
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      const [resSecciones, resGrados, resAnios, resTurnos, resDocentes] =
        await Promise.all([
          api.get("/catalogos/secciones", { params: { page: 0, size: 100 } }),
          api.get("/catalogos/grados", { params: { page: 0, size: 100 } }),
          api.get("/matriculas/gestion/anios"),
          api.get("/catalogos/turnos", { params: { page: 0, size: 100 } }),
          api.get("/catalogos/docentes"),
        ]);
      setSecciones(resSecciones.data.content);
      setGrados(resGrados.data.content);
      setAnios(resAnios.data);
      setTurnos(resTurnos.data.content);
      setDocentes(resDocentes.data);
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudieron cargar las secciones."),
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void cargar(), 0);
    return () => window.clearTimeout(timer);
  }, [cargar]);

  const gradosPorId = useMemo(
    () => new Map(grados.map((grado) => [String(grado.id), grado])),
    [grados],
  );

  const visibles = useMemo(
    () =>
      secciones.filter(
        (seccion) =>
          (!filtroGrado || String(seccion.gradoId) === filtroGrado) &&
          (!filtroAnio || String(seccion.anioEscolar) === filtroAnio),
      ),
    [secciones, filtroGrado, filtroAnio],
  );

  const abrirNuevo = () => {
    setErrores({});
    setForm({
      ...VACIO,
      gradoId: String(grados.find((grado) => grado.activo)?.id ?? ""),
      anioEscolar: String(anios.find((anio) => anio.activo)?.anio ?? ""),
    });
    setModal({ id: null, docentePrincipalNombre: null });
  };

  const abrirEditar = (seccion) => {
    setErrores({});
    setForm({
      gradoId: String(seccion.gradoId),
      letraSeccion: seccion.letraSeccion ?? "",
      capacidadMaxima: String(seccion.capacidadMaxima ?? 40),
      docentePrincipalId: String(seccion.docentePrincipalId ?? ""),
      turnoId: String(seccion.turnoId ?? ""),
      anioEscolar: String(seccion.anioEscolar),
      activo: Boolean(seccion.activo),
    });
    setModal({
      id: seccion.id,
      docentePrincipalNombre: seccion.docentePrincipalNombre,
    });
  };

  const actualizarCampo = (campo, valor) => {
    setForm((actual) => ({ ...actual, [campo]: valor }));
    setErrores((actual) => ({ ...actual, [campo]: undefined }));
  };

  const guardar = async (event) => {
    event.preventDefault();
    const nuevosErrores = {};
    const letraSeccion = form.letraSeccion.trim().toUpperCase();
    const capacidadMaxima = Number(form.capacidadMaxima);

    if (!form.gradoId) nuevosErrores.gradoId = "Selecciona un grado.";
    if (!/^[A-Z]$/.test(letraSeccion)) {
      nuevosErrores.letraSeccion = "Indica una sola letra.";
    }
    if (
      !Number.isInteger(capacidadMaxima) ||
      capacidadMaxima < 1 ||
      capacidadMaxima > 60
    ) {
      nuevosErrores.capacidadMaxima =
        "La capacidad debe estar entre 1 y 60 estudiantes.";
    }
    if (!form.anioEscolar) nuevosErrores.anioEscolar = "Selecciona un año escolar.";

    const duplicada = secciones.some(
      (seccion) =>
        seccion.id !== modal.id &&
        String(seccion.gradoId) === form.gradoId &&
        String(seccion.anioEscolar) === form.anioEscolar &&
        seccion.letraSeccion?.toUpperCase() === letraSeccion,
    );
    if (duplicada) {
      nuevosErrores.letraSeccion =
        "Ya existe esa letra para el grado y año escolar seleccionados.";
    }
    const grado = gradosPorId.get(form.gradoId);
    if (form.activo && grado && !grado.activo) {
      nuevosErrores.gradoId = "No se puede activar una sección de un grado inactivo.";
    }

    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length) return;

    const seccionActual = secciones.find((seccion) => seccion.id === modal.id);
    if (
      seccionActual?.activo &&
      !form.activo &&
      !window.confirm(
        "¿Desactivar esta sección? No es posible hacerlo si tiene matrículas activas en el año escolar vigente.",
      )
    ) {
      return;
    }

    const payload = {
      gradoId: Number(form.gradoId),
      letraSeccion,
      capacidadMaxima,
      docentePrincipalId: form.docentePrincipalId
        ? Number(form.docentePrincipalId)
        : null,
      turnoId: form.turnoId ? Number(form.turnoId) : null,
      anioEscolar: Number(form.anioEscolar),
      activo: form.activo,
    };
    setGuardando(true);
    try {
      if (modal.id) {
        await api.put(`/catalogos/secciones/${modal.id}`, payload);
        toast.success("Sección actualizada correctamente.");
      } else {
        await api.post("/catalogos/secciones", payload);
        toast.success("Sección creada correctamente.");
      }
      setModal(null);
      await cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo guardar la sección."));
    } finally {
      setGuardando(false);
    }
  };

  const cambiarEstado = async (seccion) => {
    const activo = !seccion.activo;
    if (
      !activo &&
      !window.confirm(
        "¿Desactivar esta sección? No es posible hacerlo si tiene matrículas activas en el año escolar vigente.",
      )
    ) {
      return;
    }
    try {
      await api.put(`/catalogos/secciones/${seccion.id}`, {
        gradoId: seccion.gradoId,
        letraSeccion: seccion.letraSeccion,
        capacidadMaxima: seccion.capacidadMaxima,
        docentePrincipalId: seccion.docentePrincipalId,
        turnoId: seccion.turnoId,
        anioEscolar: seccion.anioEscolar,
        activo,
      });
      toast.success(`Sección ${activo ? "activada" : "desactivada"} correctamente.`);
      await cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo cambiar el estado."));
    }
  };

  const eliminar = async (seccion) => {
    if (
      !window.confirm(
        `¿Eliminar la sección ${seccion.letraSeccion} del año ${seccion.anioEscolar}? Solo se permite si no tiene matrículas asociadas en ningún año.`,
      )
    ) {
      return;
    }
    try {
      await api.delete(`/catalogos/secciones/${seccion.id}`);
      toast.success("Sección eliminada correctamente.");
      await cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo eliminar la sección."));
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Secciones</h1>
          <div className="mod-heading-actions">
            <button
              type="button"
              className="btn-primary"
              onClick={abrirNuevo}
              disabled={
                !grados.some((grado) => grado.activo) || anios.length === 0
              }
            >
              + Crear sección
            </button>
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}
        {!cargando && anios.length === 0 && (
          <p className="error-msg">
            Registra un año escolar en el calendario de matrícula antes de crear
            secciones.
          </p>
        )}

        <div className="filters-bar">
          <label>
            Grado
            <select
              value={filtroGrado}
              onChange={(event) => setFiltroGrado(event.target.value)}
            >
              <option value="">Todos</option>
              {grados.map((grado) => (
                <option key={grado.id} value={grado.id}>
                  {grado.nivelEducativoNombre} · {nombreGrado(grado)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Año escolar
            <select
              value={filtroAnio}
              onChange={(event) => setFiltroAnio(event.target.value)}
            >
              <option value="">Todos</option>
              {anios.map((anio) => (
                <option key={anio.anio} value={anio.anio}>
                  {anio.nombre} {anio.activo ? "(vigente)" : ""}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Grado</th>
                <th>Sección</th>
                <th>Año escolar</th>
                <th>Docente</th>
                <th>Turno</th>
                <th>Cupos</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={8} className="tabla-estado">
                    Cargando secciones...
                  </td>
                </tr>
              ) : visibles.length === 0 ? (
                <tr>
                  <td colSpan={8} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.seccion} />
                    </span>
                    No hay secciones para los filtros seleccionados.
                  </td>
                </tr>
              ) : (
                visibles.map((seccion) => {
                  const grado = gradosPorId.get(String(seccion.gradoId));
                  const turno = turnos.find(
                    (item) => item.id === seccion.turnoId,
                  );
                  return (
                    <tr key={seccion.id}>
                      <td data-label="Grado">
                        {grado
                          ? `${grado.nivelEducativoNombre} · ${nombreGrado(grado)}`
                          : seccion.gradoNombre || "—"}
                      </td>
                      <td data-label="Sección">{seccion.letraSeccion}</td>
                      <td data-label="Año escolar">{seccion.anioEscolar}</td>
                      <td data-label="Docente">
                        {seccion.docentePrincipalNombre || "Sin asignar"}
                      </td>
                      <td data-label="Turno">{turno?.nombre || "Sin asignar"}</td>
                      <td data-label="Cupos">
                        {seccion.matriculados ?? 0}/{seccion.capacidadMaxima}
                        {" · "}
                        {seccion.cuposDisponibles ?? 0} disponibles
                      </td>
                      <td data-label="Estado">
                        {seccion.activo ? "Activa" : "Inactiva"}
                      </td>
                      <td data-label="Acciones">
                        <button
                          type="button"
                          className="btn-edit"
                          onClick={() => abrirEditar(seccion)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className="btn-secondary"
                          onClick={() => cambiarEstado(seccion)}
                        >
                          {seccion.activo ? "Desactivar" : "Activar"}
                        </button>
                        <button
                          type="button"
                          className="btn-delete"
                          onClick={() => eliminar(seccion)}
                        >
                          Eliminar
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {modal && (
        <ModalFormulario
          titulo={modal.id ? "Editar sección" : "Crear sección"}
          onCerrar={() => setModal(null)}
          onSubmit={guardar}
          guardando={guardando}
          etiquetaGuardar={modal.id ? "Guardar cambios" : "Crear sección"}
        >
          <label>
            Grado *
            <select
              value={form.gradoId}
              onChange={(event) => actualizarCampo("gradoId", event.target.value)}
            >
              <option value="">Selecciona...</option>
              {grados.map((grado) => (
                <option key={grado.id} value={grado.id}>
                  {grado.nivelEducativoNombre} · {nombreGrado(grado)}
                  {!grado.activo ? " (inactivo)" : ""}
                </option>
              ))}
            </select>
            {errores.gradoId && (
              <span className="campo-error">{errores.gradoId}</span>
            )}
          </label>
          <div className="form-grid-2">
            <label>
              Letra de sección *
              <input
                maxLength={1}
                value={form.letraSeccion}
                onChange={(event) =>
                  actualizarCampo("letraSeccion", event.target.value)
                }
                placeholder="A"
              />
              {errores.letraSeccion && (
                <span className="campo-error">{errores.letraSeccion}</span>
              )}
            </label>
            <label>
              Capacidad máxima *
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={form.capacidadMaxima}
                onChange={(event) =>
                  actualizarCampo(
                    "capacidadMaxima",
                    soloDigitos(event.target.value),
                  )
                }
              />
              {errores.capacidadMaxima && (
                <span className="campo-error">{errores.capacidadMaxima}</span>
              )}
            </label>
            <label>
              Año escolar *
              <select
                value={form.anioEscolar}
                onChange={(event) =>
                  actualizarCampo("anioEscolar", event.target.value)
                }
              >
                <option value="">Selecciona...</option>
                {anios.map((anio) => (
                  <option key={anio.anio} value={anio.anio}>
                    {anio.nombre} {anio.activo ? "(vigente)" : ""}
                  </option>
                ))}
              </select>
              {errores.anioEscolar && (
                <span className="campo-error">{errores.anioEscolar}</span>
              )}
            </label>
            <label>
              Docente principal
              <select
                value={form.docentePrincipalId}
                onChange={(event) =>
                  actualizarCampo("docentePrincipalId", event.target.value)
                }
              >
                <option value="">Sin asignar</option>
                {modal.docentePrincipalNombre &&
                  form.docentePrincipalId &&
                  !docentes.some(
                    (docente) =>
                      String(docente.id) === form.docentePrincipalId,
                  ) && (
                    <option value={form.docentePrincipalId}>
                      {modal.docentePrincipalNombre} (asignación existente)
                    </option>
                  )}
                {docentes.map((docente) => (
                  <option key={docente.id} value={docente.id}>
                    {docente.apellido}, {docente.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Turno
              <select
                value={form.turnoId}
                onChange={(event) =>
                  actualizarCampo("turnoId", event.target.value)
                }
              >
                <option value="">Sin asignar</option>
                {turnos.map((turno) => (
                  <option key={turno.id} value={turno.id}>
                    {turno.nombre}
                    {!turno.activo ? " (inactivo)" : ""}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={form.activo}
              onChange={(event) => actualizarCampo("activo", event.target.checked)}
            />
            Activa
          </label>
        </ModalFormulario>
      )}
    </>
  );
}

export default Secciones;
