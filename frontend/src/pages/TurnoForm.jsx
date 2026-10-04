// src/pages/TurnoForm.jsx
import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import Topbar from "../components/Topbar";
import "./TurnoForm.css";

const DIAS = [
  { campo: "lunes", etiqueta: "Lunes" },
  { campo: "martes", etiqueta: "Martes" },
  { campo: "miercoles", etiqueta: "Miércoles" },
  { campo: "jueves", etiqueta: "Jueves" },
  { campo: "viernes", etiqueta: "Viernes" },
  { campo: "sabado", etiqueta: "Sábado" },
  { campo: "domingo", etiqueta: "Domingo" },
];

const VACIO = {
  nombre: "",
  horaEntrada: "",
  horaSalida: "",
  toleranciaMin: 0,
  minutosSalidaAnticipadaPermitidos: 0,
  requiereJustificacionTardanza: false,
  lunes: true,
  martes: true,
  miercoles: true,
  jueves: true,
  viernes: true,
  sabado: false,
  domingo: false,
  activo: true,
};

function TurnoForm() {
  const { id } = useParams();
  const esEdicion = !!id;
  const navigate = useNavigate();
  const toast = useToast();

  const [form, setForm] = useState(VACIO);
  const [cargandoDatos, setCargandoDatos] = useState(esEdicion);
  const [guardando, setGuardando] = useState(false);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    if (!esEdicion) return;
    api
      .get(`/catalogos/turnos/${id}`)
      .catch(() => api.get("/catalogos/turnos", { params: { size: 100 } }))
      .then((res) => {
        // Si el backend no tiene GET /turnos/{id}, buscamos en la lista paginada
        const data = res.data.content
          ? res.data.content.find((t) => String(t.id) === String(id))
          : res.data;
        if (!data) throw new Error("Turno no encontrado");
        setForm({
          nombre: data.nombre || "",
          horaEntrada: data.horaEntrada ? data.horaEntrada.substring(0, 5) : "",
          horaSalida: data.horaSalida ? data.horaSalida.substring(0, 5) : "",
          toleranciaMin: data.toleranciaMin ?? 0,
          minutosSalidaAnticipadaPermitidos:
            data.minutosSalidaAnticipadaPermitidos ?? 0,
          requiereJustificacionTardanza: !!data.requiereJustificacionTardanza,
          lunes: !!data.lunes,
          martes: !!data.martes,
          miercoles: !!data.miercoles,
          jueves: !!data.jueves,
          viernes: !!data.viernes,
          sabado: !!data.sabado,
          domingo: !!data.domingo,
          activo: data.activo ?? true,
        });
      })
      .catch((err) => {
        toast.error(extractErrorMessage(err, "No se pudo cargar el turno."));
        navigate("/turnos");
      })
      .finally(() => setCargandoDatos(false));
  }, [id, esEdicion, navigate, toast]);

  const actualizarCampo = (campo, valor) => {
    setForm((prev) => ({ ...prev, [campo]: valor }));
    setErrores((prev) => ({ ...prev, [campo]: undefined }));
  };

  const validar = () => {
    const nuevosErrores = {};
    if (!form.nombre.trim()) nuevosErrores.nombre = "El nombre es obligatorio.";
    if (!form.horaEntrada)
      nuevosErrores.horaEntrada = "La hora de entrada es obligatoria.";
    if (!form.horaSalida)
      nuevosErrores.horaSalida = "La hora de salida es obligatoria.";
    // Nota: no se valida que horaSalida > horaEntrada — los turnos de
    // vigilancia cruzan medianoche (ej: 22:00 a 06:00).

    setErrores(nuevosErrores);
    return Object.keys(nuevosErrores).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validar()) return;

    setGuardando(true);
    const payload = {
      nombre: form.nombre,
      horaEntrada: `${form.horaEntrada}:00`,
      horaSalida: `${form.horaSalida}:00`,
      toleranciaMin: Number(form.toleranciaMin) || 0,
      minutosSalidaAnticipadaPermitidos:
        Number(form.minutosSalidaAnticipadaPermitidos) || 0,
      requiereJustificacionTardanza: form.requiereJustificacionTardanza,
      lunes: form.lunes,
      martes: form.martes,
      miercoles: form.miercoles,
      jueves: form.jueves,
      viernes: form.viernes,
      sabado: form.sabado,
      domingo: form.domingo,
      activo: form.activo,
    };

    try {
      if (esEdicion) {
        await api.put(`/catalogos/turnos/${id}`, payload);
        toast.success("Turno actualizado correctamente.");
      } else {
        await api.post("/catalogos/turnos", payload);
        toast.success("Turno creado correctamente.");
      }
      navigate("/turnos");
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo guardar el turno."));
    } finally {
      setGuardando(false);
    }
  };

  if (cargandoDatos) {
    return (
      <>
        <Topbar mostrarVolver />
        <div className="mod-main">
          <p>Cargando turno...</p>
        </div>
      </>
    );
  }

  return (
    <>
      <Topbar mostrarVolver />
      <div className="mod-main">
        <div className="mod-heading">
          <h1>{esEdicion ? "Editar Turno" : "Crear Turno"}</h1>
        </div>

        <form className="turno-form" onSubmit={handleSubmit}>
          <div className="form-grid-2">
            <label>
              Nombre *
              <input
                value={form.nombre}
                onChange={(e) => actualizarCampo("nombre", e.target.value)}
                placeholder="Ej: Turno Mañana"
              />
              {errores.nombre && (
                <span className="campo-error">{errores.nombre}</span>
              )}
            </label>

            <label>
              Tolerancia (minutos)
              <input
                type="number"
                min="0"
                value={form.toleranciaMin}
                onChange={(e) =>
                  actualizarCampo("toleranciaMin", e.target.value)
                }
              />
            </label>

            <label>
              Hora de entrada *
              <input
                type="time"
                value={form.horaEntrada}
                onChange={(e) => actualizarCampo("horaEntrada", e.target.value)}
              />
              {errores.horaEntrada && (
                <span className="campo-error">{errores.horaEntrada}</span>
              )}
            </label>

            <label>
              Hora de salida *
              <input
                type="time"
                value={form.horaSalida}
                onChange={(e) => actualizarCampo("horaSalida", e.target.value)}
              />
              {errores.horaSalida && (
                <span className="campo-error">{errores.horaSalida}</span>
              )}
            </label>

            <label>
              Minutos de salida anticipada permitidos
              <input
                type="number"
                min="0"
                value={form.minutosSalidaAnticipadaPermitidos}
                onChange={(e) =>
                  actualizarCampo(
                    "minutosSalidaAnticipadaPermitidos",
                    e.target.value,
                  )
                }
              />
            </label>

            <label className="switch-inline-label">
              <input
                type="checkbox"
                checked={form.requiereJustificacionTardanza}
                onChange={(e) =>
                  actualizarCampo(
                    "requiereJustificacionTardanza",
                    e.target.checked,
                  )
                }
              />
              Requiere justificación de tardanza
            </label>
          </div>

          <p className="turno-form-nota">
            ⚠️ Si la hora de salida es menor que la de entrada, el turno se
            interpreta como un turno nocturno que cruza la medianoche.
          </p>

          <fieldset className="dias-fieldset">
            <legend>Días laborables</legend>
            <div className="dias-grid">
              {DIAS.map((d) => (
                <label key={d.campo} className="dia-checkbox">
                  <input
                    type="checkbox"
                    checked={form[d.campo]}
                    onChange={(e) => actualizarCampo(d.campo, e.target.checked)}
                  />
                  {d.etiqueta}
                </label>
              ))}
            </div>
          </fieldset>

          <label className="switch-inline-label">
            <input
              type="checkbox"
              checked={form.activo}
              onChange={(e) => actualizarCampo("activo", e.target.checked)}
            />
            Turno activo
          </label>

          <div className="form-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate("/turnos")}
              disabled={guardando}
            >
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={guardando}>
              {guardando
                ? "Guardando..."
                : esEdicion
                  ? "Guardar cambios"
                  : "Crear turno"}
            </button>
          </div>
        </form>
      </div>
    </>
  );
}

export default TurnoForm;
