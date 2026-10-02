// src/pages/EscanerQR.jsx
import { useRef, useState, useCallback, useEffect } from "react";
import { Link } from "react-router-dom";
import jsQR from "jsqr";
import api from "../utils/api";
import { extractErrorMessage } from "../utils/errors";
import { logout } from "../utils/auth";
import "./EscanerQR.css";
import Topbar from "../components/Topbar";

function EscanerQR() {
  const videoRef = useRef(null);
  const canvasRef = useRef(document.createElement("canvas"));
  const streamRef = useRef(null);
  const animFrameRef = useRef(null);
  const procesandoRef = useRef(false);

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

  const enviarEscaneo = useCallback(async (qrToken) => {
    procesandoRef.current = true;
    setResultado({ tipo: "cargando", mensaje: "Procesando…", hora: "" });

    try {
      const { data } = await api.post("/asistencias/qr", { qrToken });
      const tipo = data.tipoRegistro === "entrada" ? "Entrada" : "Salida";
      const hora =
        data.tipoRegistro === "entrada" ? data.horaEntrada : data.horaSalida;
      const horaFormateada = hora ? hora.substring(0, 5) : "";

      setResultado({
        tipo: "success",
        mensaje: `${tipo} — ${data.empleadoNombre || "Empleado"}`,
        hora: horaFormateada,
      });
    } catch (err) {
      setResultado({
        tipo: "error",
        mensaje: extractErrorMessage(err, "QR inválido o vencido."),
        hora: "",
      });
    } finally {
      setTimeout(() => {
        procesandoRef.current = false;
        setResultado(null);
      }, 3000);
    }
  }, []);

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
                <span>📷</span>
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
                {resultado.tipo === "success"
                  ? "✅"
                  : resultado.tipo === "error"
                    ? "❌"
                    : "⏳"}
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
