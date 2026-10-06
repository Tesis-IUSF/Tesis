import { useEffect, useState } from "react";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";
import "./Toast.css";

const ICONO_POR_TIPO = {
  success: ICONOS.exito,
  error: ICONOS.error,
  info: ICONOS.info,
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
      <span className="toast-icono">
        <Icon icon={ICONO_POR_TIPO[type] || ICONOS.info} />
      </span>
      <span className="toast-mensaje">{message}</span>
      <button className="toast-cerrar" onClick={cerrar} aria-label="Cerrar">
        <Icon icon={ICONOS.cerrar} />
      </button>
    </div>
  );
}

export default Toast;
