import { Link, useNavigate } from "react-router-dom";
import { Icon } from "@iconify/react";
import { getNombre, getRol, logout } from "../utils/auth";
import { ROLES } from "../constants/roles";
import { ICONOS } from "../utils/iconos";
import "./Topbar.css";

function Topbar({ onToggleMenu }) {
  const navigate = useNavigate();
  const rol = getRol();
  const nombre = getNombre();
  const esEscaner = rol === ROLES.ESCANER;

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <header className={`topbar${onToggleMenu ? " topbar--con-menu" : ""}`}>
      <div className="topbar-izquierda">
        {onToggleMenu && (
          <button
            type="button"
            className="topbar-menu-btn"
            onClick={onToggleMenu}
            aria-label="Mostrar u ocultar el menú"
          >
            <Icon icon={ICONOS.menu} />
          </button>
        )}

        <Link
          className="topbar-brand"
          to={esEscaner ? "/escaner" : "/dashboard"}
        >
          <img
            src="/img/logo_asansa.jpeg"
            alt="Logo institucional"
            onError={(e) => (e.currentTarget.style.display = "none")}
          />
          <div className="topbar-brand-text">
            <strong>C.E.N. Roraima</strong>
            <span>Control de asistencia</span>
          </div>
        </Link>
      </div>

      {esEscaner && (
        <nav className="topbar-nav">
          <Link to="/asistencia/hoy" className="btn-back">
            Asistencia de hoy
          </Link>
          <Link to="/asistencia/historico" className="btn-back">
            Histórico
          </Link>
        </nav>
      )}

      <div className="topbar-right">
        {nombre && (
          <span className="topbar-usuario">
            <Icon icon={ICONOS.usuario} className="topbar-usuario-icono" />
            <span className="topbar-nombre">{nombre}</span>
          </span>
        )}
        <button
          className="btn-logout"
          onClick={handleLogout}
          aria-label="Cerrar sesión"
          title="Cerrar sesión"
        >
          <Icon icon={ICONOS.salir} className="btn-logout-icono" />
          <span className="btn-logout-texto">Cerrar sesión</span>
        </button>
      </div>
    </header>
  );
}

export default Topbar;
