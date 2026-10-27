import { Icon } from "@iconify/react";
import { ICONOS } from "../../utils/iconos";
import useAparecer from "../../utils/useAparecer";

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

const PUNTOS = [
  "Formación integral",
  "Disciplina y valores",
  "Compromiso con la comunidad",
];

function Nosotros() {
  const [ref, visible] = useAparecer(0.15);

  return (
    <section id="mision" className="ini-seccion ini-nosotros" ref={ref}>
      <div className="ini-contenedor">
        <h2 className="ini-titulo">Quiénes somos</h2>
        <p className="ini-subtitulo">
          Una escuela que forma con disciplina, valores y compromiso con la
          comunidad.
        </p>

        <div
          className={`ini-nos-intro${visible ? " ini-nos-intro--visible" : ""}`}
        >
          <div className="ini-nos-texto">
            <span className="ini-nos-etiqueta">
              C.E.N. Licenciada Roraima Asansa Escandela
            </span>
            <h3>Educación con disciplina, valores y comunidad</h3>
            <p>
              Somos una institución educativa de la Parroquia San Francisco que
              forma estudiantes integrales, con conocimientos, disciplina y
              valores.
            </p>
            <p>
              Trabajamos junto a representantes, docentes y comunidad para que
              cada estudiante crezca con respeto, responsabilidad y sentido de
              pertenencia.
            </p>
            <ul className="ini-nos-puntos">
              {PUNTOS.map((punto) => (
                <li key={punto}>
                  <Icon icon={ICONOS.check} width="18" height="18" />
                  {punto}
                </li>
              ))}
            </ul>
            <a href="#galeria" className="ini-boton-borde">
              Conoce nuestro plantel
            </a>
          </div>

          <figure className="ini-nos-foto">
            <img src="/img/aula.jpeg" alt="Aula del plantel" loading="lazy" />
          </figure>
        </div>

        <div className="ini-tarjetas">
          {TARJETAS.map((t, i) => (
            <article
              key={t.clave}
              className={`ini-tarjeta ini-tarjeta--${t.clave}${
                visible ? " ini-tarjeta--visible" : ""
              }`}
              style={{ transitionDelay: `${300 + i * 120}ms` }}
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
