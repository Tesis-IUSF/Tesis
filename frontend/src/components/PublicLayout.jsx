import { Link, Outlet } from "react-router-dom";
import { Icon } from "@iconify/react";
import { getRol, isAuthenticated } from "../utils/auth";
import { ROLES } from "../constants/roles";
import { ICONOS } from "../utils/iconos";
import "./PublicLayout.css";

function PublicLayout() {
  const hayConSesion = isAuthenticated();
  const destino = !hayConSesion
    ? "/login"
    : getRol() === ROLES.ESCANER
      ? "/escaner"
      : "/dashboard";

  return (
    <div className="publico">
      <header className="publico-header">
        <Link className="publico-brand" to="/noticias">
          <img
            src="/img/logo_asansa.jpeg"
            alt="Escudo institucional"
            onError={(e) => (e.currentTarget.style.display = "none")}
          />
          <div className="publico-brand-texto">
            <strong>C.E.N. Roraima Asansa</strong>
            <small>Escándela</small>
          </div>
        </Link>

        <nav className="publico-nav">
          <Link to="/noticias" className="publico-link">
            Noticias
          </Link>
          <Link to={destino} className="publico-acceso">
            <Icon icon={ICONOS.usuario} />
            <span>{hayConSesion ? "Ir al sistema" : "Acceder"}</span>
          </Link>
        </nav>
      </header>

      <main className="publico-main">
        <Outlet />
      </main>

      <footer className="publico-footer">
        <div className="publico-footer-top">
          <div className="publico-footer-marca">
            <img
              src="/img/logo_asansa.jpeg"
              alt=""
              onError={(e) => (e.currentTarget.style.display = "none")}
            />
            <div>
              <strong>
                C.E.N. Lcda. Roraima
                <br />
                Asansa Escándela
              </strong>
              <small>Complejo Educativo</small>
            </div>
          </div>

          <ul className="publico-contacto">
            <li>
              <Icon icon={ICONOS.telefono} />
              <span>(0412) 368-0055</span>
            </li>
            <li>
              <Icon icon={ICONOS.ubicacion} />
              <span>
                Urb. Villa Chinita, Calle N.º 1 Av. N.º 1, Parroquia San
                Francisco
              </span>
            </li>
            <li>
              <Icon icon={ICONOS.correo} />
              <span>info@asansa.edu.ve</span>
            </li>
          </ul>
        </div>
        <p className="publico-copy">
          Complejo Educativo Roraima Asansa Escándela © 2026 · Todos los
          derechos reservados
        </p>
      </footer>
    </div>
  );
}

export default PublicLayout;
