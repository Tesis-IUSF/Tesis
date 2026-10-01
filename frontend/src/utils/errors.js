export function extractErrorMessage(
  err,
  fallback = "Ocurrió un error inesperado. Intenta de nuevo.",
) {
  if (!err?.response) {
    return "No se pudo conectar con el servidor. Verifica tu conexión.";
  }

  const { status, data } = err.response;

  if (data?.message) {
    return data.message;
  }

  switch (status) {
    case 401:
    case 403:
      return "No tienes permiso para realizar esta acción.";
    case 404:
      return "No se encontró el recurso solicitado.";
    case 409:
      return "El registro ya existe o entra en conflicto con uno existente.";
    default:
      return fallback;
  }
}
