// src/pages/AsistenciaHoy.jsx
import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import Topbar from "../components/Topbar";
import "./AsistenciaHoy.css";

const INTERVALO_AUTO_REFRESH = 30000; // 30 segundos, según criterio #6

function formatHora(hora) {
  return hora ? hora.substring(0, 5) : "—";
}

function RelojDigital() {
  const [hora, setHora] = useState(new Date());

  useEffect(() => {
    const id = setInterval(() => setHora(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="reloj-digital">
      {hora.toLocaleTimeString("es-VE", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      })}
    </div>
  );
}

function AsistenciaHoy() {
  const [registros, setRegistros] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const cargar = useCallback(async () => {
    try {
      const { data } = await api.get("/asistencias/hoy");
      setRegistros(data);
      setError("");
    } catch (err) {
      setError(
        extractErrorMessage(err, "No se pudo cargar la asistencia de hoy."),
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
    const id = setInterval(cargar, INTERVALO_AUTO_REFRESH);
    return () => clearInterval(id);
  }, [cargar]);

  return (
    <>
      <Topbar mostrarVolver />
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Asistencia de Hoy</h1>
          <RelojDigital />
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Hora Entrada</th>
                <th>Hora Salida</th>
                <th>Horas Trabajadas</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={6} className="tabla-estado">
                    Cargando asistencia de hoy...
                  </td>
                </tr>
              ) : registros.length === 0 ? (
                <tr>
                  <td colSpan={6} className="tabla-estado">
                    <span className="tabla-estado-icono">📋</span>
                    Nadie ha marcado entrada todavía hoy.
                  </td>
                </tr>
              ) : (
                registros.map((r) => (
                  <tr key={r.asistenciaId}>
                    <td data-label="Nombre">
                      {r.nombre} {r.apellido}
                    </td>
                    <td data-label="Hora Entrada">
                      {formatHora(r.horaEntrada)}
                    </td>
                    <td data-label="Hora Salida">{formatHora(r.horaSalida)}</td>
                    <td data-label="Horas Trabajadas">
                      {r.horasTrabajadas || "—"}
                    </td>
                    <td data-label="Estado">
                      <span className={`badge badge-${r.estado}`}>
                        {r.estado}
                      </span>
                    </td>
                    <td data-label="Acciones">
                      {!r.horaSalida && (
                        <Link to="/escaner" className="btn-marcar-salida">
                          Marcar Salida
                        </Link>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Nota: el botón "Marcar Salida" redirige al escáner porque hoy
            no existe un endpoint para registrar la salida manualmente sin
            volver a escanear el QR. Pendiente de confirmar con backend. */}
      </div>
    </>
  );
}

export default AsistenciaHoy;
