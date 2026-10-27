// src/components/ModalFormulario.jsx
import { useCallback, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import "./ModalFormulario.css";

function ModalFormulario({
  titulo,
  onCerrar,
  onSubmit,
  guardando,
  etiquetaGuardar,
  className = "",
  children,
}) {
  const [cerrando, setCerrando] = useState(false);

  const cerrar = useCallback(() => {
    if (guardando || cerrando) return;
    setCerrando(true);
  }, [cerrando, guardando]);

  useEffect(() => {
    const alPulsar = (e) => {
      if (e.key === "Escape") cerrar();
    };
    document.addEventListener("keydown", alPulsar);
    return () => document.removeEventListener("keydown", alPulsar);
  }, [cerrar]);

  return createPortal(
    <div
      className={`mf-overlay${cerrando ? " mf-overlay--cerrando" : ""}`}
      onClick={cerrar}
      onAnimationEnd={(event) => {
        if (cerrando && event.target === event.currentTarget) onCerrar();
      }}
    >
      <form
        className={`mf-modal ${className}`.trim()}
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
            onClick={cerrar}
            disabled={guardando}
          >
            Cancelar
          </button>
          <button type="submit" className="btn-primary" disabled={guardando}>
            {guardando ? "Guardando..." : etiquetaGuardar}
          </button>
        </div>
      </form>
    </div>,
    document.body,
  );
}

export default ModalFormulario;
