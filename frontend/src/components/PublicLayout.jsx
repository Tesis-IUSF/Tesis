import { useEffect, useState } from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { Icon } from "@iconify/react";
import { getRol, isAuthenticated } from "../utils/auth";
import { ROLES } from "../constants/roles";
import { ICONOS } from "../utils/iconos";
import "./PublicLayout.css";

function comportamientoDesplazamiento() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ? "auto"
    : "smooth";
}

function PublicLayout() {
  const { pathname, hash } = useLocation();
  const [menuAbierto, setMenuAbierto] = useState(false);

  const hayConSesion = isAuthenticated();
  const destino = !hayConSesion
    ? "/login"
    : getRol() === ROLES.ESCANER
      ? "/escaner"
      : "/dashboard";

  // Al llegar a una ruta con #ancla (por ejemplo "/#galeria") baja hasta esa
  // sección; en cualquier otra navegación vuelve arriba.
  useEffect(() => {
    if (!hash) {
      window.scrollTo(0, 0);
      return;
    }
    const temporizador = setTimeout(() => {
      document
        .getElementById(hash.slice(1))
        ?.scrollIntoView({ behavior: comportamientoDesplazamiento() });
    }, 50);
    return () => clearTimeout(temporizador);
  }, [pathname, hash]);

  const cerrarMenu = () => setMenuAbierto(false);

  // Enlace a una sección del inicio: si ya estamos ahí, solo se desplaza.
  const irASeccion = (e, id) => {
    setMenuAbierto(false);
    if (pathname === "/") {
      e.preventDefault();
      document
        .getElementById(id)
        ?.scrollIntoView({ behavior: comportamientoDesplazamiento() });
    }
  };

  return (
    <div className="publico">
      <header className="publico-header">
        <Link className="publico-brand" to="/" onClick={cerrarMenu}>
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

        <button
          type="button"
          className="publico-toggle"
          onClick={() => setMenuAbierto((abierto) => !abierto)}
          aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
          aria-expanded={menuAbierto}
          aria-controls="publico-nav"
        >
          <Icon icon={menuAbierto ? ICONOS.cerrar : ICONOS.menu} />
        </button>

        <nav
          id="publico-nav"
          className={`publico-nav${menuAbierto ? " publico-nav--abierto" : ""}`}
        >
          <Link
            to="/#mision"
            className="publico-link"
            onClick={(e) => irASeccion(e, "mision")}
          >
            Nosotros
          </Link>
          <Link
            to="/#galeria"
            className="publico-link"
            onClick={(e) => irASeccion(e, "galeria")}
          >
            Galería
          </Link>
          <Link to="/noticias" className="publico-link" onClick={cerrarMenu}>
            Noticias
          </Link>
          <Link
            to="/#contacto"
            className="publico-link"
            onClick={(e) => irASeccion(e, "contacto")}
          >
            Contacto
          </Link>
          <Link to={destino} className="publico-acceso" onClick={cerrarMenu}>
            <Icon icon={ICONOS.usuario} />
            <span>{hayConSesion ? "Ir al sistema" : "Acceder"}</span>
          </Link>
        </nav>
      </header>

      <main className="publico-main">
        <Outlet />
      </main>

      <footer className="publico-footer" id="contacto">
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

          <nav className="publico-interes" aria-label="De interés">
            <h3>De interés</h3>
            <ul>
              <li>
                <Link to="/#mision" onClick={(e) => irASeccion(e, "mision")}>
                  Misión y Visión
                </Link>
              </li>
              <li>
                <Link to="/#galeria" onClick={(e) => irASeccion(e, "galeria")}>
                  Galería
                </Link>
              </li>
              <li>
                <Link to="/noticias">Noticias</Link>
              </li>
            </ul>
          </nav>

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
