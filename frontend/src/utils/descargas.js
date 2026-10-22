// src/utils/descargas.js
import { extractErrorMessage } from "./errors";

export function descargarArchivo(blob, nombre) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}

// Con responseType "blob" los errores también llegan como Blob: hay que leerlos.
export async function mensajeErrorBlob(err, porDefecto) {
  const data = err?.response?.data;
  if (data instanceof Blob) {
    try {
      const json = JSON.parse(await data.text());
      return json.message || json.error || porDefecto;
    } catch {
      return porDefecto;
    }
  }
  return extractErrorMessage(err, porDefecto);
}
