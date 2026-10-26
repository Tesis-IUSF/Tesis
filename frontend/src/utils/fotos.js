// src/utils/fotos.js
import api from "./api";

// Sube una imagen a POST /fotos y devuelve la URL guardada ("/api/fotos/nombre")
export async function subirFoto(archivo) {
  const datos = new FormData();
  datos.append("archivo", archivo);
  const { data } = await api.post("/fotos", datos, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data.url;
}

// "/api/fotos/abc123.jpg" -> "abc123.jpg"
export const nombreFoto = (url) =>
  String(url ?? "")
    .split("/")
    .filter(Boolean)
    .pop() ?? "";

// GET /fotos/{nombre} exige token, así que se pide como imagen binaria con
// axios y se devuelve una URL local que sí sirve en <img src>.
// Quien la use debe liberarla con URL.revokeObjectURL.
export async function cargarFotoBlob(url) {
  const nombre = nombreFoto(url);
  const { data } = await api.get(`/fotos/${encodeURIComponent(nombre)}`, {
    responseType: "blob",
  });
  return URL.createObjectURL(data);
}
