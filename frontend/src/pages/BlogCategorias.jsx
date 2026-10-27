// src/pages/BlogCategorias.jsx
import { useState, useEffect, useCallback } from "react";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { soloDigitos } from "../utils/validacionesEntrada";
import { ICONOS } from "../utils/iconos";
import "./BlogCategorias.css";

const VACIO = {
  nombre: "",
  descripcion: "",
  iconoUrl: "",
  colorHex: "#3498db",
  ordinal: 0,
  activo: true,
};

// El backend espera null (no "") en los textos opcionales vacíos.
function aPayload(form) {
  return {
    nombre: form.nombre.trim(),
    descripcion: form.descripcion.trim() || null,
    iconoUrl: form.iconoUrl.trim() || null,
    colorHex: form.colorHex,
    ordinal: Number(form.ordinal) || 0,
    activo: form.activo,
  };
}

function BlogCategorias() {
  const toast = useToast();
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // Modal: null = cerrado, { id|null, form }
  const [modal, setModal] = useState(null);
  const [errores, setErrores] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      // size máximo del backend = 100; hay pocas categorías, no hace falta paginar.
      const { data } = await api.get("/blog/admin/categorias", {
        params: { page: 0, size: 100 },
      });
      const ordenadas = [...data.content].sort(
        (a, b) =>
          (a.ordinal ?? 0) - (b.ordinal ?? 0) ||
          a.nombre.localeCompare(b.nombre, "es"),
      );
      setCategorias(ordenadas);
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudo cargar la lista de categorías."),
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  // ---------- Modal ----------
  const abrirNueva = () => {
    setErrores({});
    setModal({ id: null, form: { ...VACIO } });
  };

  const abrirEditar = (c) => {
    setErrores({});
    setModal({
      id: c.id,
      form: {
        nombre: c.nombre ?? "",
        descripcion: c.descripcion ?? "",
        iconoUrl: c.iconoUrl ?? "",
        colorHex: c.colorHex ?? "#3498db",
        ordinal: c.ordinal ?? 0,
        activo: c.activo ?? true,
      },
    });
  };

  const cerrarModal = () => {
    if (!guardando) setModal(null);
  };

  const cambiar = (campo, valor) => {
    setModal((prev) => ({ ...prev, form: { ...prev.form, [campo]: valor } }));
    setErrores((prev) => ({ ...prev, [campo]: undefined }));
  };

  const validar = (form) => {
    const e = {};
    if (!form.nombre.trim()) e.nombre = "El nombre es obligatorio.";
    else if (form.nombre.trim().length > 100)
      e.nombre = "Máximo 100 caracteres.";
    if (!/^#[0-9A-Fa-f]{6}$/.test(form.colorHex))
      e.colorHex = "Usa el formato #RRGGBB.";
    if (form.iconoUrl.trim().length > 500)
      e.iconoUrl = "Máximo 500 caracteres.";
    const ordinal = Number(form.ordinal);
    if (!Number.isInteger(ordinal) || ordinal < 0 || ordinal > 999)
      e.ordinal = "El orden debe ser un entero entre 0 y 999.";
    setErrores(e);
    return Object.keys(e).length === 0;
  };

  const guardar = async (ev) => {
    ev.preventDefault();
    if (!validar(modal.form)) return;
    setGuardando(true);
    try {
      const payload = aPayload(modal.form);
      if (modal.id) {
        await api.put(`/blog/admin/categorias/${modal.id}`, payload);
        toast.success("Categoría actualizada correctamente.");
      } else {
        await api.post("/blog/admin/categorias", payload);
        toast.success("Categoría creada correctamente.");
      }
      setModal(null);
      cargar();
    } catch (err) {
      // 409 = nombre duplicado: se muestra el mensaje del backend
      toast.error(extractErrorMessage(err, "No se pudo guardar la categoría."));
    } finally {
      setGuardando(false);
    }
  };

  // ---------- Acciones de fila ----------
  const alternarActivo = async (c) => {
    try {
      await api.put(
        `/blog/admin/categorias/${c.id}`,
        aPayload({
          nombre: c.nombre,
          descripcion: c.descripcion ?? "",
          iconoUrl: c.iconoUrl ?? "",
          colorHex: c.colorHex ?? "#3498db",
          ordinal: c.ordinal ?? 0,
          activo: !c.activo,
        }),
      );
      setCategorias((prev) =>
        prev.map((x) => (x.id === c.id ? { ...x, activo: !x.activo } : x)),
      );
      toast.success(
        `Categoría ${!c.activo ? "activada" : "desactivada"} correctamente.`,
      );
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo actualizar la categoría."),
      );
    }
  };

  const eliminar = async (c) => {
    if (!window.confirm(`¿Eliminar la categoría "${c.nombre}"?`)) return;
    try {
      await api.delete(`/blog/admin/categorias/${c.id}`);
      setCategorias((prev) => prev.filter((x) => x.id !== c.id));
      toast.success("Categoría eliminada correctamente.");
    } catch (err) {
      // 409 si tiene comunicados: el backend sugiere desactivarla
      toast.error(
        extractErrorMessage(err, "No se pudo eliminar la categoría."),
      );
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Categorías del blog</h1>
          <div className="mod-heading-actions">
            <button type="button" className="btn-primary" onClick={abrirNueva}>
              + Crear categoría
            </button>
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Orden</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th>Color</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={6} className="tabla-estado">
                    Cargando categorías...
                  </td>
                </tr>
              ) : categorias.length === 0 ? (
                <tr>
                  <td colSpan={6} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.blog} />
                    </span>
                    Aún no hay categorías. Crea la primera para poder publicar
                    comunicados.
                  </td>
                </tr>
              ) : (
                categorias.map((c) => (
                  <tr key={c.id}>
                    <td data-label="Orden">{c.ordinal ?? 0}</td>
                    <td data-label="Nombre">{c.nombre}</td>
                    <td data-label="Descripción">{c.descripcion || "—"}</td>
                    <td data-label="Color">
                      <span className="bcat-color">
                        <span
                          className="bcat-color-muestra"
                          style={{ background: c.colorHex }}
                          aria-hidden="true"
                        />
                        {c.colorHex}
                      </span>
                    </td>
                    <td data-label="Estado">
                      <span
                        className={`badge ${c.activo ? "badge-presente" : "badge-ausente"}`}
                      >
                        {c.activo ? "Activa" : "Inactiva"}
                      </span>
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
                        className={c.activo ? "btn-delete" : "btn-reactivar"}
                        onClick={() => alternarActivo(c)}
                      >
                        {c.activo ? "Desactivar" : "Activar"}
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
        <div className="bcat-overlay" onClick={cerrarModal}>
          <form
            className="bcat-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="bcat-titulo"
            onClick={(e) => e.stopPropagation()}
            onSubmit={guardar}
            noValidate
          >
            <h2 id="bcat-titulo">
              {modal.id ? "Editar categoría" : "Crear categoría"}
            </h2>

            <label>
              Nombre *
              <input
                value={modal.form.nombre}
                onChange={(e) => cambiar("nombre", e.target.value)}
                placeholder="Ej: Avisos generales"
                maxLength={100}
                autoFocus
              />
              {errores.nombre && (
                <span className="campo-error">{errores.nombre}</span>
              )}
            </label>

            <label>
              Descripción
              <textarea
                rows={3}
                value={modal.form.descripcion}
                onChange={(e) => cambiar("descripcion", e.target.value)}
                placeholder="Para qué tipo de comunicados se usa"
              />
            </label>

            <div className="bcat-fila">
              <label>
                Color
                <span className="bcat-color-campo">
                  <input
                    type="color"
                    value={modal.form.colorHex}
                    onChange={(e) => cambiar("colorHex", e.target.value)}
                    aria-label="Elegir color"
                  />
                  <input
                    value={modal.form.colorHex}
                    onChange={(e) => cambiar("colorHex", e.target.value)}
                    maxLength={7}
                    aria-label="Código de color"
                  />
                </span>
                {errores.colorHex && (
                  <span className="campo-error">{errores.colorHex}</span>
                )}
              </label>

              <label>
                Orden
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={modal.form.ordinal}
                  onChange={(e) =>
                    cambiar("ordinal", soloDigitos(e.target.value))
                  }
                />
                {errores.ordinal && (
                  <span className="campo-error">{errores.ordinal}</span>
                )}
              </label>
            </div>

            <label>
              URL del ícono (opcional)
              <input
                value={modal.form.iconoUrl}
                onChange={(e) => cambiar("iconoUrl", e.target.value)}
                placeholder="https://..."
              />
              {errores.iconoUrl && (
                <span className="campo-error">{errores.iconoUrl}</span>
              )}
            </label>

            <label className="switch-inline-label">
              <input
                type="checkbox"
                checked={modal.form.activo}
                onChange={(e) => cambiar("activo", e.target.checked)}
              />
              Categoría activa
            </label>

            <div className="bcat-acciones">
              <button
                type="button"
                className="btn-secondary"
                onClick={cerrarModal}
                disabled={guardando}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={guardando}
              >
                {guardando
                  ? "Guardando..."
                  : modal.id
                    ? "Guardar cambios"
                    : "Crear categoría"}
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}

export default BlogCategorias;
