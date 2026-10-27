import AnimatedSelect from "../components/AnimatedSelect";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import "./EmpleadoForm.css";

const VACIO = {
  nombreUsuario: "",
  email: "",
  password: "",
  rolId: "",
  tipoUsuario: "PERSONAL",
  activo: true,
};

const TIPOS_USUARIO = [
  ["PERSONAL", "Personal"],
  ["ESTUDIANTE", "Estudiante"],
  ["REPRESENTANTE", "Representante"],
  ["VISITANTE", "Visitante"],
];

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MAX_PASSWORD_BYTES = 72;
const PASSWORDS_COMUNES = new Set([
  "123456",
  "12345678",
  "123456789",
  "1234567890",
  "123456789012",
  "password",
  "password1",
  "password123",
  "qwerty",
  "qwerty123",
  "admin",
  "admin123",
  "letmein",
  "welcome",
  "iloveyou",
  "monkey",
  "dragon",
  "sunshine",
  "princess",
  "football",
  "baseball",
  "secret",
  "contrasena",
  "contraseña",
  "clave123",
  "tequiero",
]);

function UsuarioForm() {
  const { id } = useParams();
  const esEdicion = Boolean(id);
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState(VACIO);
  const [roles, setRoles] = useState([]);
  const [errorRoles, setErrorRoles] = useState("");
  const [cargando, setCargando] = useState(esEdicion);
  const [cargandoRoles, setCargandoRoles] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    let activo = true;

    api
      .get("/catalogos/roles", { params: { page: 0, size: 100 } })
      .then(({ data }) => {
        if (activo) setRoles(data.content);
      })
      .catch((err) => {
        if (activo) {
          const mensaje = extractErrorMessage(
            err,
            "No se pudieron cargar los roles.",
          );
          setErrorRoles(mensaje);
          toast.error(mensaje);
        }
      })
      .finally(() => {
        if (activo) setCargandoRoles(false);
      });

    if (esEdicion) {
      api
        .get(`/usuarios/${id}`)
        .then(({ data }) => {
          if (!activo) return;
          setForm({
            nombreUsuario: data.nombreUsuario || "",
            email: data.email || "",
            password: "",
            rolId: data.rolId ?? "",
            tipoUsuario: data.tipoUsuario || "PERSONAL",
            activo: data.activo !== false,
          });
        })
        .catch((err) => {
          if (activo) {
            toast.error(
              extractErrorMessage(err, "No se pudo cargar el usuario."),
            );
            navigate("/usuarios");
          }
        })
        .finally(() => {
          if (activo) setCargando(false);
        });
    }

    return () => {
      activo = false;
    };
  }, [esEdicion, id, navigate, toast]);

  const actualizarCampo = (campo, valor) => {
    setForm((prev) => ({ ...prev, [campo]: valor }));
    setErrores((prev) => ({ ...prev, [campo]: undefined }));
  };

  const validar = () => {
    const nuevosErrores = {};
    if (!form.nombreUsuario.trim()) {
      nuevosErrores.nombreUsuario = "El nombre de usuario es obligatorio.";
    }
    if (!form.email.trim()) {
      nuevosErrores.email = "El email es obligatorio.";
    } else if (form.email.trim().length > 150) {
      nuevosErrores.email = "El email no puede superar 150 caracteres.";
    } else if (!EMAIL.test(form.email.trim())) {
      nuevosErrores.email = "Ingresa un email válido.";
    }
    if (!esEdicion && !form.password.trim()) {
      nuevosErrores.password = "La contraseña es obligatoria.";
    }
    if (form.password.trim()) {
      if (Array.from(form.password).length < 12) {
        nuevosErrores.password =
          "La contraseña debe tener al menos 12 caracteres.";
      } else if (
        new TextEncoder().encode(form.password).length > MAX_PASSWORD_BYTES
      ) {
        nuevosErrores.password =
          "La contraseña no puede superar 72 bytes UTF-8.";
      } else if (/\s/u.test(form.password)) {
        nuevosErrores.password = "La contraseña no puede contener espacios.";
      } else if (!/\p{Lu}/u.test(form.password)) {
        nuevosErrores.password =
          "La contraseña debe incluir al menos una mayúscula.";
      } else if (!/\p{Nd}/u.test(form.password)) {
        nuevosErrores.password =
          "La contraseña debe incluir al menos un número.";
      } else if (!/[^\p{L}\p{N}\s]/u.test(form.password)) {
        nuevosErrores.password =
          "La contraseña debe incluir al menos un símbolo.";
      } else {
        const passwordNormalizada = form.password.toLowerCase();
        const baseComun = passwordNormalizada.replace(/[^\p{L}]+$/u, "");
        const usuarioNormalizado = form.nombreUsuario.trim().toLowerCase();
        const emailNormalizado = form.email.trim().toLowerCase();
        const usuarioEmail = emailNormalizado.split("@", 1)[0];
        const contieneIdentificador = [
          usuarioNormalizado,
          emailNormalizado,
          usuarioEmail,
        ].some(
          (identificador) =>
            identificador.length >= 3 &&
            passwordNormalizada.includes(identificador),
        );
        if (
          PASSWORDS_COMUNES.has(passwordNormalizada) ||
          PASSWORDS_COMUNES.has(baseComun)
        ) {
          nuevosErrores.password = "Elige una contraseña menos común.";
        } else if (contieneIdentificador) {
          nuevosErrores.password =
            "La contraseña no puede incluir el nombre de usuario ni el correo.";
        }
      }
    }
    if (!form.rolId) {
      nuevosErrores.rolId = "Selecciona un rol.";
    }
    setErrores(nuevosErrores);
    return Object.keys(nuevosErrores).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validar()) return;

    setGuardando(true);
    const payload = {
      nombreUsuario: form.nombreUsuario.trim(),
      email: form.email.trim(),
      password: form.password || null,
      rolId: Number(form.rolId),
      tipoUsuario: form.tipoUsuario,
      activo: form.activo,
    };

    try {
      if (esEdicion) {
        await api.put(`/usuarios/${id}`, payload);
        toast.success("Usuario actualizado correctamente.");
      } else {
        await api.post("/usuarios", payload);
        toast.success("Usuario creado correctamente.");
      }
      navigate("/usuarios");
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo guardar el usuario."));
    } finally {
      setGuardando(false);
    }
  };

  if (cargando) {
    return (
      <div className="mod-main">
        <p>Cargando usuario...</p>
      </div>
    );
  }

  const rolesDisponibles = roles.filter(
    (rol) => rol.activo !== false || String(rol.id) === String(form.rolId),
  );

  return (
    <div className="mod-main">
      <div className="mod-heading">
        <h1>{esEdicion ? "Editar Usuario" : "Crear Usuario"}</h1>
      </div>

      <form className="empleado-form" onSubmit={handleSubmit}>
        <div className="form-grid-2">
          <label>
            Nombre de usuario *
            <input
              maxLength={100}
              value={form.nombreUsuario}
              onChange={(e) =>
                actualizarCampo("nombreUsuario", e.target.value)
              }
              autoComplete="username"
            />
            {errores.nombreUsuario && (
              <span className="campo-error">{errores.nombreUsuario}</span>
            )}
          </label>

          <label>
            Email *
            <input
              type="email"
              maxLength={150}
              required
              value={form.email}
              onChange={(e) => actualizarCampo("email", e.target.value)}
              autoComplete="email"
            />
            {errores.email && (
              <span className="campo-error">{errores.email}</span>
            )}
          </label>

          <label>
            {esEdicion ? "Nueva contraseña" : "Contraseña *"}
            <input
              type="password"
              value={form.password}
              onChange={(e) => actualizarCampo("password", e.target.value)}
              autoComplete="new-password"
              required={!esEdicion}
              maxLength={72}
              aria-describedby="ayuda-password"
            />
            <span className="campo-ayuda" id="ayuda-password">
              {esEdicion
                ? "Déjala vacía para conservar la contraseña actual. Usa al menos 12 caracteres, sin espacios; incluye una mayúscula, un número y un símbolo. Límite: 72 bytes UTF-8. Evita contraseñas comunes y datos de usuario o correo."
                : "Usa al menos 12 caracteres, sin espacios; incluye una mayúscula, un número y un símbolo. Límite: 72 bytes UTF-8. Evita contraseñas comunes y datos de usuario o correo."}
            </span>
            {errores.password && (
              <span className="campo-error">{errores.password}</span>
            )}
          </label>

          <label>
            Rol *
            <AnimatedSelect
              value={form.rolId}
              onChange={(e) => actualizarCampo("rolId", e.target.value)}
              disabled={cargandoRoles || rolesDisponibles.length === 0}
            >
              <option value="">
                {cargandoRoles ? "Cargando roles..." : "Selecciona un rol..."}
              </option>
              {rolesDisponibles.map((rol) => (
                <option key={rol.id} value={rol.id}>
                  {rol.nombreRol}
                </option>
              ))}
            </AnimatedSelect>
            {errores.rolId && (
              <span className="campo-error">{errores.rolId}</span>
            )}
            {errorRoles && <span className="campo-error">{errorRoles}</span>}
            {!cargandoRoles && !errorRoles && rolesDisponibles.length === 0 && (
              <span className="campo-error">No hay roles activos disponibles.</span>
            )}
          </label>

          <label>
            Tipo de usuario *
            <AnimatedSelect
              value={form.tipoUsuario}
              onChange={(e) => actualizarCampo("tipoUsuario", e.target.value)}
            >
              {TIPOS_USUARIO.map(([valor, etiqueta]) => (
                <option key={valor} value={valor}>
                  {etiqueta}
                </option>
              ))}
            </AnimatedSelect>
          </label>

          <label>
            Estado
            <AnimatedSelect
              value={form.activo ? "true" : "false"}
              onChange={(e) =>
                actualizarCampo("activo", e.target.value === "true")
              }
            >
              <option value="true">Activo</option>
              <option value="false">Inactivo</option>
            </AnimatedSelect>
          </label>
        </div>

        <div className="form-actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={() => navigate("/usuarios")}
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="btn-primary"
            disabled={
              guardando ||
              cargandoRoles ||
              Boolean(errorRoles) ||
              rolesDisponibles.length === 0
            }
          >
            {guardando
              ? "Guardando..."
              : esEdicion
                ? "Guardar cambios"
                : "Crear Usuario"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default UsuarioForm;
