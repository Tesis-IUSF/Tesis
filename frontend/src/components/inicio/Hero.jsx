import { useEffect, useState } from "react";
import { Icon } from "@iconify/react";
import { ICONOS } from "../../utils/iconos";

// Cada foto lleva su frase (igual que en el sitio anterior).
const DIAPOSITIVAS = [
  {
    imagen: "/img/asansa.jpeg",
    frase: "Educación con valores, formando el futuro de nuestra comunidad.",
  },
  {
    imagen: "/img/grupo_premilitar.jpeg",
    frase: "La disciplina es el puente entre metas y logros.",
  },
  {
    imagen: "/img/estudiantes_premilitar.jpeg",
    frase: "Nuestros estudiantes son el orgullo de la Parroquia San Francisco.",
  },
  {
    imagen: "/img/representantes.jpeg",
    frase: "Unidos construimos una comunidad educativa de excelencia.",
  },
];

const INTERVALO_MS = 5000;

function Hero() {
  const [indice, setIndice] = useState(0);
  const total = DIAPOSITIVAS.length;

  // Avanza sola cada 5 s. Como depende de "indice", cualquier cambio de foto
  // (también al pulsar las flechas o los puntos) reinicia el reloj.
  useEffect(() => {
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches) {
      return;
    }
    const temporizador = setTimeout(
      () => setIndice((actual) => (actual + 1) % total),
      INTERVALO_MS,
    );
    return () => clearTimeout(temporizador);
  }, [indice, total]);

  const irA = (nuevo) => setIndice((nuevo + total) % total);

  return (
    <section
      className="ini-hero"
      id="inicio"
      aria-roledescription="carrusel"
      aria-label="Fotos de la institución"
    >
      <div className="ini-hero-slides">
        {DIAPOSITIVAS.map((d, i) => (
          <div
            key={d.imagen}
            className={`ini-hero-slide${i === indice ? " activa" : ""}`}
            style={{ backgroundImage: `url(${d.imagen})` }}
            aria-hidden={i !== indice}
          />
        ))}
      </div>

      <div className="ini-hero-contenido">
        <h1 className="ini-hero-titulo">
          C.E.N. Lcda. Roraima
          <br />
          Asansa Escándela
        </h1>
      </div>

      <div className="ini-hero-frase-caja">
        {/* "key" hace que la frase aparezca con un fundido en cada cambio */}
        <p key={indice} className="ini-hero-frase" aria-live="polite">
          “{DIAPOSITIVAS[indice].frase}”
        </p>
      </div>

      <button
        type="button"
        className="ini-hero-flecha ini-hero-flecha--prev"
        onClick={() => irA(indice - 1)}
        aria-label="Foto anterior"
      >
        <Icon icon={ICONOS.anterior} />
      </button>
      <button
        type="button"
        className="ini-hero-flecha ini-hero-flecha--sig"
        onClick={() => irA(indice + 1)}
        aria-label="Foto siguiente"
      >
        <Icon icon={ICONOS.siguiente} />
      </button>

      <div className="ini-hero-puntos">
        {DIAPOSITIVAS.map((d, i) => (
          <button
            key={d.imagen}
            type="button"
            className={`ini-hero-punto${i === indice ? " activo" : ""}`}
            onClick={() => irA(i)}
            aria-label={`Ir a la foto ${i + 1}`}
            aria-current={i === indice}
          />
        ))}
      </div>

      <div className="ini-hero-rapido">
        <div className="ini-rapido-tarjeta">
          <strong>Año Escolar</strong>
          <span>2026 – 2027</span>
        </div>
        <div className="ini-rapido-tarjeta">
          <strong>Control de Asistencia</strong>
          <span>Sistema institucional en línea</span>
        </div>
        <div className="ini-rapido-tarjeta">
          <strong>Comunidad Educativa</strong>
          <span>Personal, docentes y directivos</span>
        </div>
      </div>
    </section>
  );
}

export default Hero;
