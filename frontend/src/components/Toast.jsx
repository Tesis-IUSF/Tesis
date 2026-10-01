import { useEffect, useState } from "react";
import "./Toast.css";

const ICONOS = {
  success: "✓",
  error: "✕",
  info: "ℹ",
};

function Toast({ message, type, onClose }) {
  const [saliendo, setSaliendo] = useState(false);

  const cerrar = () => {
    setSaliendo(true);
    setTimeout(onClose, 200);
  };

  useEffect(() => {
    // Permite que la animación de entrada se vea desde el principio
  }, []);

  return (
    <div
      className={`toast toast--${type} ${saliendo ? "toast--saliendo" : ""}`}
      role="alert"
    >
      <span className="toast-icono">{ICONOS[type] || ICONOS.info}</span>
      <span className="toast-mensaje">{message}</span>
      <button className="toast-cerrar" onClick={cerrar} aria-label="Cerrar">
        ×
      </button>
    </div>
  );
}

export default Toast;
