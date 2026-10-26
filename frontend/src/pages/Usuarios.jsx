import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Icon } from "@iconify/react";
import ConfirmDialog from "../components/ConfirmDialog";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { ICONOS } from "../utils/iconos";
import api from "../utils/api";
import "./Empleados.css";

function Usuarios() {
  const toast = useToast();
  const [usuarios, setUsuarios] = useState([]);
  const [pagina, setPagina] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [usuarioAEliminar, setUsuarioAEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);

  const cargarUsuarios = useCallback(async (page = 0) => {
    setCargando(true);
    setError("");
    try {
      const { data } = await api.get("/usuarios", {
        params: { page, size: 25 },
      });
      setUsuarios(data.content);
      setPagina({
        page: data.page,
        totalPages: data.totalPages,
        totalElements: data.totalElements,
      });
    } catch (err) {
      setError(extractErrorMessage(err, "No se pudo cargar la lista de usuarios."));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    let activo = true;
    api
      .get("/usuarios", { params: { page: 0, size: 25 } })
      .then(({ data }) => {
        if (!activo) return;
        setUsuarios(data.content);
        setPagina({
          page: data.page,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
        });
      })
      .catch((err) => {
        if (activo) {
          setError(
            extractErrorMessage(err, "No se pudo cargar la lista de usuarios."),
          );
        }
      })
      .finally(() => {
        if (activo) setCargando(false);
      });

    return () => {
      activo = false;
    };
  }, []);

  const confirmarEliminar = async () => {
    if (!usuarioAEliminar) return;
    setEliminando(true);
    try {
      await api.delete(`/usuarios/${usuarioAEliminar.id}`);
      toast.success(`El usuario ${usuarioAEliminar.nombreUsuario} fue eliminado.`);
      setUsuarioAEliminar(null);
      const paginaActual =
        usuarios.length === 1 && pagina.page > 0 ? pagina.page - 1 : pagina.page;
      await cargarUsuarios(paginaActual);
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo eliminar el usuario."));
    } finally {
      setEliminando(false);
    }
  };

  return (
    <>
      <div className="mod-main">
        <div className="mod-heading">
          <h1>Usuarios</h1>
          <div className="mod-heading-actions">
            <Link to="/usuarios/nuevo" className="btn-primary">
              + Crear Usuario
            </Link>
          </div>
        </div>

        {error && <p className="error-msg">{error}</p>}

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nombre de usuario</th>
                <th>Email</th>
                <th>Rol</th>
                <th>Tipo</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={6} className="tabla-estado">
                    Cargando usuarios...
                  </td>
                </tr>
              ) : usuarios.length === 0 ? (
                <tr>
                  <td colSpan={6} className="tabla-estado">
                    <span className="tabla-estado-icono">
                      <Icon icon={ICONOS.lista} />
                    </span>
                    No hay usuarios registrados todavía.
                  </td>
                </tr>
              ) : (
                usuarios.map((usuario) => (
                  <tr key={usuario.id}>
                    <td data-label="Nombre de usuario">
                      {usuario.nombreUsuario}
                    </td>
                    <td data-label="Email">{usuario.email}</td>
                    <td data-label="Rol">{usuario.rolNombre || "—"}</td>
                    <td data-label="Tipo">{usuario.tipoUsuario || "—"}</td>
                    <td data-label="Estado">
                      <span
                        className={`badge-estado ${
                          usuario.activo ? "badge-activo" : "badge-inactivo"
                        }`}
                      >
                        {usuario.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td data-label="Acciones">
                      <Link
                        to={`/usuarios/${usuario.id}/editar`}
                        className="btn-edit"
                      >
                        Editar
                      </Link>
                      <button
                        type="button"
                        className="btn-delete"
                        onClick={() => setUsuarioAEliminar(usuario)}
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {pagina.totalPages > 1 && (
          <div className="paginacion">
            <button
              disabled={pagina.page === 0 || cargando}
              onClick={() => cargarUsuarios(pagina.page - 1)}
            >
              ← Anterior
            </button>
            <span>
              Página {pagina.page + 1} de {pagina.totalPages} (
              {pagina.totalElements} usuarios)
            </span>
            <button
              disabled={pagina.page >= pagina.totalPages - 1 || cargando}
              onClick={() => cargarUsuarios(pagina.page + 1)}
            >
              Siguiente →
            </button>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={!!usuarioAEliminar}
        title="Eliminar usuario"
        message={`¿Seguro que deseas eliminar el usuario "${usuarioAEliminar?.nombreUsuario}"? Esta acción no se puede deshacer.`}
        onConfirm={confirmarEliminar}
        onCancel={() => setUsuarioAEliminar(null)}
        cargando={eliminando}
      />
    </>
  );
}

export default Usuarios;
