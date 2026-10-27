import AnimatedSelect from "../components/AnimatedSelect";
// src/pages/BlogComunicados.jsx
import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { Icon } from "@iconify/react";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import "./BlogComunicados.css";

function formatFecha(valor) {
  if (!valor) return "—";
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime())
    ? "—"
    : fecha.toLocaleString("es-VE", { dateStyle: "short", timeStyle: "short" });
}

// Estado según las mismas reglas que usa la página pública.
function estadoDe(c) {
  const ahora = new Date();
  if (!c.publicado) return { clave: "borrador", etiqueta: "Borrador" };
  if (c.fechaPublicacion && new Date(c.fechaPublicacion) > ahora)
    return { clave: "programado", etiqueta: "Programado" };
  if (c.fechaExpiracion && new Date(c.fechaExpiracion) < ahora)
    return { clave: "vencido", etiqueta: "Vencido" };
  return { clave: "publicado", etiqueta: "Publicado" };
}

function BlogComunicados() {
  const toast = useToast();
  const [filas, setFilas] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const [categorias, setCategorias] = useState([]);
  const [categoriaId, setCategoriaId] = useState("");
  const [publicado, setPublicado] = useState(""); // "" | "true" | "false"

  useEffect(() => {
    api
      .get("/blog/admin/categorias", { params: { page: 0, size: 100 } })
      .then(({ data }) => setCategorias(data.content))
      .catch(() => setCategorias([]));
  }, []);

  const cargar = useCallback(
    async (page = 0) => {
      setCargando(true);
      setError("");
      try {
        const { data } = await api.get("/blog/admin/comunicados", {
          params: {
            page,
            size: 25,
            categoriaId: categoriaId || undefined,
            publicado: publicado === "" ? undefined : publicado === "true",
          },
        });
        setFilas(data.content);
        setPagina({
          page: data.page,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
        });
      } catch (err) {
        setError(
          extractErrorMessage(
            err,
            "No se pudo cargar la lista de comunicados.",
          ),
        );
      } finally {
        setCargando(false);
      }
    },
    [categoriaId, publicado],
  );

  // Recarga desde la primera página al cambiar los filtros
  useEffect(() => {
    cargar(0);
  }, [cargar]);

  const eliminar = async (c) => {
    if (!window.confirm(`¿Eliminar el comunicado "${c.titulo}"?`)) return;
    try {
      await api.delete(`/blog/admin/comunicados/${c.id}`);
      toast.success("Comunicado eliminado correctamente.");
      // Si era el único de la página, retrocede una
      const destino =
        filas.length === 1 && pagina.page > 0 ? pagina.page - 1 : pagina.page;
      cargar(destino);
    } catch (err) {
      toast.error(
        extractErrorMessage(err, "No se pudo eliminar el comunicado."),
      );
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Comunicados</h1>
          <div className="mod-heading-actions">
            <Link to="/blog/comunicados/nuevo" className="btn-primary">
              + Crear comunicado
            </Link>
          </div>
        </div>

        <div className="filters-bar">
          <label>
            Categoría
            <AnimatedSelect
              value={categoriaId}
              onChange={(e) => setCategoriaId(e.target.value)}
            >
              <option value="">Todas</option>
              {categorias.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                  {c.activo ? "" : " (inactiva)"}
                </option>
              ))}
            </AnimatedSelect>
          </label>

          <label>
            Estado
            <AnimatedSelect
              value={publicado}
              onChange={(e) => setPublicado(e.target.value)}
            >
              <option value="">Todos</option>
              <option value="true">Publicados</option>
              <option value="false">Borradores</option>
            </AnimatedSelect>
          </label>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Título</th>
                <th>Categoría</th>
                <th>Estado</th>
                <th>Visible al público</th>
                <th>Publicación</th>
                <th>Expira</th>
                <th>Orden</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={8} className="tabla-estado">
                    Cargando comunicados...
                  </td>
                </tr>
              ) : filas.length === 0 ? (
                <tr>
                  <td colSpan={8} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.comunicados} />
                    </span>
                    No hay comunicados que coincidan con los filtros.
                  </td>
                </tr>
              ) : (
                filas.map((c) => {
                  const estado = estadoDe(c);
                  return (
                    <tr key={c.id}>
                      <td data-label="Título">{c.titulo}</td>
                      <td data-label="Categoría">{c.categoriaNombre}</td>
                      <td data-label="Estado">
                        <span className={`badge bcom-estado--${estado.clave}`}>
                          {estado.etiqueta}
                        </span>
                      </td>
                      <td data-label="Visible al público">
                        {c.visiblePublico ? "Sí" : "No"}
                      </td>
                      <td data-label="Publicación">
                        {formatFecha(c.fechaPublicacion)}
                      </td>
                      <td data-label="Expira">
                        {formatFecha(c.fechaExpiracion)}
                      </td>
                      <td data-label="Orden">{c.ordenVisualizacion ?? 0}</td>
                      <td data-label="Acciones">
                        <Link
                          to={`/blog/comunicados/${c.id}/editar`}
                          className="btn-edit"
                        >
                          Editar
                        </Link>
                        <button
                          type="button"
                          className="btn-delete"
                          onClick={() => eliminar(c)}
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

        {pagina.totalPages > 1 && (
          <div className="paginacion">
            <button
              disabled={pagina.page === 0 || cargando}
              onClick={() => cargar(pagina.page - 1)}
            >
              ← Anterior
            </button>
            <span>
              Página {pagina.page + 1} de {pagina.totalPages} (
              {pagina.totalElements} comunicados)
            </span>
            <button
              disabled={pagina.page + 1 >= pagina.totalPages || cargando}
              onClick={() => cargar(pagina.page + 1)}
            >
              Siguiente →
            </button>
          </div>
        )}
      </div>
    </>
  );
}

export default BlogComunicados;
