// src/pages/BlogComunicadoForm.jsx
import { useState, useEffect, useRef, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import DOMPurify from "dompurify";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { soloDigitos } from "../utils/validacionesEntrada";
import "./BlogComunicadoForm.css";

const VACIO = {
  titulo: "",
  categoriaId: "",
  resumen: "",
  contenidoHtml: "",
  imagenUrl: "",
  publicado: false,
  visiblePublico: false,
  fechaPublicacion: "",
  fechaExpiracion: "",
  ordenVisualizacion: 0,
};

// El backend usa LocalDateTime ("2026-10-13T01:39:00"); datetime-local quiere "YYYY-MM-DDTHH:mm".
const aInputFecha = (v) => (v ? String(v).substring(0, 16) : "");
const aApiFecha = (v) => (v ? `${v}:00` : null);

function BlogComunicadoForm() {
  const { id } = useParams();
  const esEdicion = !!id;
  const navigate = useNavigate();
  const toast = useToast();

  const [form, setForm] = useState(VACIO);
  const [categorias, setCategorias] = useState([]);
  const [cargandoDatos, setCargandoDatos] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [errores, setErrores] = useState({});
  const [pestana, setPestana] = useState("editar"); // editar | vista
  const [imagenRota, setImagenRota] = useState(false);
  const areaRef = useRef(null);

  useEffect(() => {
    let activo = true;
    const pedirCategorias = api.get("/blog/admin/categorias", {
      params: { page: 0, size: 100 },
    });
    const pedirComunicado = esEdicion
      ? api.get(`/blog/admin/comunicados/${id}`)
      : Promise.resolve(null);

    Promise.all([pedirCategorias, pedirComunicado])
      .then(([resCat, resCom]) => {
        if (!activo) return;
        setCategorias(resCat.data.content);
        if (resCom) {
          const d = resCom.data;
          setForm({
            titulo: d.titulo ?? "",
            categoriaId: d.categoriaId ?? "",
            resumen: d.resumen ?? "",
            contenidoHtml: d.contenidoHtml ?? "",
            imagenUrl: d.imagenUrl ?? "",
            publicado: !!d.publicado,
            visiblePublico: !!d.visiblePublico,
            fechaPublicacion: aInputFecha(d.fechaPublicacion),
            fechaExpiracion: aInputFecha(d.fechaExpiracion),
            ordenVisualizacion: d.ordenVisualizacion ?? 0,
          });
        }
      })
      .catch((err) => {
        if (!activo) return;
        toast.error(
          extractErrorMessage(err, "No se pudo cargar el comunicado."),
        );
        navigate("/blog/comunicados");
      })
      .finally(() => {
        if (activo) setCargandoDatos(false);
      });
    return () => {
      activo = false;
    };
  }, [id, esEdicion, navigate, toast]);

  const actualizar = (campo, valor) => {
    setForm((prev) => ({ ...prev, [campo]: valor }));
    setErrores((prev) => ({ ...prev, [campo]: undefined }));
  };

  // Solo se pueden elegir categorías activas (el backend rechaza las inactivas),
  // pero si el comunicado ya tenía una inactiva se muestra para no perder el dato.
  const opcionesCategoria = useMemo(
    () =>
      categorias.filter(
        (c) => c.activo || String(c.id) === String(form.categoriaId),
      ),
    [categorias, form.categoriaId],
  );

  const vistaPrevia = useMemo(
    () => DOMPurify.sanitize(form.contenidoHtml),
    [form.contenidoHtml],
  );

  // ---------- Botones rápidos de formato ----------
  const envolver = (abre, cierra, relleno = "texto") => {
    const ta = areaRef.current;
    if (!ta) return;
    const { selectionStart: s, selectionEnd: e } = ta;
    const seleccion = form.contenidoHtml.slice(s, e) || relleno;
    const nuevo =
      form.contenidoHtml.slice(0, s) +
      abre +
      seleccion +
      cierra +
      form.contenidoHtml.slice(e);
    actualizar("contenidoHtml", nuevo);
    requestAnimationFrame(() => {
      ta.focus();
      ta.setSelectionRange(s + abre.length, s + abre.length + seleccion.length);
    });
  };

  const insertarEnlace = () => {
    const url = window.prompt("Dirección del enlace (https://...)");
    if (!url) return;
    envolver(
      `<a href="${url.replace(/"/g, "&quot;")}" target="_blank" rel="noopener noreferrer">`,
      "</a>",
      "enlace",
    );
  };

  const BOTONES = [
    { etiqueta: "Párrafo", accion: () => envolver("<p>", "</p>") },
    { etiqueta: "Título", accion: () => envolver("<h3>", "</h3>", "Título") },
    { etiqueta: "Negrita", accion: () => envolver("<strong>", "</strong>") },
    { etiqueta: "Cursiva", accion: () => envolver("<em>", "</em>") },
    {
      etiqueta: "Lista",
      accion: () => envolver("<ul>\n  <li>", "</li>\n</ul>", "elemento"),
    },
    { etiqueta: "Enlace", accion: insertarEnlace },
  ];

  // ---------- Validación y envío ----------
  const validar = () => {
    const e = {};
    if (!form.titulo.trim()) e.titulo = "El título es obligatorio.";
    else if (form.titulo.trim().length > 255)
      e.titulo = "Máximo 255 caracteres.";
    if (!form.categoriaId) e.categoriaId = "Selecciona una categoría.";
    if (form.resumen.length > 500) e.resumen = "Máximo 500 caracteres.";
    if (!form.contenidoHtml.trim())
      e.contenidoHtml = "El contenido es obligatorio.";
    if (form.imagenUrl.trim().length > 500)
      e.imagenUrl = "Máximo 500 caracteres.";
    const ordenVisualizacion = Number(form.ordenVisualizacion);
    if (
      !Number.isInteger(ordenVisualizacion) ||
      ordenVisualizacion < 0 ||
      ordenVisualizacion > 999
    )
      e.ordenVisualizacion = "El orden debe ser un entero entre 0 y 999.";
    if (
      form.fechaPublicacion &&
      form.fechaExpiracion &&
      form.fechaExpiracion < form.fechaPublicacion
    )
      e.fechaExpiracion =
        "La expiración no puede ser anterior a la publicación.";
    setErrores(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev) => {
    ev.preventDefault();
    if (!validar()) {
      toast.error("Revisa los campos marcados.");
      return;
    }
    setGuardando(true);
    const payload = {
      titulo: form.titulo.trim(),
      contenidoHtml: form.contenidoHtml,
      resumen: form.resumen.trim() || null,
      categoriaId: Number(form.categoriaId),
      publicado: form.publicado,
      fechaPublicacion: aApiFecha(form.fechaPublicacion),
      fechaExpiracion: aApiFecha(form.fechaExpiracion),
      imagenUrl: form.imagenUrl.trim() || null,
      ordenVisualizacion: Number(form.ordenVisualizacion) || 0,
      visiblePublico: form.visiblePublico,
    };
    try {
      if (esEdicion) {
        await api.put(`/blog/admin/comunicados/${id}`, payload);
        toast.success("Comunicado actualizado correctamente.");
      } else {
        await api.post("/blog/admin/comunicados", payload);
        toast.success("Comunicado creado correctamente.");
      }
      navigate("/blog/comunicados");
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo guardar el comunicado."),
      );
    } finally {
      setGuardando(false);
    }
  };

  if (cargandoDatos) {
    return (
      <div className="mod-main">
        <p>Cargando comunicado...</p>
      </div>
    );
  }

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>{esEdicion ? "Editar comunicado" : "Crear comunicado"}</h1>
      </div>

      <form className="empleado-form bfrm" onSubmit={handleSubmit} noValidate>
        <div className="bfrm-grid">
          <label className="bfrm-ancho">
            Título *
            <input
              value={form.titulo}
              onChange={(e) => actualizar("titulo", e.target.value)}
              maxLength={255}
              placeholder="Ej: Inicio del año escolar 2026 – 2027"
            />
            {errores.titulo && (
              <span className="campo-error">{errores.titulo}</span>
            )}
          </label>

          <label>
            Categoría *
            <select
              value={form.categoriaId}
              onChange={(e) => actualizar("categoriaId", e.target.value)}
            >
              <option value="">Selecciona...</option>
              {opcionesCategoria.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                  {c.activo ? "" : " (inactiva)"}
                </option>
              ))}
            </select>
            {errores.categoriaId && (
              <span className="campo-error">{errores.categoriaId}</span>
            )}
          </label>

          <label>
            Orden de visualización
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={form.ordenVisualizacion}
              onChange={(e) =>
                actualizar(
                  "ordenVisualizacion",
                  soloDigitos(e.target.value),
                )
              }
            />
            {errores.ordenVisualizacion && (
              <span className="campo-error">
                {errores.ordenVisualizacion}
              </span>
            )}
            <span className="bfrm-ayuda">
              Los números menores salen primero en el blog.
            </span>
          </label>

          <label className="bfrm-ancho">
            Resumen
            <textarea
              rows={3}
              value={form.resumen}
              onChange={(e) => actualizar("resumen", e.target.value)}
              placeholder="Texto corto que se muestra en las tarjetas del blog"
            />
            <span className="bfrm-ayuda">{form.resumen.length}/500</span>
            {errores.resumen && (
              <span className="campo-error">{errores.resumen}</span>
            )}
          </label>
        </div>

        <fieldset className="bfrm-contenido">
          <legend>Contenido *</legend>

          <div className="bfrm-pestanas" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={pestana === "editar"}
              className={pestana === "editar" ? "activa" : ""}
              onClick={() => setPestana("editar")}
            >
              Editar
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={pestana === "vista"}
              className={pestana === "vista" ? "activa" : ""}
              onClick={() => setPestana("vista")}
            >
              Vista previa
            </button>
          </div>

          {pestana === "editar" ? (
            <>
              <div className="bfrm-barra">
                {BOTONES.map((b) => (
                  <button
                    key={b.etiqueta}
                    type="button"
                    className="btn-secondary"
                    onClick={b.accion}
                  >
                    {b.etiqueta}
                  </button>
                ))}
              </div>
              <textarea
                ref={areaRef}
                className="bfrm-html"
                rows={12}
                value={form.contenidoHtml}
                onChange={(e) => actualizar("contenidoHtml", e.target.value)}
                placeholder="<p>Escribe aquí el comunicado...</p>"
                aria-label="Contenido en HTML"
              />
            </>
          ) : (
            <div
              className="bfrm-vista"
              // El HTML pasa por DOMPurify antes de mostrarse
              dangerouslySetInnerHTML={{
                __html: vistaPrevia || "<p><em>Aún no hay contenido.</em></p>",
              }}
            />
          )}
          {errores.contenidoHtml && (
            <span className="campo-error">{errores.contenidoHtml}</span>
          )}
        </fieldset>

        <label>
          URL de la imagen (opcional)
          <input
            value={form.imagenUrl}
            onChange={(e) => {
              actualizar("imagenUrl", e.target.value);
              setImagenRota(false);
            }}
            placeholder="https://..."
          />
          <span className="bfrm-ayuda">
            Por ahora se pega el enlace de una imagen ya alojada; no se suben
            archivos.
          </span>
          {errores.imagenUrl && (
            <span className="campo-error">{errores.imagenUrl}</span>
          )}
        </label>

        {form.imagenUrl.trim() && (
          <div className="bfrm-imagen">
            {imagenRota ? (
              <p className="campo-error">
                No se pudo cargar la imagen. Revisa la dirección.
              </p>
            ) : (
              <img
                src={form.imagenUrl.trim()}
                alt="Vista previa de la imagen"
                onError={() => setImagenRota(true)}
              />
            )}
          </div>
        )}

        <fieldset className="bfrm-publicacion">
          <legend>Publicación</legend>

          <div className="bfrm-grid">
            <label className="switch-inline-label">
              <input
                type="checkbox"
                checked={form.publicado}
                onChange={(e) => actualizar("publicado", e.target.checked)}
              />
              Publicado
            </label>

            <label className="switch-inline-label">
              <input
                type="checkbox"
                checked={form.visiblePublico}
                onChange={(e) => actualizar("visiblePublico", e.target.checked)}
              />
              Visible al público
            </label>

            <label>
              Fecha de publicación
              <input
                type="datetime-local"
                value={form.fechaPublicacion}
                onChange={(e) => actualizar("fechaPublicacion", e.target.value)}
              />
              <span className="bfrm-ayuda">
                Si publicas sin fecha, se publica en ese momento. Con una fecha
                futura queda programado.
              </span>
            </label>

            <label>
              Fecha de expiración
              <input
                type="datetime-local"
                value={form.fechaExpiracion}
                onChange={(e) => actualizar("fechaExpiracion", e.target.value)}
              />
              <span className="bfrm-ayuda">
                Opcional. Después de esta fecha deja de mostrarse.
              </span>
              {errores.fechaExpiracion && (
                <span className="campo-error">{errores.fechaExpiracion}</span>
              )}
            </label>
          </div>

          <p className="bfrm-nota">
            Para aparecer en el blog público deben estar marcados{" "}
            <strong>Publicado</strong> y <strong>Visible al público</strong>, y
            la categoría debe estar activa.
          </p>
        </fieldset>

        <div className="form-actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={() => navigate("/blog/comunicados")}
            disabled={guardando}
          >
            Cancelar
          </button>
          <button type="submit" className="btn-primary" disabled={guardando}>
            {guardando
              ? "Guardando..."
              : esEdicion
                ? "Guardar cambios"
                : "Crear comunicado"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default BlogComunicadoForm;
