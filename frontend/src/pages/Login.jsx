import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Login.css";

function Login() {
  const [usuario, setUsuario] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const navigate = useNavigate();

  const iniciarSesion = async () => {
    if (!usuario.trim() || !password.trim()) {
      setError("Por favor completa todos los campos.");
      return;
    }
    setCargando(true);
    setError("");
    try {
      // TODO: conectar con services/api.js más adelante
      console.log("Login simulado con:", usuario);
      navigate("/dashboard");
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        setError("Usuario o contraseña incorrectos.");
      } else {
        setError("No se pudo conectar con el servidor. Intenta de nuevo.");
      }
    } finally {
      setCargando(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") iniciarSesion();
  };

  return (
    <div className="login-page">
      <div className="login-split" onKeyDown={handleKeyDown}>
        <div className="login-panel-left">
          <div className="login-panel-overlay">
            <p className="login-side-tagline">
              Sistema de Control de Asistencia
            </p>
          </div>
        </div>

        <div className="login-panel-right">
          <div className="login-form-container">
            <div className="login-brand">
              <img
                src="/img/logo_asansa.jpeg"
                alt="Logo institucional"
                className="login-logo"
                onError={(e) => (e.currentTarget.style.display = "none")}
              />
              <h1>
                C.E.N. Lcda. Roraima
                <br />
                Asansa Escándela
              </h1>
              <p>Ingresa tus datos para continuar</p>
            </div>

            {error && (
              <p className="error-msg" role="alert" aria-live="assertive">
                {error}
              </p>
            )}

            <div className="login-field">
              <label htmlFor="usuario">Usuario</label>
              <input
                type="text"
                id="usuario"
                name="usuario"
                placeholder="Tu nombre de usuario"
                required
                autoFocus
                value={usuario}
                onChange={(e) => setUsuario(e.target.value)}
              />
            </div>

            <div className="login-field">
              <label htmlFor="password">Contraseña</label>
              <input
                type="password"
                id="password"
                name="password"
                placeholder="Tu contraseña"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <button
              type="button"
              className="login-btn"
              onClick={iniciarSesion}
              aria-busy={cargando}
            >
              {cargando ? "Ingresando..." : "Iniciar sesión"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
