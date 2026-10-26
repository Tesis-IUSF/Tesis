import { BrowserRouter, Routes, Route } from "react-router-dom";
import { ToastProvider } from "./context/ToastContext";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Empleados from "./pages/Empleados";
import Usuarios from "./pages/Usuarios";
import UsuarioForm from "./pages/UsuarioForm";
import ProtectedRoute from "./components/ProtectedRoute";
import Layout from "./components/Layout";
import PublicLayout from "./components/PublicLayout";
import NoAutorizado from "./pages/NoAutorizado";
import { ROLES } from "./constants/roles";
import Historico from "./pages/Historico";
import EmpleadoForm from "./pages/EmpleadoForm";
import EscanerQR from "./pages/EscanerQR";
import AsistenciaHoy from "./pages/AsistenciaHoy";
import Turnos from "./pages/Turnos";
import TurnoForm from "./pages/TurnoForm";
import Carnets from "./pages/Carnets";
import Noticias from "./pages/Noticias";
import NoticiaDetalle from "./pages/NoticiaDetalle";
import Inicio from "./pages/Inicio";
import BlogCategorias from "./pages/BlogCategorias";
import BlogComunicados from "./pages/BlogComunicados";
import BlogComunicadoForm from "./pages/BlogComunicadoForm";
import Departamentos from "./pages/Departamentos";
import Cargos from "./pages/Cargos";
import Preinscripcion from "./pages/Preinscripcion";
import Matricula from "./pages/Matricula";
import Inscripcion from "./pages/Inscripcion";
import EmpleadoDetalle from "./pages/EmpleadoDetalle";
import { getRol } from "./utils/auth";

function EmpleadosPorRol() {
  return <Empleados soloConsulta={getRol() === ROLES.DIRECTOR} />;
}

function MatriculaPorRol() {
  const rol = getRol();
  return (
    <Matricula
      soloConsulta={rol === ROLES.DIRECTOR}
      permitirRetiros={rol === ROLES.COORDINADOR}
    />
  );
}

function InscripcionPorRol() {
  return <Inscripcion soloConsulta={getRol() === ROLES.DIRECTOR} />;
}

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/no-autorizado" element={<NoAutorizado />} />

          {/* Páginas públicas (sin sesión) */}
          <Route element={<PublicLayout />}>
            <Route path="/" element={<Inicio />} />
            <Route path="/noticias" element={<Noticias />} />
            <Route path="/noticias/:id" element={<NoticiaDetalle />} />
          </Route>

          {/* Solo Escáner (sin menú lateral) */}
          <Route element={<ProtectedRoute allowedRoles={[ROLES.ESCANER]} />}>
            <Route path="/escaner" element={<EscanerQR />} />
          </Route>

          {/* Perfiles del panel (con menú lateral) */}
          <Route
            element={
              <ProtectedRoute
                allowedRoles={[
                  ROLES.ADMIN,
                  ROLES.DIRECTOR,
                  ROLES.COORDINADOR,
                  ROLES.ADMINISTRATIVO,
                ]}
              />
            }
          >
            <Route element={<Layout />}>
              <Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
                <Route path="/usuarios" element={<Usuarios />} />
                <Route path="/usuarios/nuevo" element={<UsuarioForm />} />
                <Route path="/usuarios/:id/editar" element={<UsuarioForm />} />
              </Route>

              <Route
                element={<ProtectedRoute allowedRoles={[ROLES.COORDINADOR]} />}
              >
                <Route path="/empleados/nuevo" element={<EmpleadoForm />} />
                <Route
                  path="/empleados/:id/editar"
                  element={<EmpleadoForm />}
                />
                <Route path="/carnets" element={<Carnets />} />
              </Route>

              <Route
                element={
                  <ProtectedRoute
                    allowedRoles={[ROLES.DIRECTOR, ROLES.COORDINADOR]}
                  />
                }
              >
                <Route path="/empleados" element={<EmpleadosPorRol />} />
                <Route path="/empleados/:id" element={<EmpleadoDetalle />} />
              </Route>

              <Route
                element={<ProtectedRoute allowedRoles={[ROLES.DIRECTOR]} />}
              >
                <Route path="/dashboard" element={<Dashboard />} />
                <Route
                  path="/asistencia/hoy"
                  element={<AsistenciaHoy soloConsulta />}
                />
                <Route path="/asistencia/historico" element={<Historico />} />
                <Route path="/turnos" element={<Turnos />} />
                <Route path="/turnos/nuevo" element={<TurnoForm />} />
                <Route path="/turnos/:id/editar" element={<TurnoForm />} />
                <Route path="/departamentos" element={<Departamentos />} />
                <Route path="/cargos" element={<Cargos />} />
              </Route>

              <Route
                element={
                  <ProtectedRoute
                    allowedRoles={[ROLES.DIRECTOR, ROLES.COORDINADOR]}
                  />
                }
              >
                <Route path="/blog/categorias" element={<BlogCategorias />} />
                <Route path="/blog/comunicados" element={<BlogComunicados />} />
                <Route
                  path="/blog/comunicados/nuevo"
                  element={<BlogComunicadoForm />}
                />
                <Route
                  path="/blog/comunicados/:id/editar"
                  element={<BlogComunicadoForm />}
                />
              </Route>

              <Route
                element={
                  <ProtectedRoute
                    allowedRoles={[
                      ROLES.DIRECTOR,
                      ROLES.COORDINADOR,
                      ROLES.ADMINISTRATIVO,
                      ROLES.ADMIN,
                    ]}
                  />
                }
              >
                <Route path="/matricula" element={<MatriculaPorRol />} />
                <Route
                  path="/matricula/:id/inscripcion"
                  element={<InscripcionPorRol />}
                />
              </Route>

              <Route
                element={
                  <ProtectedRoute
                    allowedRoles={[
                      ROLES.ADMIN,
                      ROLES.COORDINADOR,
                      ROLES.ADMINISTRATIVO,
                    ]}
                  />
                }
              >
                <Route
                  path="/matricula/preinscripcion"
                  element={<Preinscripcion />}
                />
                <Route
                  path="/matricula/:id/preinscripcion/editar"
                  element={<Preinscripcion />}
                />
              </Route>
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;
