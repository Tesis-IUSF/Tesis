import { Link } from "react-router-dom";

function NoAutorizado() {
  return (
    <div
      className="no-autorizado"
      style={{ padding: "3rem", textAlign: "center" }}
    >
      <h1>Acceso no autorizado</h1>
      <p>No tienes permisos para ver esta página.</p>
      <Link to="/login">Volver al inicio de sesión</Link>
    </div>
  );
}

export default NoAutorizado;
