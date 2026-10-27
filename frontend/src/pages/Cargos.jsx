// src/pages/Cargos.jsx
import { useState, useEffect, useCallback, useMemo } from "react";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import ModalFormulario from "../components/ModalFormulario";

function Cargos() {
  const toast = useToast();
  const [cargos, setCargos] = useState([]);
  const [departamentos, setDepartamentos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [filtroDepto, setFiltroDepto] = useState("");

  const [modal, setModal] = useState(null); // { id|null, nombreCargo, departamentoId }
  const [errores, setErrores] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      const [resCargos, resDeptos] = await Promise.all([
        api.get("/catalogos/cargos", { params: { page: 0, size: 100 } }),
        api.get("/catalogos/departamentos", { params: { page: 0, size: 100 } }),
      ]);
      setCargos(resCargos.data.content);
      setDepartamentos(
        [...resDeptos.data.content].sort((a, b) =>
          a.nombre.localeCompare(b.nombre, "es"),
        ),
      );
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudo cargar la lista de cargos."),
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const visibles = useMemo(() => {
    const lista = filtroDepto
      ? cargos.filter((c) => String(c.departamentoId) === filtroDepto)
      : cargos;
    return [...lista].sort(
      (a, b) =>
        (a.nombreDepartamento ?? "").localeCompare(
          b.nombreDepartamento ?? "",
          "es",
        ) || a.nombreCargo.localeCompare(b.nombreCargo, "es"),
    );
  }, [cargos, filtroDepto]);

  const abrirNuevo = () => {
    setErrores({});
    setModal({
      id: null,
      nombreCargo: "",
      departamentoId: filtroDepto || "",
    });
  };

  const abrirEditar = (c) => {
    setErrores({});
    setModal({
      id: c.id,
      nombreCargo: c.nombreCargo,
      departamentoId: c.departamentoId ?? "",
    });
  };

  const cerrarModal = useCallback(() => setModal(null), []);

  const cambiar = (campo, valor) => {
    setModal((prev) => ({ ...prev, [campo]: valor }));
    setErrores((prev) => ({ ...prev, [campo]: undefined }));
  };

  const guardar = async (ev) => {
    ev.preventDefault();
    const nombreCargo = modal.nombreCargo.trim();
    const e = {};
    if (!nombreCargo) e.nombreCargo = "El nombre es obligatorio.";
    else if (nombreCargo.length > 100) e.nombreCargo = "Máximo 100 caracteres.";
    if (!modal.departamentoId) e.departamentoId = "Selecciona un departamento.";

    if (!e.nombreCargo && !e.departamentoId) {
      const repetido = cargos.some(
        (c) =>
          c.id !== modal.id &&
          String(c.departamentoId) === String(modal.departamentoId) &&
          c.nombreCargo.trim().toLowerCase() === nombreCargo.toLowerCase(),
      );
      if (repetido)
        e.nombreCargo = "Ya existe un cargo con ese nombre en el departamento.";
    }
    setErrores(e);
    if (Object.keys(e).length > 0) return;

    setGuardando(true);
    const payload = {
      nombreCargo,
      departamentoId: Number(modal.departamentoId),
    };
    try {
      if (modal.id) {
        await api.put(`/catalogos/cargos/${modal.id}`, payload);
        toast.success("Cargo actualizado correctamente.");
      } else {
        await api.post("/catalogos/cargos", payload);
        toast.success("Cargo creado correctamente.");
      }
      setModal(null);
      cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo guardar el cargo."));
    } finally {
      setGuardando(false);
    }
  };

  const eliminar = async (c) => {
    if (!window.confirm(`¿Eliminar el cargo "${c.nombreCargo}"?`)) return;
    try {
      await api.delete(`/catalogos/cargos/${c.id}`);
      setCargos((prev) => prev.filter((x) => x.id !== c.id));
      toast.success("Cargo eliminado correctamente.");
    } catch (err) {
      toast.error(
        extractErrorMessage(
          err,
          "No se pudo eliminar el cargo. Verifica que no tenga empleados asignados.",
        ),
      );
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Cargos</h1>
          <div className="mod-heading-actions">
            <button type="button" className="btn-primary" onClick={abrirNuevo}>
              + Crear cargo
            </button>
          </div>
        </div>

        <div className="filters-bar">
          <label>
            Departamento
            <select
              value={filtroDepto}
              onChange={(e) => setFiltroDepto(e.target.value)}
            >
              <option value="">Todos</option>
              {departamentos.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.nombre}
                </option>
              ))}
            </select>
          </label>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Cargo</th>
                <th>Departamento</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={3} className="tabla-estado">
                    Cargando cargos...
                  </td>
                </tr>
              ) : visibles.length === 0 ? (
                <tr>
                  <td colSpan={3} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.cargos} />
                    </span>
                    {filtroDepto
                      ? "Este departamento aún no tiene cargos."
                      : "No hay cargos registrados todavía."}
                  </td>
                </tr>
              ) : (
                visibles.map((c) => (
                  <tr key={c.id}>
                    <td data-label="Cargo">{c.nombreCargo}</td>
                    <td data-label="Departamento">
                      {c.nombreDepartamento ?? "—"}
                    </td>
                    <td data-label="Acciones">
                      <button
                        type="button"
                        className="btn-edit"
                        onClick={() => abrirEditar(c)}
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        className="btn-delete"
                        onClick={() => eliminar(c)}
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
          titulo={modal.id ? "Editar cargo" : "Crear cargo"}
          onCerrar={cerrarModal}
          onSubmit={guardar}
          guardando={guardando}
          etiquetaGuardar={modal.id ? "Guardar cambios" : "Crear cargo"}
        >
          <label>
            Nombre del cargo *
            <input
              value={modal.nombreCargo}
              onChange={(e) => cambiar("nombreCargo", e.target.value)}
              maxLength={100}
              required
              placeholder="Ej: Docente de aula"
              autoFocus
            />
            {errores.nombreCargo && (
              <span className="campo-error">{errores.nombreCargo}</span>
            )}
          </label>

          <label>
            Departamento *
            <select
              value={modal.departamentoId}
              onChange={(e) => cambiar("departamentoId", e.target.value)}
              required
            >
              <option value="">Selecciona...</option>
              {departamentos.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.nombre}
                </option>
              ))}
            </select>
            {errores.departamentoId && (
              <span className="campo-error">{errores.departamentoId}</span>
            )}
          </label>
        </ModalFormulario>
      )}
    </>
  );
}

export default Cargos;
