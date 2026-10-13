// src/pages/Departamentos.jsx
import { useState, useEffect, useCallback, useMemo } from "react";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import ModalFormulario from "../components/ModalFormulario";

function Departamentos() {
  const toast = useToast();
  const [departamentos, setDepartamentos] = useState([]);
  const [cargos, setCargos] = useState(null); // null = no se pudo cargar
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const [modal, setModal] = useState(null); // { id|null, nombre }
  const [errorNombre, setErrorNombre] = useState("");
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError("");
    const [deptos, listaCargos] = await Promise.allSettled([
      api.get("/catalogos/departamentos", { params: { page: 0, size: 100 } }),
      api.get("/catalogos/cargos", { params: { page: 0, size: 100 } }),
    ]);

    if (deptos.status === "fulfilled") {
      setDepartamentos(
        [...deptos.value.data.content].sort((a, b) =>
          a.nombre.localeCompare(b.nombre, "es"),
        ),
      );
    } else {
      setError(
        extractErrorMessage(
          deptos.reason,
          "No se pudo cargar la lista de departamentos.",
        ),
      );
    }
    // Los cargos solo sirven para contar y proteger el borrado
    setCargos(
      listaCargos.status === "fulfilled"
        ? listaCargos.value.data.content
        : null,
    );
    setCargando(false);
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const cargosPorDepartamento = useMemo(() => {
    if (!cargos) return null;
    const mapa = {};
    cargos.forEach((c) => {
      mapa[c.departamentoId] = (mapa[c.departamentoId] ?? 0) + 1;
    });
    return mapa;
  }, [cargos]);

  const abrirNuevo = () => {
    setErrorNombre("");
    setModal({ id: null, nombre: "" });
  };

  const abrirEditar = (d) => {
    setErrorNombre("");
    setModal({ id: d.id, nombre: d.nombre });
  };

  const cerrarModal = useCallback(() => setModal(null), []);

  const guardar = async (ev) => {
    ev.preventDefault();
    const nombre = modal.nombre.trim();
    if (!nombre) {
      setErrorNombre("El nombre es obligatorio.");
      return;
    }
    if (nombre.length > 100) {
      setErrorNombre("Máximo 100 caracteres.");
      return;
    }
    const repetido = departamentos.some(
      (d) =>
        d.id !== modal.id &&
        d.nombre.trim().toLowerCase() === nombre.toLowerCase(),
    );
    if (repetido) {
      setErrorNombre("Ya existe un departamento con ese nombre.");
      return;
    }

    setGuardando(true);
    try {
      if (modal.id) {
        await api.put(`/catalogos/departamentos/${modal.id}`, { nombre });
        toast.success("Departamento actualizado correctamente.");
      } else {
        await api.post("/catalogos/departamentos", { nombre });
        toast.success("Departamento creado correctamente.");
      }
      setModal(null);
      cargar();
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo guardar el departamento."),
      );
    } finally {
      setGuardando(false);
    }
  };

  const eliminar = async (d) => {
    if (!window.confirm(`¿Eliminar el departamento "${d.nombre}"?`)) return;
    try {
      await api.delete(`/catalogos/departamentos/${d.id}`);
      setDepartamentos((prev) => prev.filter((x) => x.id !== d.id));
      toast.success("Departamento eliminado correctamente.");
    } catch (err) {
      toast.error(
        extractErrorMessage(
          err,
          "No se pudo eliminar el departamento. Verifica que no tenga cargos ni empleados asociados.",
        ),
      );
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Departamentos</h1>
          <div className="mod-heading-actions">
            <button type="button" className="btn-primary" onClick={abrirNuevo}>
              + Crear departamento
            </button>
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Cargos</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={3} className="tabla-estado">
                    Cargando departamentos...
                  </td>
                </tr>
              ) : departamentos.length === 0 ? (
                <tr>
                  <td colSpan={3} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.departamentos} />
                    </span>
                    No hay departamentos registrados todavía.
                  </td>
                </tr>
              ) : (
                departamentos.map((d) => {
                  const total = cargosPorDepartamento
                    ? (cargosPorDepartamento[d.id] ?? 0)
                    : null;
                  const tieneCargos = total !== null && total > 0;
                  return (
                    <tr key={d.id}>
                      <td data-label="Nombre">{d.nombre}</td>
                      <td data-label="Cargos">{total ?? "—"}</td>
                      <td data-label="Acciones">
                        <button
                          type="button"
                          className="btn-edit"
                          onClick={() => abrirEditar(d)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className="btn-delete"
                          onClick={() => eliminar(d)}
                          disabled={tieneCargos}
                          title={
                            tieneCargos
                              ? `Tiene ${total} cargo(s): reasígnalos o elimínalos primero`
                              : undefined
                          }
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
          titulo={modal.id ? "Editar departamento" : "Crear departamento"}
          onCerrar={cerrarModal}
          onSubmit={guardar}
          guardando={guardando}
          etiquetaGuardar={modal.id ? "Guardar cambios" : "Crear departamento"}
        >
          <label>
            Nombre *
            <input
              value={modal.nombre}
              onChange={(e) => {
                setModal((prev) => ({ ...prev, nombre: e.target.value }));
                setErrorNombre("");
              }}
              maxLength={100}
              placeholder="Ej: Administración"
              autoFocus
            />
            {errorNombre && <span className="campo-error">{errorNombre}</span>}
          </label>
        </ModalFormulario>
      )}
    </>
  );
}

export default Departamentos;
