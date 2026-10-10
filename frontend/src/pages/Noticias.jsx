import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { Icon } from "@iconify/react";
import apiPublica from "../utils/apiPublica";
import { extractErrorMessage } from "../utils/errors";
import { formatearFecha, resumenDe } from "../utils/noticias";
import { ICONOS } from "../utils/iconos";
import "./Noticias.css";

const POR_PAGINA = 9;
const COLOR_POR_DEFECTO = "#2f5a8a";

function Noticias() {
  const [categorias, setCategorias] = useState([]);
  const [categoriaId, setCategoriaId] = useState("");
  const [publicaciones, setPublicaciones] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // Categorías para los filtros. Si fallan, las noticias se ven igual.
  useEffect(() => {
    let activo = true;
    apiPublica
      .get("/blog/categorias", { params: { size: 100 } })
      .then(({ data }) => {
        if (activo) setCategorias(data.content);
      })
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, []);

  const cargar = useCallback(
    async (page = 0) => {
      setCargando(true);
      setError("");
      try {
        const { data } = await apiPublica.get("/blog/publicaciones", {
          params: {
            page,
            size: POR_PAGINA,
            categoriaId: categoriaId || undefined,
          },
        });
        setPublicaciones(data.content);
        setPagina({
          page: data.page,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
        });
      } catch (err) {
        setError(
          extractErrorMessage(err, "No se pudieron cargar las noticias."),
        );
      } finally {
        setCargando(false);
      }
    },
    [categoriaId],
  );

  useEffect(() => {
    cargar(0);
  }, [cargar]);

  const colores = Object.fromEntries(
    categorias.map((c) => [c.id, c.colorHex || COLOR_POR_DEFECTO]),
  );

  const irAPagina = (page) => {
    cargar(page);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <>
      <section className="noticias-hero">
        <span className="noticias-eyebrow">Institución</span>
        <h1 className="noticias-titulo">Noticias y avisos</h1>
        <p className="noticias-subtitulo">
          Mantente al día con lo que sucede en nuestro plantel.
        </p>
      </section>

      <div className="noticias-contenedor">
        {categorias.length > 0 && (
          <div
            className="noticias-filtros"
            role="group"
            aria-label="Filtrar por categoría"
          >
            <button
              type="button"
              className={`noticias-chip${categoriaId === "" ? " noticias-chip--activo" : ""}`}
              onClick={() => setCategoriaId("")}
            >
              Todas
            </button>
            {categorias.map((c) => (
              <button
                key={c.id}
                type="button"
                className={`noticias-chip${String(c.id) === categoriaId ? " noticias-chip--activo" : ""}`}
                style={{ "--color": c.colorHex || COLOR_POR_DEFECTO }}
                onClick={() => setCategoriaId(String(c.id))}
              >
                {c.nombre}
              </button>
            ))}
          </div>
        )}

        {error && (
          <p className="noticias-error" role="alert">
            {error}
          </p>
        )}

        {cargando ? (
          <p className="noticias-estado">Cargando noticias...</p>
        ) : publicaciones.length === 0 && !error ? (
          <p className="noticias-estado">
            <Icon icon={ICONOS.blog} className="noticias-estado-icono" />
            No hay publicaciones por ahora.
          </p>
        ) : (
          <div className="noticias-grid">
            {publicaciones.map((p) => (
              <Link
                key={p.id}
                to={`/noticias/${p.id}`}
                className="noticia-card"
                style={{
                  "--color-categoria":
                    colores[p.categoriaId] || COLOR_POR_DEFECTO,
                }}
              >
                {p.imagenUrl && (
                  <img
                    className="noticia-imagen"
                    src={p.imagenUrl}
                    alt=""
                    loading="lazy"
                    onError={(e) => (e.currentTarget.style.display = "none")}
                  />
                )}
                <div className="noticia-cuerpo">
                  <div className="noticia-meta">
                    {p.categoriaNombre && (
                      <span className="noticia-categoria">
                        {p.categoriaNombre}
                      </span>
                    )}
                    <span className="noticia-fecha">
                      <Icon icon={ICONOS.fecha} />
                      {formatearFecha(p.fechaPublicacion)}
                    </span>
                  </div>
                  <h3>{p.titulo}</h3>
                  <p className="noticia-resumen">{resumenDe(p)}</p>
                  <span className="noticia-leer">Leer más →</span>
                </div>
              </Link>
            ))}
          </div>
        )}

        {pagina.totalPages > 1 && (
          <div className="noticias-paginacion">
            <button
              type="button"
              disabled={pagina.page === 0 || cargando}
              onClick={() => irAPagina(pagina.page - 1)}
            >
              <Icon icon={ICONOS.anterior} /> Anterior
            </button>
            <span>
              Página {pagina.page + 1} de {pagina.totalPages}
            </span>
            <button
              type="button"
              disabled={pagina.page + 1 >= pagina.totalPages || cargando}
              onClick={() => irAPagina(pagina.page + 1)}
            >
              Siguiente <Icon icon={ICONOS.siguiente} />
            </button>
          </div>
        )}
      </div>
    </>
  );
}

export default Noticias;
