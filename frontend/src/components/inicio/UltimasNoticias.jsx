import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import apiPublica from "../../utils/apiPublica";
import { formatearFecha, resumenDe } from "../../utils/noticias";

function UltimasNoticias() {
  const [items, setItems] = useState([]);
  const [estado, setEstado] = useState("cargando"); // cargando | listo | error

  useEffect(() => {
    let activo = true;
    apiPublica
      .get("/blog/publicaciones", { params: { page: 0, size: 3 } })
      .then(({ data }) => {
        if (!activo) return;
        setItems(data.content ?? []);
        setEstado("listo");
      })
      .catch(() => {
        if (activo) setEstado("error");
      });
    return () => {
      activo = false;
    };
  }, []);

  return (
    <section className="ini-seccion ini-noticias" id="noticias-inicio">
      <div className="ini-contenedor">
        <h2 className="ini-titulo">Últimas noticias</h2>
        <p className="ini-subtitulo">
          Avisos y comunicados recientes del plantel.
        </p>

        {estado === "cargando" && (
          <p className="ini-noticias-estado">Cargando noticias...</p>
        )}
        {estado === "error" && (
          <p className="ini-noticias-estado">
            No se pudieron cargar las noticias en este momento.
          </p>
        )}
        {estado === "listo" && items.length === 0 && (
          <p className="ini-noticias-estado">Aún no hay noticias publicadas.</p>
        )}

        {items.length > 0 && (
          <div className="ini-noticias-grid">
            {items.map((n) => (
              <article key={n.id} className="ini-noticia">
                {n.imagenUrl && (
                  <img
                    className="ini-noticia-img"
                    src={n.imagenUrl}
                    alt=""
                    loading="lazy"
                  />
                )}
                <div className="ini-noticia-cuerpo">
                  {n.categoriaNombre && (
                    <span className="ini-noticia-categoria">
                      {n.categoriaNombre}
                    </span>
                  )}
                  <h3>
                    <Link to={`/noticias/${n.id}`}>{n.titulo}</Link>
                  </h3>
                  <time className="ini-noticia-fecha">
                    {formatearFecha(n.fechaPublicacion)}
                  </time>
                  <p>{resumenDe(n)}</p>
                </div>
              </article>
            ))}
          </div>
        )}

        <div className="ini-noticias-pie">
          <Link to="/noticias" className="ini-boton-borde">
            Ver todas las noticias
          </Link>
        </div>
      </div>
    </section>
  );
}

export default UltimasNoticias;
