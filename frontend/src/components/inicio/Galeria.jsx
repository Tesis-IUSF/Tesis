import { useEffect, useState } from "react";
import { Icon } from "@iconify/react";
import { ICONOS } from "../../utils/iconos";

// Mismas rutas públicas que usa el Hero (carpeta public/img)
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

const INTERVALO = 4000;

function Galeria() {
  const total = FOTOS.length;
  const [indice, setIndice] = useState(0);

  useEffect(() => {
    if (total < 2) return undefined;
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches) {
      return undefined;
    }
    const t = setTimeout(() => setIndice((i) => (i + 1) % total), INTERVALO);
    return () => clearTimeout(t);
  }, [indice, total]);

  if (total === 0) return null;

  const irA = (n) => setIndice((n + total) % total);

  return (
    <section id="galeria" className="ini-seccion ini-galeria">
      <div className="ini-contenedor">
        <h2 className="ini-titulo">Galería</h2>
        <p className="ini-subtitulo">Momentos de nuestra vida escolar.</p>

        <div className="ini-galeria-visor">
          <div
            className="ini-galeria-pista"
            style={{ transform: `translateX(-${indice * 100}%)` }}
          >
            {FOTOS.map((f, i) => (
              <figure
                key={f.src}
                className="ini-galeria-diapo"
                aria-hidden={i !== indice}
              >
                <img src={f.src} alt={f.alt} loading="lazy" />
                <figcaption>{f.leyenda}</figcaption>
              </figure>
            ))}
          </div>

          {total > 1 && (
            <>
              <button
                type="button"
                className="ini-galeria-flecha ini-galeria-flecha--prev"
                onClick={() => irA(indice - 1)}
                aria-label="Foto anterior"
              >
                <Icon icon={ICONOS.anterior} width="24" height="24" />
              </button>
              <button
                type="button"
                className="ini-galeria-flecha ini-galeria-flecha--sig"
                onClick={() => irA(indice + 1)}
                aria-label="Foto siguiente"
              >
                <Icon icon={ICONOS.siguiente} width="24" height="24" />
              </button>
            </>
          )}
        </div>

        {total > 1 && (
          <div className="ini-galeria-puntos">
            {FOTOS.map((f, i) => (
              <button
                key={f.src}
                type="button"
                className={`ini-galeria-punto${i === indice ? " activo" : ""}`}
                onClick={() => irA(i)}
                aria-label={`Ir a la foto ${i + 1}`}
                aria-current={i === indice}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

export default Galeria;
