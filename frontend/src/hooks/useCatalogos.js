import { useState, useEffect } from "react";
import api from "../utils/api";

export function useCatalogo(endpoint) {
  const [items, setItems] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let activo = true;
    setCargando(true);
    api
      .get(endpoint, { params: { size: 100 } })
      .then(({ data }) => {
        if (activo) setItems(data.content);
      })
      .catch(() => {
        if (activo) setError(`No se pudo cargar ${endpoint}.`);
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [endpoint]);

  return { items, cargando, error };
}

export const useCargos = () => useCatalogo("/catalogos/cargos");
export const useDepartamentos = () => useCatalogo("/catalogos/departamentos");
export const useTurnos = () => useCatalogo("/catalogos/turnos");
