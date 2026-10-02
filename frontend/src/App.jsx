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

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/no-autorizado" element={<NoAutorizado />} />
          <Route path="/asistencia/historico" element={<Historico />} />
          <Route path="/empleados/nuevo" element={<EmpleadoForm />} />
          <Route path="/empleados/:id/editar" element={<EmpleadoForm />} />

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
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/empleados" element={<Empleados />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={[ROLES.ESCANER]} />}>
            <Route
              path="/escaner"
              element={<div>Escáner QR (pendiente)</div>}
            />
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;
