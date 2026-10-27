export const TELEFONO_VENEZOLANO =
  /^(?:\+?58\s?|0)?(?:2\d{2}|412|414|424|416|426)\d{7}$/;

export const URL_HTTP_HTTPS = /^https?:\/\/\S+$/i;

export function esUrlHttpHttpsOpcional(valor) {
  const texto = String(valor ?? "").trim();
  return texto === "" || URL_HTTP_HTTPS.test(texto);
}

export function soloDigitos(valor) {
  return String(valor ?? "").replace(/\D/g, "");
}

export function limpiarEntradaTelefono(valor) {
  return String(valor ?? "")
    .replace(/[^\d+\s]/g, "")
    .replace(/\s+/g, " ")
    .slice(0, 20);
}
