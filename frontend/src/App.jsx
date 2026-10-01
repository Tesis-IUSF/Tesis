import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import ProtectedRoute from "./components/ProtectedRoute";
import NoAutorizado from "./pages/NoAutorizado";
import { ROLES } from "./constants/roles";
import Dashboard from "./pages/Dashboard";
import Empleados from "./pages/Empleados";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/no-autorizado" element={<NoAutorizado />} />

        {/* Rutas para ADMIN, DIRECTOR, ADMINISTRATIVO */}
        <Route
          element={
            <ProtectedRoute
              allowedRoles={[ROLES.ADMIN, ROLES.DIRECTOR, ROLES.ADMINISTRATIVO]}
            />
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/empleados" element={<Empleados />} />
        </Route>

        {/* Ruta exclusiva para ESCANER */}
        <Route element={<ProtectedRoute allowedRoles={[ROLES.ESCANER]} />}>
          <Route path="/escaner" element={<div>Escáner QR (pendiente)</div>} />
        </Route>

        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

// import { BrowserRouter } from "react-router-dom";
// import Login from "./pages/Login";

// function App() {
//   return (
//     <BrowserRouter>
//       <Login />
//     </BrowserRouter>
//   );
// }

// export default App;
