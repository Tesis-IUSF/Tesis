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

export function normalizarRol(rol) {
  const normalized = String(rol || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_|_$/g, "");

  return normalized.startsWith("ROLE_") ? normalized : `ROLE_${normalized}`;
}

export function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("rol");
  localStorage.removeItem("nombre");
}
