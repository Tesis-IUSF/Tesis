// src/components/ModalFormulario.jsx
import { useEffect } from "react";
import "./ModalFormulario.css";

function ModalFormulario({
  titulo,
  onCerrar,
  onSubmit,
  guardando,
  etiquetaGuardar,
  children,
}) {
  // Esc cierra el modal (salvo mientras se guarda)
  useEffect(() => {
    const alPulsar = (e) => {
      if (e.key === "Escape" && !guardando) onCerrar();
    };
    document.addEventListener("keydown", alPulsar);
    return () => document.removeEventListener("keydown", alPulsar);
  }, [guardando, onCerrar]);

  return (
    <div className="mf-overlay" onClick={() => !guardando && onCerrar()}>
      <form
        className="mf-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="mf-titulo"
        onClick={(e) => e.stopPropagation()}
        onSubmit={onSubmit}
        noValidate
      >
        <h2 id="mf-titulo">{titulo}</h2>

        {children}

        <div className="mf-acciones">
          <button
            type="button"
            className="btn-secondary"
            onClick={onCerrar}
            disabled={guardando}
          >
            Cancelar
          </button>
          <button type="submit" className="btn-primary" disabled={guardando}>
            {guardando ? "Guardando..." : etiquetaGuardar}
          </button>
        </div>
      </form>
    </div>
  );
}

export default ModalFormulario;
