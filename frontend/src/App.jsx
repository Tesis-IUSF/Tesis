import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ToastProvider } from "./context/ToastContext";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Empleados from "./pages/Empleados";
import ProtectedRoute from "./components/ProtectedRoute";
import NoAutorizado from "./pages/NoAutorizado";
import { ROLES } from "./constants/roles";
import Historico from "./pages/Historico";
import EmpleadoForm from "./pages/EmpleadoForm";
import EscanerQR from "./pages/EscanerQR";
import AsistenciaHoy from "./pages/AsistenciaHoy";
import Turnos from "./pages/Turnos";
import TurnoForm from "./pages/TurnoForm";
import Carnets from "./pages/Carnets";

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/no-autorizado" element={<NoAutorizado />} />

          {/* Solo Escáner */}
          <Route element={<ProtectedRoute allowedRoles={[ROLES.ESCANER]} />}>
            <Route path="/escaner" element={<EscanerQR />} />
          </Route>

          {/* Admin, Director, Administrativo */}
          <Route
            element={
              <ProtectedRoute
                allowedRoles={[
                  ROLES.ADMIN,
                  ROLES.DIRECTOR,
                  ROLES.ADMINISTRATIVO,
                ]}
              />
            }
          >
            <Route
              element={
                <ProtectedRoute allowedRoles={[ROLES.ADMIN, ROLES.DIRECTOR]} />
              }
            >
              <Route path="/carnets" element={<Carnets />} />
            </Route>

            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/empleados" element={<Empleados />} />
            <Route path="/empleados/nuevo" element={<EmpleadoForm />} />
            <Route path="/empleados/:id/editar" element={<EmpleadoForm />} />
            <Route path="/asistencia/hoy" element={<AsistenciaHoy />} />
            <Route path="/asistencia/historico" element={<Historico />} />
            <Route path="/turnos" element={<Turnos />} />
            <Route path="/turnos/nuevo" element={<TurnoForm />} />
            <Route path="/turnos/:id/editar" element={<TurnoForm />} />
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;
