import { Link, useNavigate } from "react-router-dom";
import { getNombre, getRol, logout } from "../utils/auth";
import { ROLES } from "../constants/roles";
import "./Topbar.css";

function Topbar({ mostrarVolver = false }) {
  const navigate = useNavigate();
  const rol = getRol();
  const nombre = getNombre();
  const esEscaner = rol === ROLES.ESCANER;

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const puedeVerCarnets = rol === ROLES.ADMIN || rol === ROLES.DIRECTOR;

  return (
    <header className="topbar">
      <Link className="topbar-brand" to={esEscaner ? "/escaner" : "/dashboard"}>
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

      <nav className="topbar-nav">
        {mostrarVolver && (
          <Link to={esEscaner ? "/escaner" : "/dashboard"} className="btn-back">
            ← Volver
          </Link>
        )}

        {!esEscaner && (
          <>
            <Link to="/empleados" className="btn-back">
              Empleados
            </Link>
            <Link to="/asistencia/hoy" className="btn-back">
              Asistencia de hoy
            </Link>
            <Link to="/asistencia/historico" className="btn-back">
              Histórico
            </Link>
            <Link to="/turnos" className="btn-back">
              Turnos
            </Link>
            {puedeVerCarnets && (
              <Link to="/carnets" className="btn-back">
                Carnets
              </Link>
            )}
          </>
        )}

        {esEscaner && (
          <>
            <Link to="/asistencia/hoy" className="btn-back">
              Asistencia de hoy
            </Link>
            <Link to="/asistencia/historico" className="btn-back">
              Histórico
            </Link>
          </>
        )}
      </nav>

      <div className="topbar-right">
        {nombre && <span className="topbar-nombre">{nombre}</span>}
        <button className="btn-logout" onClick={handleLogout}>
          Cerrar sesión
        </button>
      </div>
    </header>
  );
}

export default Topbar;
