import axios from "axios";

// Cliente para páginas públicas: no envía el token de sesión.
const apiPublica = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
});

export default apiPublica;
