// src/pages/Dashboard.jsx
import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import Topbar from "../components/Topbar";
import "./Dashboard.css";

function getSemanaActual() {
  const hoy = new Date();
  const diaSemana = hoy.getDay() || 7;
  const lunes = new Date(hoy);
  lunes.setDate(hoy.getDate() - (diaSemana - 1));
  const domingo = new Date(lunes);
  domingo.setDate(lunes.getDate() + 6);
  const fmt = (d) => d.toISOString().split("T")[0];
  return { desde: fmt(lunes), hasta: fmt(domingo) };
}

function Dashboard() {
  const [nombre] = useState(() => localStorage.getItem("nombre") || "");

  const [totales, setTotales] = useState({
    personal: "—",
    asistencias: "—",
    deptos: "—",
  });

  const [semana, setSemana] = useState({
    presentes: "—",
    ausentes: "—",
    tardanzas: "—",
    permisos: "—",
  });

  const { desde, hasta } = getSemanaActual();

  const cargarEstadisticas = useCallback(async () => {
    const d = new Date();
    const año = d.getFullYear();
    const mes = String(d.getMonth() + 1).padStart(2, "0");
    const dia = String(d.getDate()).padStart(2, "0");
    const fechaLocal = `${año}-${mes}-${dia}`;

    try {
      // TODO: conectar con utils/api.js más adelante
      console.log("Cargando estadísticas simuladas para", fechaLocal);
    } catch (err) {
      console.error("Error crítico en bloque de estadísticas:", err);
      setTotales({ personal: "—", asistencias: "—", deptos: "—" });
    }
  }, []);

  const cargarEstadisticasSemana = useCallback(async () => {
    try {
      // TODO: conectar con utils/api.js más adelante
      console.log("Cargando estadísticas de semana", desde, hasta);
    } catch (err) {
      console.error("Error cargando estadisticas de semana:", err);
      setSemana({
        presentes: "—",
        ausentes: "—",
        tardanzas: "—",
        permisos: "—",
      });
    }
  }, [desde, hasta]);

  useEffect(() => {
    cargarEstadisticas();
    cargarEstadisticasSemana();
  }, [cargarEstadisticas, cargarEstadisticasSemana]);

  return (
    <>
      <Topbar />
      <main className="dash-main">
        <div className="dash-heading">
          <p className="eyebrow"></p>
          <h1>Panel principal</h1>
          <p id="bienvenida-texto">
            {nombre
              ? `¡Hola, ${nombre}! Aquí el resumen de hoy.`
              : "Aquí el resumen de hoy."}
          </p>
        </div>

        <div className="stats-row">
          <div className="stat-card stat-card--azul">
            <div className="stat-value">{totales.personal}</div>
            <div className="stat-label">Personal registrado</div>
          </div>
          <div className="stat-card stat-card--verde">
            <div className="stat-value">{totales.asistencias}</div>
            <div className="stat-label">Registros de hoy</div>
          </div>
          <div className="stat-card stat-card--dorado">
            <div className="stat-value">{totales.deptos}</div>
            <div className="stat-label">Departamentos</div>
          </div>
        </div>

        <div className="semana-heading">
          <p className="modules-title">Asistencia esta semana</p>
          <Link
            to={`/asistencia?fechaDesde=${desde}&fechaHasta=${hasta}`}
            className="link-detalle-semana"
          >
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
          <Link className="module-card" to="/empleados">
            <h3>Empleados</h3>
            <p>Ver y administrar empleados del plantel.</p>
            <span className="module-link">Ir a Empleados</span>
          </Link>

          <Link className="module-card" to="/departamentos">
            <h3>Departamentos</h3>
            <p>Administrar las áreas institucionales.</p>
            <span className="module-link">Ir a Departamentos</span>
          </Link>

          <Link className="module-card" to="/cargos">
            <h3>Cargos</h3>
            <p>Crear, editar y eliminar cargos vinculados a departamentos.</p>
            <span className="module-link">Ir a Cargos</span>
          </Link>

          <Link className="module-card" to="/asistencia/historico">
            <h3>Asistencia</h3>
            <p>Registrar y consultar asistencias del personal.</p>
            <span className="module-link">Ir a Asistencia</span>
          </Link>

          <Link className="module-card" to="/escaner">
            <h3>Escáner QR</h3>
            <p>Registrar entrada y salida escaneando el QR del empleado.</p>
            <span className="module-link">Ir al Escáner</span>
          </Link>
        </div>
      </main>
    </>
  );
}

export default Dashboard;
