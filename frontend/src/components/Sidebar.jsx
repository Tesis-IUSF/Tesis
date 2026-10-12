import { NavLink } from "react-router-dom";
import { Icon } from "@iconify/react";
import { getRol } from "../utils/auth";
import { ROLES } from "../constants/roles";
import { ICONOS } from "../utils/iconos";
import "./Sidebar.css";

// Para agregar un módulo nuevo basta con añadir un ítem aquí.
// - ruta: a dónde lleva (sin ruta + pronto: true = deshabilitado)
// - roles: si se indica, solo esos roles lo ven
// - externo: true = se abre en una pestaña nueva (páginas públicas)
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
      { etiqueta: "Departamentos", icono: ICONOS.departamentos, pronto: true },
      { etiqueta: "Cargos", icono: ICONOS.cargos, pronto: true },
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
      { etiqueta: "Matrícula", icono: ICONOS.matricula, pronto: true },
      {
        ruta: "/noticias",
        etiqueta: "Blog",
        icono: ICONOS.blog,
        externo: true,
      },
      { etiqueta: "Reportes", icono: ICONOS.reportes, pronto: true },
    ],
  },
];

function Sidebar({ colapsado, abiertoMovil, onCerrar }) {
  const rol = getRol();

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
                    className={({ isActive }) =>
                      `sidebar-item${isActive ? " sidebar-item--activo" : ""}`
                    }
                  >
                    <Icon icon={item.icono} className="sidebar-icono" />
                    <span className="sidebar-etiqueta">{item.etiqueta}</span>
                  </NavLink>
                ),
              )}
            </div>
          ))}
        </nav>
      </aside>
    </>
  );
}

export default Sidebar;
