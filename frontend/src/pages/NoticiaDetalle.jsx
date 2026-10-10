import { useState, useEffect, useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { Icon } from "@iconify/react";
import DOMPurify from "dompurify";
import apiPublica from "../utils/apiPublica";
import { extractErrorMessage } from "../utils/errors";
import { formatearFecha } from "../utils/noticias";
import { ICONOS } from "../utils/iconos";
import "./Noticias.css";

function NoticiaDetalle() {
  const { id } = useParams();
  const [noticia, setNoticia] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let activo = true;
    apiPublica
      .get(`/blog/publicaciones/${id}`)
      .then(({ data }) => {
        if (activo) setNoticia(data);
      })
      .catch((err) => {
        if (!activo) return;
        setError(
          err.response?.status === 404
            ? "Esta publicación no existe o ya no está disponible."
            : extractErrorMessage(err, "No se pudo cargar la publicación."),
        );
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [id]);

  // El contenido es HTML escrito por el personal: se limpia antes de mostrarlo.
  const contenidoLimpio = useMemo(
    () => (noticia ? DOMPurify.sanitize(noticia.contenidoHtml || "") : ""),
    [noticia],
  );

  return (
    <article className="noticia-detalle">
      <Link to="/noticias" className="noticia-volver">
        <Icon icon={ICONOS.volver} /> Volver a las noticias
      </Link>

      {cargando && <p className="noticias-estado">Cargando publicación...</p>}

      {error && (
        <p className="noticias-error" role="alert">
          {error}
        </p>
      )}

      {noticia && (
        <>
          <div className="noticia-meta">
            {noticia.categoriaNombre && (
              <span className="noticia-categoria">
                {noticia.categoriaNombre}
              </span>
            )}
            <span className="noticia-fecha">
              <Icon icon={ICONOS.fecha} />
              {formatearFecha(noticia.fechaPublicacion)}
            </span>
          </div>

          <h1>{noticia.titulo}</h1>

          {noticia.imagenUrl && (
            <img
              className="noticia-detalle-imagen"
              src={noticia.imagenUrl}
              alt=""
              onError={(e) => (e.currentTarget.style.display = "none")}
            />
          )}

          <div
            className="noticia-contenido"
            dangerouslySetInnerHTML={{ __html: contenidoLimpio }}
          />
        </>
      )}
    </article>
  );
}

export default NoticiaDetalle;
