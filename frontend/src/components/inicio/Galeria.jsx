import { useCallback, useEffect, useState } from "react";
import { Icon } from "@iconify/react";
import { ICONOS } from "../../utils/iconos";
import useAparecer from "../../utils/useAparecer";

// Mismas rutas públicas que usa el Hero (carpeta public/img).
// El mosaico está pensado para 4 fotos: la primera se muestra más grande.
const FOTOS = [
  {
    src: "/img/asansa.jpeg",
    alt: "Fachada del plantel",
    leyenda: "Nuestro plantel",
  },
  {
    src: "/img/grupo_premilitar.jpeg",
    alt: "Grupo premilitar",
    leyenda: "Grupo premilitar",
  },
  {
    src: "/img/estudiantes_premilitar.jpeg",
    alt: "Estudiantes premilitares",
    leyenda: "Formación y disciplina",
  },
  {
    src: "/img/representantes.jpeg",
    alt: "Representantes reunidos",
    leyenda: "Comunidad educativa",
  },
];

function Galeria() {
  const total = FOTOS.length;
  const [ref, visible] = useAparecer(0.15);
  const [abierta, setAbierta] = useState(null);

  const cerrar = useCallback(() => setAbierta(null), []);
  const mover = useCallback(
    (paso) => setAbierta((i) => (i === null ? i : (i + paso + total) % total)),
    [total],
  );

  // Teclado y bloqueo del scroll mientras la foto está ampliada
  useEffect(() => {
    if (abierta === null) return undefined;
    const alPulsar = (evento) => {
      if (evento.key === "Escape") cerrar();
      if (evento.key === "ArrowLeft") mover(-1);
      if (evento.key === "ArrowRight") mover(1);
    };
    const overflowPrevio = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", alPulsar);
    return () => {
      document.removeEventListener("keydown", alPulsar);
      document.body.style.overflow = overflowPrevio;
    };
  }, [abierta, cerrar, mover]);

  if (total === 0) return null;

  const actual = abierta === null ? null : FOTOS[abierta];

  return (
    <section id="galeria" className="ini-seccion ini-galeria" ref={ref}>
      <div className="ini-contenedor">
        <h2 className="ini-titulo">Galería</h2>
        <p className="ini-subtitulo">
          Momentos de nuestra vida escolar. Toca una foto para verla en grande.
        </p>

        <div
          className={`ini-gal-grid${visible ? " ini-gal-grid--visible" : ""}`}
        >
          {FOTOS.map((f, i) => (
            <button
              key={f.src}
              type="button"
              className="ini-gal-foto"
              style={{ transitionDelay: `${i * 110}ms` }}
              onClick={() => setAbierta(i)}
              aria-label={`Ampliar foto: ${f.leyenda}`}
            >
              <img src={f.src} alt={f.alt} loading="lazy" />
              <span className="ini-gal-leyenda">{f.leyenda}</span>
            </button>
          ))}
        </div>
      </div>

      {actual && (
        <div
          className="ini-lb"
          role="dialog"
          aria-modal="true"
          aria-label={`Foto ampliada: ${actual.leyenda}`}
          onClick={cerrar}
        >
          <button
            type="button"
            className="ini-lb-boton ini-lb-cerrar"
            onClick={cerrar}
            aria-label="Cerrar"
            autoFocus
          >
            <Icon icon={ICONOS.cerrar} width="26" height="26" />
          </button>

          {total > 1 && (
            <button
              type="button"
              className="ini-lb-boton ini-lb-flecha ini-lb-flecha--prev"
              onClick={(evento) => {
                evento.stopPropagation();
                mover(-1);
              }}
              aria-label="Foto anterior"
            >
              <Icon icon={ICONOS.anterior} width="28" height="28" />
            </button>
          )}

          <figure
            className="ini-lb-figura"
            key={abierta}
            onClick={(evento) => evento.stopPropagation()}
          >
            <img src={actual.src} alt={actual.alt} />
            <figcaption>
              {actual.leyenda}
              <span>
                {abierta + 1} / {total}
              </span>
            </figcaption>
          </figure>

          {total > 1 && (
            <button
              type="button"
              className="ini-lb-boton ini-lb-flecha ini-lb-flecha--sig"
              onClick={(evento) => {
                evento.stopPropagation();
                mover(1);
              }}
              aria-label="Foto siguiente"
            >
              <Icon icon={ICONOS.siguiente} width="28" height="28" />
            </button>
          )}
        </div>
      )}
    </section>
  );
}

export default Galeria;
