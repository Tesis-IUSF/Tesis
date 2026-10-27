import AnimatedSelect from "../components/AnimatedSelect";
// src/pages/Requisitos.jsx
import { useState, useEffect, useCallback, useMemo } from "react";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import { esUrlHttpHttpsOpcional } from "../utils/validacionesEntrada";
import ModalFormulario from "../components/ModalFormulario";
import "./Requisitos.css";

// Valores exactos que acepta el backend
const TIPOS = [
  { valor: "nuevo_ingreso", etiqueta: "Nuevo ingreso" },
  { valor: "regular", etiqueta: "Regular" },
  { valor: "reingreso", etiqueta: "Reingreso" },
  { valor: "traslado", etiqueta: "Traslado" },
];
const ETIQUETA_TIPO = Object.fromEntries(
  TIPOS.map((t) => [t.valor, t.etiqueta]),
);

const FORM_VACIO = {
  nombre: "",
  descripcion: "",
  obligatorio: true,
  tipos: [],
  nivelEducativoId: "",
  documentoTemplateUrl: "",
  activo: true,
};

const o = (v) => (v && v.trim() ? v.trim() : null);

const separarTipos = (texto) =>
  texto
    ? texto
        .split(",")
        .map((t) => t.trim())
        .filter(Boolean)
    : [];

// El PUT del backend exige el objeto completo
const aPayload = (r, cambios = {}) => ({
  nombre: r.nombre,
  descripcion: r.descripcion ?? null,
  obligatorio: r.obligatorio,
  aplicaTipoIngreso: r.aplicaTipoIngreso ?? null,
  nivelEducativoId: r.nivelEducativoId ?? null,
  documentoTemplateUrl: r.documentoTemplateUrl ?? null,
  activo: r.activo,
  ...cambios,
});

