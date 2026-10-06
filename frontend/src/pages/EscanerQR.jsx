// src/pages/EscanerQR.jsx
import { useRef, useState, useCallback, useEffect } from "react";
import { Link } from "react-router-dom";
import jsQR from "jsqr";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import { logout } from "../utils/auth";
import "./EscanerQR.css";
import Topbar from "../components/Topbar";
import { useToast } from "../context/ToastContext";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";

function EscanerQR() {
  const videoRef = useRef(null);
  const canvasRef = useRef(document.createElement("canvas"));
  const streamRef = useRef(null);
  const animFrameRef = useRef(null);
  const procesandoRef = useRef(false);
  const toast = useToast();

  const [activo, setActivo] = useState(false);
  const [resultado, setResultado] = useState(null); // { tipo: "success"|"error"|"cargando", mensaje, hora }
  const [iniciando, setIniciando] = useState(false);

  const detener = useCallback(() => {
    if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    setActivo(false);
  }, []);

  const enviarEscaneo = useCallback(
    async (qrToken) => {
      procesandoRef.current = true;
      setResultado({ tipo: "cargando", mensaje: "Procesando…", hora: "" });

      try {
        const { data } = await api.post("/asistencias/qr", { qrToken });
        const tipo = data.tipoRegistro === "entrada" ? "Entrada" : "Salida";
        const hora =
          data.tipoRegistro === "entrada" ? data.horaEntrada : data.horaSalida;
        const horaFormateada = hora ? hora.substring(0, 5) : "";
        const nombreEmpleado = data.empleadoNombre || "Empleado";

        setResultado({
          tipo: "success",
          mensaje: `${tipo} — ${nombreEmpleado}`,
          hora: horaFormateada,
        });

        toast.success(
          `${tipo} registrada para ${nombreEmpleado} a las ${horaFormateada}.`,
        );
      } catch (err) {
        const status = err.response?.status;
        let mensaje;

        // Mensajes específicos según el criterio de SW-53 (400, 401, 404, 409)
        switch (status) {
          case 400:
            mensaje = "El código QR no tiene un formato válido.";
            break;
          case 401:
            mensaje = "QR inválido, revocado o vencido.";
            break;
          case 404:
            mensaje = "No se encontró al empleado asociado a este QR.";
            break;
          case 409:
            // El backend ya manda el mensaje exacto en estos casos:
            // "ya tiene entrada y salida registradas", "no tiene turno asignado",
            // "la salida debe registrarse después de la entrada", etc.
            mensaje =
              err.response?.data?.message || "Ya se registró esta asistencia.";
            break;
          default:
            mensaje = "No se pudo conectar con el servidor. Intenta de nuevo.";
        }

        setResultado({ tipo: "error", mensaje, hora: "" });
        toast.error(mensaje);
      } finally {
        setTimeout(() => {
          procesandoRef.current = false;
          setResultado(null);
        }, 3000);
      }
    },
    [toast],
  );

  const loopEscaneo = useCallback(() => {
    const video = videoRef.current;
    const canvas = canvasRef.current;

    if (video && video.readyState === video.HAVE_ENOUGH_DATA) {
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const ctx = canvas.getContext("2d");
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
      const codigo = jsQR(imageData.data, imageData.width, imageData.height);

      if (codigo && codigo.data && !procesandoRef.current) {
        enviarEscaneo(codigo.data);
      }
    }

    animFrameRef.current = requestAnimationFrame(loopEscaneo);
  }, [enviarEscaneo]);

  const iniciar = useCallback(async () => {
    setIniciando(true);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setActivo(true);
      animFrameRef.current = requestAnimationFrame(loopEscaneo);
    } catch (err) {
      setResultado({
        tipo: "error",
        mensaje:
          "No se pudo acceder a la cámara. Verifica los permisos del navegador.",
        hora: "",
      });
    } finally {
      setIniciando(false);
    }
  }, [loopEscaneo]);

  useEffect(() => {
    return () => detener(); // limpia la cámara al salir de la página
  }, [detener]);

  return (
    <div className="escaner-page">
      <Topbar />

      <main className="escaner-main">
        <section className="escaner-panel">
          <h2>Apunta la cámara al código QR del empleado</h2>

          <div className="qr-video-wrapper">
            <video ref={videoRef} className="qr-video" muted playsInline />
            {!activo && (
              <div className="qr-video-placeholder">
                <span>
                  <Icon icon={ICONOS.camara} />
                </span>
                <p>La cámara está detenida</p>
              </div>
            )}
          </div>

          <div className="escaner-controles">
            {!activo ? (
              <button
                className="btn-consultar"
                onClick={iniciar}
                disabled={iniciando}
              >
                {iniciando ? "Iniciando…" : "Iniciar escáner"}
              </button>
            ) : (
              <button className="btn-back" onClick={detener}>
                Detener escáner
              </button>
            )}
          </div>

          {resultado && (
            <div className={`escaner-resultado escaner-${resultado.tipo}`}>
              <span className="escaner-icono">
                <Icon
                  icon={
                    resultado.tipo === "success"
                      ? ICONOS.exito
                      : resultado.tipo === "error"
                        ? ICONOS.error
                        : ICONOS.cargando
                  }
                />
              </span>
              <p className="escaner-mensaje">{resultado.mensaje}</p>
              {resultado.hora && (
                <p className="escaner-hora">{resultado.hora}</p>
              )}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export default EscanerQR;
