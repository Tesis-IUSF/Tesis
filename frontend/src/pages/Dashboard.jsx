// src/pages/Dashboard.jsx
import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../utils/api";
import "./Dashboard.css";

// Fecha local en formato YYYY-MM-DD (toISOString usaría UTC y adelantaría el día de noche)
const fmt = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

// De lunes a HOY: los días que aún no llegan no cuentan como ausencias.
function getSemanaActual() {
  const hoy = new Date();
  const diaSemana = hoy.getDay() || 7;
  const lunes = new Date(hoy);
  lunes.setDate(hoy.getDate() - (diaSemana - 1));
  return { desde: fmt(lunes), hasta: fmt(hoy) };
}

// Total de una respuesta: lista paginada (totalElements) o arreglo simple.
function contar(resultado) {
  if (resultado.status !== "fulfilled") return "—";
  const data = resultado.value.data;
  return data?.totalElements ?? data?.length ?? "—";
}

// Si no tiene "ruta" se muestra como "Próximamente".
const MODULOS = [
  {
    ruta: "/empleados",
    titulo: "Empleados",
    texto: "Ver y administrar empleados del plantel.",
    enlace: "Ir a Empleados",
  },
  {
    ruta: "/asistencia/historico",
    titulo: "Asistencia",
    texto: "Registrar y consultar asistencias del personal.",
    enlace: "Ir a Asistencia",
  },
  {
    ruta: "/turnos",
    titulo: "Turnos",
    texto: "Crear y editar los horarios de trabajo.",
    enlace: "Ir a Turnos",
  },
  {
    ruta: "/blog/comunicados",
    titulo: "Blog",
    texto: "Publicar comunicados y avisos para la comunidad.",
    enlace: "Ir a Comunicados",
  },
];

function Dashboard() {
  const [nombre] = useState(() => localStorage.getItem("nombre") || "");

  const [totales, setTotales] = useState({
    personal: "—",
    asistencias: "—",
  });

  const [semana, setSemana] = useState({
    presentes: "—",
    ausentes: "—",
    tardanzas: "—",
    permisos: "—",
  });

  const { desde, hasta } = getSemanaActual();

  useEffect(() => {
    let activo = true;

    // Cada petición es independiente: si una falla, solo su tarjeta muestra "—".
    Promise.allSettled([
      api.get("/empleados", { params: { page: 0, size: 1 } }),
      api.get("/asistencias/hoy"),
    ]).then(([empleados, hoy]) => {
      if (!activo) return;
      setTotales({
        personal: contar(empleados),
        asistencias: contar(hoy),
      });
    });

    Promise.allSettled([
      api.get("/asistencias/historico", { params: { desde, hasta } }),
      api.get("/asistencias/ausencias", { params: { desde, hasta } }),
    ]).then(([historico, ausencias]) => {
      if (!activo) return;
      const registros =
        historico.status === "fulfilled" ? historico.value.data : null;
      const cuenta = (...estados) =>
        registros
          ? registros.filter((r) => estados.includes(r.estado)).length
          : "—";

      setSemana({
        presentes: cuenta("presente", "tardanza", "salida_anticipada"),
        ausentes:
          ausencias.status === "fulfilled" ? ausencias.value.data.length : "—",
        tardanzas: cuenta("tardanza"),
        permisos: cuenta("permiso"),
      });
    });

    return () => {
      activo = false;
    };
  }, [desde, hasta]);

  return (
    <>
      <main className="dash-main">
        <div className="dash-heading">
          <h1>Panel principal</h1>
          <p id="bienvenida-texto">
            {nombre
              ? `¡Hola, ${nombre}! Aquí el resumen de hoy.`
              : "Aquí el resumen de hoy."}
          </p>
        </div>

        <div className="stats-row stats-row--resumen">
          <div className="stat-card stat-card--azul">
            <div className="stat-value">{totales.personal}</div>
            <div className="stat-label">Personal registrado</div>
          </div>
          <div className="stat-card stat-card--verde">
            <div className="stat-value">{totales.asistencias}</div>
            <div className="stat-label">Registros de hoy</div>
          </div>
        </div>

        <div className="semana-heading">
          <p className="modules-title">Asistencia esta semana</p>
          <Link to="/asistencia/historico" className="link-detalle-semana">
            Ver a detalle
          </Link>
        </div>

        <div className="stats-row stats-row--semana">
          <div className="stat-card stat-card--verde">
            <div className="stat-value">{semana.presentes}</div>
            <div className="stat-label">Presentes</div>
          </div>
          <div className="stat-card stat-card--rojo">
            <div className="stat-value">{semana.ausentes}</div>
            <div className="stat-label">Ausentes</div>
          </div>
          <div className="stat-card stat-card--naranja">
            <div className="stat-value">{semana.tardanzas}</div>
            <div className="stat-label">Tardanzas</div>
          </div>
          <div className="stat-card stat-card--azul">
            <div className="stat-value">{semana.permisos}</div>
            <div className="stat-label">Permisos</div>
          </div>
        </div>

        <p className="modules-title">Accesos rápidos</p>
        <div className="modules-grid">
          {MODULOS.map((m) =>
            m.ruta ? (
              <Link key={m.titulo} className="module-card" to={m.ruta}>
                <h3>{m.titulo}</h3>
                <p>{m.texto}</p>
                <span className="module-link">{m.enlace}</span>
              </Link>
            ) : (
              <div
                key={m.titulo}
                className="module-card module-card--pronto"
                aria-disabled="true"
              >
                <h3>{m.titulo}</h3>
                <p>{m.texto}</p>
                <span className="module-pronto">Próximamente</span>
              </div>
            ),
          )}
        </div>
      </main>
    </>
  );
}

export default Dashboard;
