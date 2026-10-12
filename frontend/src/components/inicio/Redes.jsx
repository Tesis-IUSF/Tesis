import { Icon } from "@iconify/react";
import { ICONOS } from "../../utils/iconos";

const REDES = [
  {
    nombre: "Instagram",
    icono: ICONOS.instagram,
    url: "https://instagram.com",
  },
  { nombre: "Facebook", icono: ICONOS.facebook, url: "https://facebook.com" },
];

function Redes() {
  return (
    <section className="ini-seccion ini-redes">
      <div className="ini-contenedor">
        <h2 className="ini-titulo">Síguenos en redes</h2>
        <p className="ini-subtitulo">
          Entérate de las actividades del plantel.
        </p>
        <div className="ini-redes-lista">
          {REDES.map((r) => (
            <a
              key={r.nombre}
              href={r.url}
              target="_blank"
              rel="noopener noreferrer"
              className="ini-red"
            >
              <Icon icon={r.icono} width="26" height="26" />
              <span>{r.nombre}</span>
            </a>
          ))}
        </div>
      </div>
    </section>
  );
}

export default Redes;
