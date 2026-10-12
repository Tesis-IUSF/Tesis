export function formatearFecha(valor) {
  if (!valor) return "";
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) return "";
  return fecha.toLocaleDateString("es-VE", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

// Usa el resumen; si no hay, saca un extracto del contenido (solo texto).
export function resumenDe(publicacion, maximo = 160) {
  if (publicacion.resumen) return publicacion.resumen;
  const texto = (publicacion.contenidoHtml || "")
    .replace(/<[^>]*>/g, " ")
    .replace(/&nbsp;/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  return texto.length > maximo ? `${texto.slice(0, maximo).trimEnd()}…` : texto;
}
