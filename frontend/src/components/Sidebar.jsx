import { useState, useEffect, useRef } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { Icon } from "@iconify/react";
import { getNombre, getRol, logout } from "../utils/auth";
import { ROLES } from "../constants/roles";
import { ICONOS } from "../utils/iconos";
import "./Sidebar.css";

// Para agregar un módulo nuevo basta con añadir un ítem aquí.
// - ruta: a dónde lleva (sin ruta + pronto: true = deshabilitado)
// - roles: si se indica, solo esos roles lo ven
// - externo: true = se abre en una pestaña nueva (páginas públicas)
// - excluir: rutas que NO deben marcar este ítem como activo
const SECCIONES = [
  {
    titulo: null,
    items: [
      {
        ruta: "/dashboard",
        etiqueta: "Panel principal",
        icono: ICONOS.panel,
      },
    ],
  },
  {
    titulo: "Personal",
    items: [
      { ruta: "/empleados", etiqueta: "Empleados", icono: ICONOS.empleados },
      { ruta: "/turnos", etiqueta: "Turnos", icono: ICONOS.reloj },
      {
        ruta: "/carnets",
        etiqueta: "Carnets",
        icono: ICONOS.carnet,
        roles: [ROLES.ADMIN, ROLES.DIRECTOR],
      },
      {
        ruta: "/departamentos",
        etiqueta: "Departamentos",
        icono: ICONOS.departamentos,
      },
      { ruta: "/cargos", etiqueta: "Cargos", icono: ICONOS.cargos },
    ],
  },
  {
    titulo: "Asistencia",
    items: [
      {
        ruta: "/asistencia/hoy",
        etiqueta: "Asistencia de hoy",
        icono: ICONOS.asistenciaHoy,
      },
      {
        ruta: "/asistencia/historico",
        etiqueta: "Histórico",
        icono: ICONOS.historico,
      },
    ],
  },
  {
    titulo: "Institución",
    items: [
      {
        ruta: "/matricula",
        etiqueta: "Matrícula",
        icono: ICONOS.matricula,
        excluir: ["/matricula/requisitos"],
      },
      {
        ruta: "/matricula/requisitos",
        etiqueta: "Requisitos",
        icono: ICONOS.requisitos,
        roles: [ROLES.ADMIN, ROLES.DIRECTOR],
      },
    ],
  },
  {
    titulo: "Blog",
    items: [
      {
        ruta: "/blog/comunicados",
        etiqueta: "Comunicados",
        icono: ICONOS.comunicados,
      },
      {
        ruta: "/blog/categorias",
        etiqueta: "Categorías",
        icono: ICONOS.categorias,
      },
      {
        ruta: "/noticias",
        etiqueta: "Ver blog público",
        icono: ICONOS.blog,
        externo: true,
      },
    ],
  },
];

function Sidebar({ colapsado, abiertoMovil, onCerrar }) {
  const rol = getRol();
  const nombre = getNombre() || "Usuario";
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [menuUsuario, setMenuUsuario] = useState(false);
  const usuarioRef = useRef(null);

  // Cierra el menú del usuario al hacer clic fuera
  useEffect(() => {
    if (!menuUsuario) return undefined;
    const alClicFuera = (evento) => {
      if (usuarioRef.current && !usuarioRef.current.contains(evento.target)) {
        setMenuUsuario(false);
      }
    };
    document.addEventListener("mousedown", alClicFuera);
    return () => document.removeEventListener("mousedown", alClicFuera);
  }, [menuUsuario]);

  const cerrarSesion = () => {
    logout();
    navigate("/login");
  };

  const secciones = SECCIONES.map((seccion) => ({
    ...seccion,
    items: seccion.items.filter(
      (item) => !item.roles || item.roles.includes(rol),
    ),
  })).filter((seccion) => seccion.items.length > 0);

  return (
    <>
      {abiertoMovil && <div className="sidebar-fondo" onClick={onCerrar} />}

      <aside
        className={`sidebar${colapsado ? " sidebar--colapsado" : ""}${abiertoMovil ? " sidebar--abierto" : ""}`}
        aria-label="Menú principal"
      >
        <nav>
          {secciones.map((seccion, indice) => (
            <div
              className="sidebar-seccion"
              key={seccion.titulo ?? `s-${indice}`}
            >
              {seccion.titulo && (
                <p className="sidebar-titulo">{seccion.titulo}</p>
              )}

              {seccion.items.map((item) =>
                item.pronto ? (
                  <span
                    key={item.etiqueta}
                    className="sidebar-item sidebar-item--pronto"
                    aria-disabled="true"
                    title={`${item.etiqueta} (próximamente)`}
                  >
                    <Icon icon={item.icono} className="sidebar-icono" />
                    <span className="sidebar-etiqueta">{item.etiqueta}</span>
                    <span className="sidebar-pronto">Pronto</span>
                  </span>
                ) : item.externo ? (
                  <a
                    key={item.ruta}
                    href={item.ruta}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={onCerrar}
                    title={
                      colapsado
                        ? item.etiqueta
                        : `${item.etiqueta} (se abre en otra pestaña)`
                    }
                    className="sidebar-item"
                  >
                    <Icon icon={item.icono} className="sidebar-icono" />
                    <span className="sidebar-etiqueta">{item.etiqueta}</span>
                    <Icon icon={ICONOS.externo} className="sidebar-externo" />
                  </a>
                ) : (
                  <NavLink
                    key={item.ruta}
                    to={item.ruta}
                    onClick={onCerrar}
                    title={colapsado ? item.etiqueta : undefined}
                    className={({ isActive }) => {
                      const excluido = (item.excluir ?? []).some((p) =>
                        pathname.startsWith(p),
                      );
                      return `sidebar-item${isActive && !excluido ? " sidebar-item--activo" : ""}`;
                    }}
                  >
                    <Icon icon={item.icono} className="sidebar-icono" />
                    <span className="sidebar-etiqueta">{item.etiqueta}</span>
                  </NavLink>
                ),
              )}
            </div>
          ))}
        </nav>

        {/* Usuario al final del menú */}
        <div className="sidebar-usuario" ref={usuarioRef}>
          {menuUsuario && (
            <button
              type="button"
              className="sidebar-item sidebar-item--salir"
              onClick={cerrarSesion}
              title={colapsado ? "Cerrar sesión" : undefined}
            >
              <Icon icon={ICONOS.salir} className="sidebar-icono" />
              <span className="sidebar-etiqueta">Cerrar sesión</span>
            </button>
          )}
          <button
            type="button"
            className="sidebar-item sidebar-usuario-btn"
            onClick={() => setMenuUsuario((abierto) => !abierto)}
            aria-expanded={menuUsuario}
            title={colapsado ? nombre : undefined}
          >
            <Icon icon={ICONOS.usuario} className="sidebar-icono" />
            <span className="sidebar-etiqueta">{nombre}</span>
            <Icon
              icon={menuUsuario ? "mdi:chevron-down" : "mdi:chevron-up"}
              className="sidebar-chevron"
            />
          </button>
        </div>
      </aside>
    </>
  );
}

export default Sidebar;
