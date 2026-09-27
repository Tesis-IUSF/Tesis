export function getToken() {
  return localStorage.getItem("token");
}

export function getRol() {
  return localStorage.getItem("rol");
}

export function getNombre() {
  return localStorage.getItem("nombre");
}

export function isAuthenticated() {
  return !!getToken();
}

export function hasRole(allowedRoles) {
  const rol = getRol();
  if (!allowedRoles || allowedRoles.length === 0) return true; // sin restricción de rol
  return allowedRoles.includes(rol);
}

export function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("rol");
  localStorage.removeItem("nombre");
}
