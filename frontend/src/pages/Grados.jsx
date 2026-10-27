import AnimatedSelect from "../components/AnimatedSelect";
import { useCallback, useEffect, useState } from "react";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { soloDigitos } from "../utils/validacionesEntrada";
import { ICONOS } from "../utils/iconos";
import ModalFormulario from "../components/ModalFormulario";

const VACIO = {
  nivelEducativoId: "",
  numeroGrado: "",
  edadMinima: "",
  edadMaxima: "",
  nombreEspecial: "",
  activo: true,
};

function nombreGrado(grado) {
  return grado.nombreEspecial?.trim() || `${grado.numeroGrado}° grado`;
}

function Grados() {
  const toast = useToast();
  const [grados, setGrados] = useState([]);
  const [niveles, setNiveles] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      const [resGrados, resNiveles] = await Promise.all([
        api.get("/catalogos/grados", { params: { page: 0, size: 100 } }),
        api.get("/catalogos/niveles-educativos", {
          params: { page: 0, size: 100 },
        }),
      ]);
      setGrados(resGrados.data.content);
      setNiveles(
        [...resNiveles.data.content].sort(
          (a, b) => (a.ordinal ?? 999) - (b.ordinal ?? 999),
        ),
      );
    } catch (err) {
      setError(extractErrorMessage(err, "No se pudieron cargar los grados."));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void cargar(), 0);
    return () => window.clearTimeout(timer);
  }, [cargar]);

  const abrirNuevo = () => {
    setErrores({});
    setForm({
      ...VACIO,
      nivelEducativoId: String(niveles.find((nivel) => nivel.activo)?.id ?? ""),
    });
    setModal({ id: null });
  };

  const abrirEditar = (grado) => {
    setErrores({});
    setForm({
      nivelEducativoId: String(grado.nivelEducativoId),
      numeroGrado: String(grado.numeroGrado),
      edadMinima: String(grado.edadMinima),
      edadMaxima: String(grado.edadMaxima),
      nombreEspecial: grado.nombreEspecial ?? "",
      activo: Boolean(grado.activo),
    });
    setModal({ id: grado.id });
  };

  const actualizarCampo = (campo, valor) => {
    setForm((actual) => ({ ...actual, [campo]: valor }));
    setErrores((actual) => ({ ...actual, [campo]: undefined }));
  };

  const guardar = async (event) => {
    event.preventDefault();
    const nuevosErrores = {};
    const numeroGrado = Number(form.numeroGrado);
    const edadMinima = Number(form.edadMinima);
    const edadMaxima = Number(form.edadMaxima);

    if (!form.nivelEducativoId) {
      nuevosErrores.nivelEducativoId = "Selecciona un nivel educativo.";
    }
    if (
      !Number.isInteger(numeroGrado) ||
      numeroGrado < 1 ||
      numeroGrado > 12
    ) {
      nuevosErrores.numeroGrado =
        "El número de grado debe estar entre 1 y 12.";
    }
    if (
      !form.edadMinima ||
      !Number.isInteger(edadMinima) ||
      edadMinima < 1 ||
      edadMinima > 20
    ) {
      nuevosErrores.edadMinima = "Indica una edad entre 1 y 20 años.";
    }
    if (
      !form.edadMaxima ||
      !Number.isInteger(edadMaxima) ||
      edadMaxima < 1 ||
      edadMaxima > 20
    ) {
      nuevosErrores.edadMaxima = "Indica una edad entre 1 y 20 años.";
    } else if (edadMaxima < edadMinima) {
      nuevosErrores.edadMaxima = "La edad máxima debe ser igual o mayor.";
    }
    if (form.nombreEspecial.trim().length > 100) {
      nuevosErrores.nombreEspecial = "Máximo 100 caracteres.";
    }
    const repetido = grados.some(
      (grado) =>
        grado.id !== modal.id &&
        String(grado.nivelEducativoId) === form.nivelEducativoId &&
        Number(grado.numeroGrado) === numeroGrado,
    );
    if (repetido) {
      nuevosErrores.numeroGrado =
        "Ya existe ese número de grado en el nivel seleccionado.";
    }
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length) return;
    const gradoActual = grados.find((grado) => grado.id === modal.id);
    if (
      gradoActual?.activo &&
      !form.activo &&
      !window.confirm(
        "¿Desactivar este grado? También se desactivarán todas sus secciones.",
      )
    ) {
      return;
    }

    const payload = {
      nivelEducativoId: Number(form.nivelEducativoId),
      numeroGrado,
      edadMinima,
      edadMaxima,
      nombreEspecial: form.nombreEspecial.trim() || null,
      activo: form.activo,
    };
    setGuardando(true);
    try {
      if (modal.id) {
        await api.put(`/catalogos/grados/${modal.id}`, payload);
        toast.success(
          gradoActual?.activo && !form.activo
            ? "Grado y secciones desactivados correctamente."
            : "Grado actualizado correctamente.",
        );
      } else {
        await api.post("/catalogos/grados", payload);
        toast.success("Grado creado correctamente.");
      }
      setModal(null);
      await cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo guardar el grado."));
    } finally {
      setGuardando(false);
    }
  };

  const cambiarEstado = async (grado) => {
    const activo = !grado.activo;
    if (
      !activo &&
      !window.confirm(
        `¿Desactivar "${nombreGrado(grado)}"? También se desactivarán todas sus secciones.`,
      )
    ) {
      return;
    }
    try {
      await api.put(`/catalogos/grados/${grado.id}`, {
        nivelEducativoId: grado.nivelEducativoId,
        numeroGrado: grado.numeroGrado,
        edadMinima: grado.edadMinima,
        edadMaxima: grado.edadMaxima,
        nombreEspecial: grado.nombreEspecial,
        activo,
      });
      toast.success(
        activo
          ? "Grado activado. Sus secciones permanecen desactivadas."
          : "Grado y secciones desactivados correctamente.",
      );
      await cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo cambiar el estado."));
    }
  };

  const eliminar = async (grado) => {
    if (!window.confirm(`¿Eliminar el grado "${nombreGrado(grado)}"?`)) return;
    try {
      await api.delete(`/catalogos/grados/${grado.id}`);
      toast.success("Grado eliminado correctamente.");
      await cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo eliminar el grado."));
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Grados</h1>
          <div className="mod-heading-actions">
            <button
              type="button"
              className="btn-primary"
              onClick={abrirNuevo}
              disabled={!niveles.some((nivel) => nivel.activo)}
            >
              + Crear grado
            </button>
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}
        {!cargando && niveles.length === 0 && (
          <p className="error-msg">
            No hay niveles educativos disponibles para asociar a los grados.
          </p>
        )}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nivel educativo</th>
                <th>Grado</th>
                <th>Rango de edad</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={5} className="tabla-estado">
                    Cargando grados...
                  </td>
                </tr>
              ) : grados.length === 0 ? (
                <tr>
                  <td colSpan={5} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.matricula} />
                    </span>
                    No hay grados registrados todavía.
                  </td>
                </tr>
              ) : (
                grados.map((grado) => (
                  <tr key={grado.id}>
                    <td data-label="Nivel educativo">
                      {grado.nivelEducativoNombre}
                    </td>
                    <td data-label="Grado">{nombreGrado(grado)}</td>
                    <td data-label="Rango de edad">
                      {grado.edadMinima}–{grado.edadMaxima} años
                    </td>
                    <td data-label="Estado">
                      {grado.activo ? "Activo" : "Inactivo"}
                    </td>
                    <td data-label="Acciones">
                      <button
                        type="button"
                        className="btn-edit"
                        onClick={() => abrirEditar(grado)}
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        className={`btn-estado ${grado.activo ? "btn-estado--desactivar" : "btn-estado--activar"}`}
                        onClick={() => cambiarEstado(grado)}
                      >
                        {grado.activo ? "Desactivar" : "Activar"}
                      </button>
                      <button
                        type="button"
                        className="btn-delete"
                        onClick={() => eliminar(grado)}
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {modal && (
        <ModalFormulario
          titulo={modal.id ? "Editar grado" : "Crear grado"}
          onCerrar={() => setModal(null)}
          onSubmit={guardar}
          guardando={guardando}
          etiquetaGuardar={modal.id ? "Guardar cambios" : "Crear grado"}
        >
          <label>
            Nivel educativo *
            <AnimatedSelect
              value={form.nivelEducativoId}
              onChange={(event) =>
                actualizarCampo("nivelEducativoId", event.target.value)
              }
            >
              <option value="">Selecciona...</option>
              {niveles.map((nivel) => (
                <option
                  key={nivel.id}
                  value={nivel.id}
                  disabled={!nivel.activo && String(nivel.id) !== form.nivelEducativoId}
                >
                  {nivel.nombre}
                  {!nivel.activo ? " (inactivo)" : ""}
                </option>
              ))}
            </AnimatedSelect>
            {errores.nivelEducativoId && (
              <span className="campo-error">{errores.nivelEducativoId}</span>
            )}
          </label>
          <div className="form-grid-2">
            <label>
              Número de grado *
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={form.numeroGrado}
                onChange={(event) =>
                  actualizarCampo(
                    "numeroGrado",
                    soloDigitos(event.target.value),
                  )
                }
              />
              {errores.numeroGrado && (
                <span className="campo-error">{errores.numeroGrado}</span>
              )}
            </label>
            <label>
              Nombre especial
              <input
                maxLength={100}
                value={form.nombreEspecial}
                onChange={(event) =>
                  actualizarCampo("nombreEspecial", event.target.value)
                }
                placeholder="Opcional"
              />
              {errores.nombreEspecial && (
                <span className="campo-error">{errores.nombreEspecial}</span>
              )}
            </label>
            <label>
              Edad mínima *
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={3}
                required
                value={form.edadMinima}
                onChange={(event) =>
                  actualizarCampo("edadMinima", soloDigitos(event.target.value))
                }
              />
              {errores.edadMinima && (
                <span className="campo-error">{errores.edadMinima}</span>
              )}
            </label>
            <label>
              Edad máxima *
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={3}
                required
                value={form.edadMaxima}
                onChange={(event) =>
                  actualizarCampo("edadMaxima", soloDigitos(event.target.value))
                }
              />
              {errores.edadMaxima && (
                <span className="campo-error">{errores.edadMaxima}</span>
              )}
            </label>
          </div>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={form.activo}
              onChange={(event) => actualizarCampo("activo", event.target.checked)}
            />
            Activo
          </label>
        </ModalFormulario>
      )}
    </>
  );
}

export default Grados;
