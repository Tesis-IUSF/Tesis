// src/components/FotoCarnet.jsx
import { useEffect, useRef, useState } from "react";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";
import "./FotoCarnet.css";

const TIPOS_VALIDOS = ["image/jpeg", "image/png", "image/webp"];
const MAX_MB = 2;

/**
 * Espacio para la foto tipo carnet: elegir imagen, vista previa y quitar.
 * - archivo: File elegido (o null)
 * - onChange(file | null): se llama al elegir o quitar
 * - urlActual: foto ya guardada en el servidor (para cuando se edite un registro)
 */
function FotoCarnet({
  etiqueta = "Foto tipo carnet",
  archivo,
  onChange,
  urlActual = null,
  disabled = false,
}) {
  const inputRef = useRef(null);
  const [vista, setVista] = useState(null);
  const [error, setError] = useState("");

  // Vista previa del archivo elegido
  useEffect(() => {
    if (!archivo) return undefined;
    let cancelado = false;
    const lector = new FileReader();
    lector.onload = () => {
      if (!cancelado) setVista(lector.result);
    };
    lector.readAsDataURL(archivo);
    return () => {
      cancelado = true;
    };
  }, [archivo]);

  const imagen = archivo ? vista : urlActual;

  const abrirSelector = () => inputRef.current?.click();

  const elegir = (ev) => {
    const f = ev.target.files?.[0];
    ev.target.value = ""; // permite volver a elegir el mismo archivo
    if (!f) return;
    if (!TIPOS_VALIDOS.includes(f.type)) {
      setError("Usa una imagen JPG, PNG o WebP.");
      return;
    }
    if (f.size > MAX_MB * 1024 * 1024) {
      setError(`La imagen supera los ${MAX_MB} MB.`);
      return;
    }
    setError("");
    onChange(f);
  };

  const quitar = () => {
    setError("");
    onChange(null);
  };

  return (
    <div className="fc">
      <span className="fc-etiqueta">{etiqueta}</span>

      <button
        type="button"
        className="fc-marco"
        onClick={abrirSelector}
        disabled={disabled}
        aria-label={imagen ? "Cambiar imagen" : "Elegir imagen"}
      >
        {imagen ? (
          <img src={imagen} alt="Vista previa de la foto" />
        ) : (
          <span className="fc-vacio">
            <Icon icon={ICONOS.persona} width="44" height="44" />
            <span>Sin foto</span>
          </span>
        )}
      </button>

      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="fc-input"
        onChange={elegir}
        tabIndex={-1}
      />

      <div className="fc-acciones">
        <button
          type="button"
          className="btn-secondary"
          onClick={abrirSelector}
          disabled={disabled}
        >
          {imagen ? "Cambiar imagen" : "Elegir imagen"}
        </button>
        {archivo && (
          <button
            type="button"
            className="btn-delete"
            onClick={quitar}
            disabled={disabled}
          >
            Quitar
          </button>
        )}
      </div>

      <small className="fc-ayuda">JPG, PNG o WebP · máx. {MAX_MB} MB</small>
      {error && (
        <small className="fc-error" role="alert">
          {error}
        </small>
      )}
    </div>
  );
}

export default FotoCarnet;
