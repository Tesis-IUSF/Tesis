import { Link } from "react-router-dom";
import { Icon } from "@iconify/react";
import { ICONOS } from "../../utils/iconos";
import { isAuthenticated, getRol } from "../../utils/auth";
import { ROLES } from "../../constants/roles";

function BandaAsistencia() {
  const sesion = isAuthenticated();
  const destino = !sesion
    ? "/login"
    : getRol() === ROLES.ESCANER
      ? "/escaner"
      : "/dashboard";

  return (
    <section className="ini-banda" aria-labelledby="ini-banda-titulo">
      <div className="ini-contenedor ini-banda-interior">
        <span className="ini-banda-icono" aria-hidden="true">
          <Icon icon={ICONOS.reloj} width="40" height="40" />
        </span>
        <div className="ini-banda-texto">
          <h2 id="ini-banda-titulo">Control de Asistencia</h2>
          <p>
            Sistema institucional para registrar y consultar la asistencia del
            personal del plantel.
          </p>
        </div>
        <Link to={destino} className="ini-banda-boton">
          {sesion ? "Ir al sistema" : "Acceder al sistema"}
        </Link>
      </div>
    </section>
  );
}

export default BandaAsistencia;