function Requisitos() {
  const toast = useToast();

  const [requisitos, setRequisitos] = useState([]);
  const [niveles, setNiveles] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const [busqueda, setBusqueda] = useState("");
  const [filtro, setFiltro] = useState("todos");

  const [modal, setModal] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VACIO);
  const [errorForm, setErrorForm] = useState("");
  const [guardando, setGuardando] = useState(false);

  const [aEliminar, setAEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const [cambiandoId, setCambiandoId] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError("");
    try {
      const { data } = await api.get("/catalogos/requisitos-matricula", {
        params: { page: 0, size: 100 },
      });
      setRequisitos(data.content);
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudieron cargar los requisitos."),
      );
      setRequisitos([]);
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
    api
      .get("/catalogos/niveles-educativos", { params: { page: 0, size: 100 } })
      .then(({ data }) => setNiveles(data.content ?? data))
      .catch(() => setNiveles([]));
  }, [cargar]);

  const visibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return requisitos.filter((r) => {
      if (filtro === "activos" && !r.activo) return false;
      if (filtro === "inactivos" && r.activo) return false;
      if (!texto) return true;
      return (
        r.nombre.toLowerCase().includes(texto) ||
        (r.descripcion ?? "").toLowerCase().includes(texto)
      );
    });
  }, [requisitos, busqueda, filtro]);

  const totales = useMemo(
    () => ({
      activos: requisitos.filter((r) => r.activo).length,
      obligatorios: requisitos.filter((r) => r.activo && r.obligatorio).length,
    }),
    [requisitos],
  );

  // ---------- Formulario ----------
  const abrirCrear = () => {
    setEditando(null);
    setForm(FORM_VACIO);
    setErrorForm("");
    setModal(true);
  };

  const abrirEditar = (r) => {
    setEditando(r);
    setForm({
      nombre: r.nombre,
      descripcion: r.descripcion ?? "",
      obligatorio: !!r.obligatorio,
      tipos: separarTipos(r.aplicaTipoIngreso),
      nivelEducativoId: r.nivelEducativoId ?? "",
      documentoTemplateUrl: r.documentoTemplateUrl ?? "",
      activo: r.activo !== false,
    });
    setErrorForm("");
    setModal(true);
  };

  const cambiar = (campo, valor) =>
    setForm((prev) => ({ ...prev, [campo]: valor }));

  const alternarTipo = (valor) =>
    setForm((prev) => ({
      ...prev,
      tipos: prev.tipos.includes(valor)
        ? prev.tipos.filter((t) => t !== valor)
        : [...prev.tipos, valor],
    }));

  const guardar = async (ev) => {
    ev.preventDefault();
    const nombre = form.nombre.trim();
    if (!nombre) {
      setErrorForm("El nombre del requisito es obligatorio.");
      return;
    }
    if (nombre.length > 200) {
      setErrorForm("El nombre admite máximo 200 caracteres.");
      return;
    }
    if (form.documentoTemplateUrl.trim().length > 500) {
      setErrorForm("El enlace de la plantilla admite máximo 500 caracteres.");
      return;
    }
    if (!esUrlHttpHttpsOpcional(form.documentoTemplateUrl)) {
      setErrorForm("El enlace de la plantilla debe comenzar con http:// o https://.");
      return;
    }

    const payload = {
      nombre,
      descripcion: o(form.descripcion),
      obligatorio: form.obligatorio,
      aplicaTipoIngreso: form.tipos.length > 0 ? form.tipos.join(",") : null,
      nivelEducativoId:
        form.nivelEducativoId === "" ? null : Number(form.nivelEducativoId),
      documentoTemplateUrl: o(form.documentoTemplateUrl),
      activo: form.activo,
    };

    setGuardando(true);
    setErrorForm("");
    try {
      if (editando) {
        await api.put(
          `/catalogos/requisitos-matricula/${editando.id}`,
          payload,
        );
        toast.success("Requisito actualizado.");
      } else {
        await api.post("/catalogos/requisitos-matricula", payload);
        toast.success("Requisito creado.");
      }
      setModal(false);
      cargar();
    } catch (err) {
      setErrorForm(
        extractErrorMessage(err, "No se pudo guardar el requisito."),
      );
    } finally {
      setGuardando(false);
    }
  };

  // ---------- Activar / desactivar ----------
  const alternarActivo = async (r) => {
    setCambiandoId(r.id);
    try {
      await api.put(
        `/catalogos/requisitos-matricula/${r.id}`,
        aPayload(r, { activo: !r.activo }),
      );
      toast.success(
        r.activo ? "Requisito desactivado." : "Requisito activado.",
      );
      cargar();
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo cambiar el estado."));
    } finally {
      setCambiandoId(null);
    }
  };

  // ---------- Eliminar ----------
  const eliminar = async (ev) => {
    ev.preventDefault();
    setEliminando(true);
    try {
      await api.delete(`/catalogos/requisitos-matricula/${aEliminar.id}`);
      toast.success("Requisito eliminado.");
      setAEliminar(null);
      cargar();
    } catch (err) {
      setAEliminar(null);
      toast.error(
        extractErrorMessage(
          err,
          "No se pudo eliminar. Si el requisito ya se usó en inscripciones, desactívalo en su lugar.",
        ),
      );
    } finally {
      setEliminando(false);
    }
  };

  const tiposDe = (r) => separarTipos(r.aplicaTipoIngreso);

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>Requisitos de matrícula</h1>
        <div className="mod-heading-actions">
          <button type="button" className="btn-primary" onClick={abrirCrear}>
            + Nuevo requisito
          </button>
        </div>
      </div>
      <p className="req-subtitulo">
        Documentos que se verifican al inscribir. {totales.activos} activos,{" "}
        {totales.obligatorios} obligatorios.
      </p>

      <div className="filters-bar">
        <label>
          Buscar
          <input
            type="search"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Nombre o descripción"
          />
        </label>
        <label>
          Estado
          <AnimatedSelect value={filtro} onChange={(e) => setFiltro(e.target.value)}>
            <option value="todos">Todos</option>
            <option value="activos">Activos</option>
            <option value="inactivos">Inactivos</option>
          </AnimatedSelect>
        </label>
      </div>

      {error && <p className="error-msg">{error}</p>}

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Requisito</th>
              <th>Tipo</th>
              <th>Aplica a</th>
              <th>Nivel</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={6} className="tabla-estado">
                  Cargando requisitos...
                </td>
              </tr>
            ) : visibles.length === 0 ? (
              <tr>
                <td colSpan={6} className="tabla-estado">
                  <span className="tabla-estado-icono">
                    <Icon icon={ICONOS.matricula} />
                  </span>
                  {requisitos.length === 0
                    ? "Aún no hay requisitos. Crea el primero con «Nuevo requisito»."
                    : "No hay requisitos que coincidan con el filtro."}
                </td>
              </tr>
            ) : (
              visibles.map((r) => {
                const ocupado = cambiandoId === r.id;
                const tipos = tiposDe(r);
                return (
                  <tr key={r.id}>
                    <td data-label="Requisito">
                      <div className="req-nombre">
                        <strong>{r.nombre}</strong>
                        {r.descripcion && <small>{r.descripcion}</small>}
                        {r.documentoTemplateUrl && (
                          <a
                            href={r.documentoTemplateUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="req-plantilla"
                          >
                            Ver plantilla
                          </a>
                        )}
                      </div>
                    </td>
                    <td data-label="Tipo">
                      <span
                        className={`req-etiqueta${r.obligatorio ? " obligatorio" : ""}`}
                      >
                        {r.obligatorio ? "Obligatorio" : "Opcional"}
                      </span>
                    </td>
                    <td data-label="Aplica a">
                      {tipos.length === 0 ? (
                        <span className="req-todos">Todos los ingresos</span>
                      ) : (
                        <span className="req-chips">
                          {tipos.map((t) => (
                            <span key={t} className="req-chip">
                              {ETIQUETA_TIPO[t] ?? t}
                            </span>
                          ))}
                        </span>
                      )}
                    </td>
                    <td data-label="Nivel">
                      {r.nivelEducativoNombre ?? (
                        <span className="req-todos">Todos los niveles</span>
                      )}
                    </td>
                    <td data-label="Estado">
                      <span
                        className={`badge ${r.activo ? "badge-presente" : "badge-ausente"}`}
                      >
                        {r.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td data-label="Acciones">
                      <div className="req-acciones">
                        <button
                          type="button"
                          className="btn-edit"
                          onClick={() => abrirEditar(r)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className={`btn-estado ${r.activo ? "btn-estado--desactivar" : "btn-estado--activar"}`}
                          disabled={ocupado}
                          onClick={() => alternarActivo(r)}
                        >
                          {r.activo ? "Desactivar" : "Activar"}
                        </button>
                        <button
                          type="button"
                          className="btn-delete"
                          onClick={() => setAEliminar(r)}
                        >
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {modal && (
        <ModalFormulario
          titulo={editando ? "Editar requisito" : "Nuevo requisito"}
          onCerrar={() => setModal(false)}
          onSubmit={guardar}
          guardando={guardando}
          etiquetaGuardar={editando ? "Guardar cambios" : "Crear requisito"}
        >
          <div className="req-form">
            {errorForm && <p className="error-msg">{errorForm}</p>}

            <label>
              Nombre *
              <input
                value={form.nombre}
                onChange={(e) => cambiar("nombre", e.target.value)}
                maxLength={200}
                autoFocus
                placeholder="Ej.: Partida de nacimiento"
              />
            </label>

            <label>
              Descripción
              <textarea
                rows={2}
                value={form.descripcion}
                onChange={(e) => cambiar("descripcion", e.target.value)}
                placeholder="Qué debe entregar el representante"
              />
            </label>

            <label>
              Nivel educativo
              <AnimatedSelect
                value={form.nivelEducativoId}
                onChange={(e) => cambiar("nivelEducativoId", e.target.value)}
              >
                <option value="">Todos los niveles</option>
                {niveles.map((n) => (
                  <option key={n.id} value={n.id}>
                    {n.nombre}
                  </option>
                ))}
              </AnimatedSelect>
            </label>

            <fieldset className="req-tipos">
              <legend>Aplica a tipos de ingreso</legend>
              <div className="req-tipos-lista">
                {TIPOS.map((t) => (
                  <label key={t.valor} className="req-check">
                    <input
                      type="checkbox"
                      checked={form.tipos.includes(t.valor)}
                      onChange={() => alternarTipo(t.valor)}
                    />
                    {t.etiqueta}
                  </label>
                ))}
              </div>
              <small className="req-ayuda">
                Sin ninguno marcado, el requisito aplica a todos los tipos de
                ingreso.
              </small>
            </fieldset>

            <label>
              Enlace de la plantilla (opcional)
              <input
                type="url"
                value={form.documentoTemplateUrl}
                onChange={(e) =>
                  cambiar("documentoTemplateUrl", e.target.value)
                }
                maxLength={500}
                pattern="https?://.+"
                placeholder="https://..."
              />
            </label>

            <div className="req-interruptores">
              <label className="req-check">
                <input
                  type="checkbox"
                  checked={form.obligatorio}
                  onChange={(e) => cambiar("obligatorio", e.target.checked)}
                />
                Obligatorio para formalizar la inscripción
              </label>
              <label className="req-check">
                <input
                  type="checkbox"
                  checked={form.activo}
                  onChange={(e) => cambiar("activo", e.target.checked)}
                />
                Activo (se pide en inscripciones nuevas)
              </label>
            </div>
          </div>
        </ModalFormulario>
      )}

      {aEliminar && (
        <ModalFormulario
          titulo="Eliminar requisito"
          onCerrar={() => setAEliminar(null)}
          onSubmit={eliminar}
          guardando={eliminando}
          etiquetaGuardar="Eliminar"
        >
          <p className="req-confirmar">
            ¿Eliminar <strong>{aEliminar.nombre}</strong>? Si ya se usó en
            inscripciones no se podrá borrar; en ese caso desactívalo.
          </p>
        </ModalFormulario>
      )}
    </div>
  );
}

export default Requisitos;
