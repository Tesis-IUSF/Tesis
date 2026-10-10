import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ROLES } from "../constants/roles";
import api from "../utils/api";
import { normalizarRol } from "../utils/auth";
import "./Login.css";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const navigate = useNavigate();

  const iniciarSesion = async () => {
    if (!email.trim() || !password) {
      setError("Por favor completa todos los campos.");
      return;
    }
    setCargando(true);
    setError("");

    try {
      const { data } = await api.post("/auth/login", {
        email: email.trim(),
        password,
      });

      if (!data.token) {
        throw new Error("La respuesta de autenticación no incluyó un token.");
      }

      const rol = normalizarRol(data.rolNombre);
      localStorage.setItem("token", data.token);
      localStorage.setItem("rol", rol);
      localStorage.setItem(
        "nombre",
        data.nombreUsuario || data.email || email.trim(),
      );

      if (rol === ROLES.ESCANER) {
        navigate("/escaner");
      } else {
        navigate("/dashboard");
      }
    } catch (err) {
      if ([401, 403, 404].includes(err.response?.status)) {
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
              <label htmlFor="email">Correo electrónico</label>
              <input
                type="email"
                id="email"
                name="email"
                placeholder="tu@correo.com"
                required
                autoFocus
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
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
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <button
              type="button"
              className="login-btn"
              onClick={iniciarSesion}
              aria-busy={cargando}
              disabled={cargando}
            >
              {cargando ? "Ingresando..." : "Iniciar sesión"}
            </button>
            <Link to="/noticias" className="login-enlace">
              Ver noticias del plantel
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
