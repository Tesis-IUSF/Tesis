import { Navigate, Outlet } from "react-router-dom";
import { isAuthenticated, hasRole } from "../utils/auth";

function ProtectedRoute({ allowedRoles }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (!hasRole(allowedRoles)) {
    return <Navigate to="/no-autorizado" replace />;
  }

  return <Outlet />;
}

export default ProtectedRoute;
