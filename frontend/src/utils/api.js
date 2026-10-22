import axios from "axios";
import { logout } from "./auth";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token && !config.url?.endsWith("/auth/login")) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Sesión vencida: el backend responde 401 con un token guardado.
// El login se excluye porque allí un 401 significa credenciales incorrectas.
api.interceptors.response.use(
  (respuesta) => respuesta,
  (err) => {
    const esLogin = err.config?.url?.endsWith("/auth/login");
    const teniaSesion = !!localStorage.getItem("token");

    if (err.response?.status === 401 && teniaSesion && !esLogin) {
      logout();
      if (window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
    }
    return Promise.reject(err);
  },
);

export default api;
