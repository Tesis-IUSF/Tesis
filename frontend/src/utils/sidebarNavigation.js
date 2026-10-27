import { getRol } from "./auth";
import { ROLES } from "../constants/roles";
import { ICONOS } from "./iconos";

// Para agregar un módulo nuevo basta con añadir un ítem aquí.
// - ruta: a dónde lleva (sin ruta + pronto: true = deshabilitado)
// - roles: si se indica, solo esos roles lo ven
// - externo: true = se abre en una pestaña nueva (páginas públicas)
// - excluir: rutas que NO deben marcar este ítem como activo
const SECCIONES = [
  {
    titulo: null,
    roles: [ROLES.DIRECTOR],
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
      {
        ruta: "/empleados",
        etiqueta: "Empleados",
        icono: ICONOS.empleados,
        roles: [ROLES.DIRECTOR, ROLES.COORDINADOR],
      },
      {
        ruta: "/usuarios",
        etiqueta: "Usuarios",
        icono: ICONOS.usuarios,
        roles: [ROLES.ADMIN],
      },
      {
        ruta: "/turnos",
        etiqueta: "Turnos",
        icono: ICONOS.reloj,
        roles: [ROLES.DIRECTOR],
      },
      {
        ruta: "/carnets",
        etiqueta: "Carnets",
        icono: ICONOS.carnet,
        roles: [ROLES.COORDINADOR],
      },
      {
        ruta: "/departamentos",
        etiqueta: "Departamentos",
        icono: ICONOS.departamentos,
        roles: [ROLES.ADMIN],
      },
      {
        ruta: "/cargos",
        etiqueta: "Cargos",
        icono: ICONOS.cargos,
        roles: [ROLES.ADMIN],
      },
    ],
  },
  {
    titulo: "Asistencia",
    roles: [ROLES.DIRECTOR],
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
    roles: [
      ROLES.ADMIN,
      ROLES.DIRECTOR,
      ROLES.COORDINADOR,
      ROLES.ADMINISTRATIVO,
    ],
    items: [
      {
        ruta: "/matricula",
        etiqueta: "Matrícula",
        icono: ICONOS.matricula,
        roles: [ROLES.DIRECTOR, ROLES.COORDINADOR, ROLES.ADMINISTRATIVO],
        excluir: ["/matricula/requisitos", "/matricula/calendario"],
      },
      {
        ruta: "/matricula/calendario",
        etiqueta: "Calendario de matrícula",
        icono: ICONOS.historico,
        roles: [ROLES.ADMIN],
      },
      {
        ruta: "/grados",
        etiqueta: "Grados",
        icono: ICONOS.matricula,
        roles: [ROLES.ADMIN],
      },
      {
        ruta: "/secciones",
        etiqueta: "Secciones",
        icono: ICONOS.seccion,
        roles: [ROLES.ADMIN],
      },
      {
        ruta: "/matricula/requisitos",
        etiqueta: "Requisitos",
        icono: ICONOS.requisitos,
        roles: [ROLES.ADMIN],
      },
    ],
  },
  {
    titulo: "Blog",
    roles: [ROLES.DIRECTOR, ROLES.COORDINADOR],
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

export function getVisibleSidebarSections(rol) {
  return SECCIONES.map((seccion) => ({
    ...seccion,
    items: seccion.items.filter(
      (item) => !item.roles || item.roles.includes(rol),
    ),
  })).filter(
    (seccion) =>
      seccion.items.length > 0 &&
      (!seccion.roles || seccion.roles.includes(rol)),
  );
}

export function getFirstSidebarRoute(rol = getRol()) {
  return (
    getVisibleSidebarSections(rol)
      .flatMap((seccion) => seccion.items)
      .find((item) => !item.externo && !item.pronto)?.ruta ?? "/no-autorizado"
  );
}
