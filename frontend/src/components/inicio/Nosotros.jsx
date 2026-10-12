import { useEffect, useRef, useState } from "react";
import { Icon } from "@iconify/react";
import { ICONOS } from "../../utils/iconos";

const TARJETAS = [
  {
    clave: "mision",
    icono: ICONOS.mision,
    titulo: "Misión",
    texto:
      "Formar estudiantes integrales, con conocimientos, disciplina y valores, que aporten al desarrollo de su comunidad y del país.",
  },
  {
    clave: "vision",
    icono: ICONOS.vision,
    titulo: "Visión",
    texto:
      "Ser una institución de referencia en la Parroquia San Francisco por la excelencia de su enseñanza y el compromiso de su comunidad educativa.",
  },
  {
    clave: "valores",
    icono: ICONOS.valores,
    titulo: "Valores",
    texto:
      "Respeto, responsabilidad, disciplina, solidaridad y sentido de pertenencia guían cada actividad de nuestro plantel.",
  },
];

function Nosotros() {
  const ref = useRef(null);
  const [visible, setVisible] = useState(false);

  // Aparece una sola vez al entrar en pantalla
  useEffect(() => {
    const el = ref.current;
    if (!el || typeof IntersectionObserver === "undefined") {
      setVisible(true);
      return undefined;
    }
    const obs = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          setVisible(true);
          obs.disconnect();
        }
      },
      { threshold: 0.2 },
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  return (
    <section id="mision" className="ini-seccion ini-nosotros" ref={ref}>
      <div className="ini-contenedor">
        <h2 className="ini-titulo">Quiénes somos</h2>
        <p className="ini-subtitulo">
          Una escuela que forma con disciplina, valores y compromiso con la
          comunidad.
        </p>

        <div className="ini-tarjetas">
          {TARJETAS.map((t, i) => (
            <article
              key={t.clave}
              className={`ini-tarjeta ini-tarjeta--${t.clave}${
                visible ? " ini-tarjeta--visible" : ""
              }`}
              style={{ transitionDelay: `${i * 120}ms` }}
            >
              <span className="ini-tarjeta-icono" aria-hidden="true">
                <Icon icon={t.icono} width="28" height="28" />
              </span>
              <h3>{t.titulo}</h3>
              <p>{t.texto}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

export default Nosotros;
