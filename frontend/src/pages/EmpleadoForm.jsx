// src/pages/EmpleadoForm.jsx
import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { useCargos, useDepartamentos, useTurnos } from "../hooks/useCatalogos";
import Topbar from "../components/Topbar";
import "./EmpleadoForm.css";

const VACIO = {
  nombre: "",
  apellido: "",
  cedula: "",
  correoLocal: "",
  telefono: "",
  fechaNacimiento: "",
  sexo: "",
  cargoId: "",
  departamentoId: "",
  fechaIngreso: "",
  turnoId: "",
};

const hoyISO = () => new Date().toISOString().split("T")[0];

const DOMINIO_CORREO = "@asansa.local";

const soloDigitos = (valor) => valor.replace(/\D/g, "");

function EmpleadoForm() {
  const { id } = useParams();
  const esEdicion = !!id;
  const navigate = useNavigate();
  const toast = useToast();
  const { items: cargos } = useCargos();
  const { items: departamentos } = useDepartamentos();
  const { items: turnos } = useTurnos();

  const [form, setForm] = useState(VACIO);
  const [turnoIdOriginal, setTurnoIdOriginal] = useState("");
  const [fechaDesdeTurno, setFechaDesdeTurno] = useState(hoyISO());
  const [cargandoDatos, setCargandoDatos] = useState(esEdicion);
  const [guardando, setGuardando] = useState(false);
  const [errores, setErrores] = useState({});

  useEffect(() => {
    if (!esEdicion) return;
    api
      .get(`/empleados/${id}`)
      .then(({ data }) => {
        const correoCompleto = data.correo || "";
        const [local] = correoCompleto.split("@");
        setForm({
          nombre: data.nombre || "",
          apellido: data.apellido || "",
          cedula: data.cedula || "",
          correoLocal: local || "",
          telefono: data.telefono || "",
          fechaNacimiento: data.fechaNacimiento || "",
          sexo: data.sexo || "",
          cargoId: data.cargoId ?? "",
          departamentoId: data.departamentoId ?? "",
          fechaIngreso: data.fechaIngreso || "",
          turnoId: data.turnoId ?? "",
        });
        setTurnoIdOriginal(data.turnoId ?? "");
      })
      .catch((err) => {
        toast.error(extractErrorMessage(err, "No se pudo cargar el empleado."));
        navigate("/empleados");
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
    if (!form.apellido.trim())
      nuevosErrores.apellido = "El apellido es obligatorio.";

    if (!form.cedula.trim()) {
      nuevosErrores.cedula = "La cédula es obligatoria.";
    } else if (form.cedula.length < 6 || form.cedula.length > 8) {
      nuevosErrores.cedula = "La cédula debe tener entre 6 y 8 dígitos.";
    }

    if (form.telefono && form.telefono.length !== 11) {
      nuevosErrores.telefono =
        "El teléfono debe tener 11 dígitos (ej: 04121234567).";
    }

    if (form.correoLocal && !/^[a-zA-Z0-9._-]+$/.test(form.correoLocal)) {
      nuevosErrores.correoLocal =
        "Solo letras, números, puntos, guiones y guion bajo.";
    }

    setErrores(nuevosErrores);
    return Object.keys(nuevosErrores).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validar()) return;

    setGuardando(true);
    const payload = {
      nombre: form.nombre,
      apellido: form.apellido,
      cedula: form.cedula,
      correo: form.correoLocal ? `${form.correoLocal}${DOMINIO_CORREO}` : null,
      telefono: form.telefono || null,
      sexo: form.sexo || null,
      cargoId: form.cargoId ? Number(form.cargoId) : null,
      departamentoId: form.departamentoId ? Number(form.departamentoId) : null,
      fechaNacimiento: form.fechaNacimiento || null,
      fechaIngreso: form.fechaIngreso || null,
    };

    try {
      let empleadoId = id;
      if (esEdicion) {
        await api.put(`/empleados/${id}`, payload);
        toast.success("Empleado actualizado correctamente.");
      } else {
        const { data } = await api.post("/empleados", payload);
        empleadoId = data.id;
        toast.success("Empleado creado correctamente.");
      }

      // Asignación de turno: solo aplica en edición, y solo si cambió
      // Asignación de turno: solo aplica en edición, y solo si cambió
      if (
        esEdicion &&
        form.turnoId &&
        String(form.turnoId) !== String(turnoIdOriginal)
      ) {
        try {
          if (turnoIdOriginal) {
            // Ya tenía turno: reemplazar la asignación vigente
            await api.put(
              `/asignaciones-turnos/empleado/${empleadoId}/vigente`,
              {
                turnoId: Number(form.turnoId),
                fechaDesde: fechaDesdeTurno,
              },
            );
          } else {
            // Primera asignación: crear una nueva
            await api.post("/asignaciones-turnos", {
              empleadoId: Number(empleadoId),
              turnoId: Number(form.turnoId),
              fechaDesde: fechaDesdeTurno,
            });
          }
          toast.success("Turno actualizado correctamente.");
        } catch (errTurno) {
          toast.error(
            extractErrorMessage(
              errTurno,
              "No se pudo actualizar el turno del empleado.",
            ),
          );
        }
      }

      navigate("/empleados");
    } catch (err) {
      toast.error(extractErrorMessage(err, "No se pudo guardar el empleado."));
    } finally {
      setGuardando(false);
    }
  };

  if (cargandoDatos) {
    return (
      <>
        <Topbar mostrarVolver />
        <div className="mod-main">
          <p>Cargando empleado...</p>
        </div>
      </>
    );
  }

  return (
    <>
      <Topbar mostrarVolver />
      <div className="mod-main">
        <div className="mod-heading">
          <h1>{esEdicion ? "Editar Empleado" : "Crear Empleado"}</h1>
        </div>

        <form className="empleado-form" onSubmit={handleSubmit}>
          <div className="form-grid-2">
            <label>
              Nombre *
              <input
                value={form.nombre}
                onChange={(e) => actualizarCampo("nombre", e.target.value)}
              />
              {errores.nombre && (
                <span className="campo-error">{errores.nombre}</span>
              )}
            </label>

            <label>
              Apellido *
              <input
                value={form.apellido}
                onChange={(e) => actualizarCampo("apellido", e.target.value)}
              />
              {errores.apellido && (
                <span className="campo-error">{errores.apellido}</span>
              )}
            </label>

            <label>
              Cédula *
              <input
                value={form.cedula}
                onChange={(e) =>
                  actualizarCampo(
                    "cedula",
                    soloDigitos(e.target.value).slice(0, 8),
                  )
                }
                inputMode="numeric"
                placeholder="12345678"
              />
              {errores.cedula && (
                <span className="campo-error">{errores.cedula}</span>
              )}
            </label>

            <label>
              Correo
              <div className="input-group">
                <input
                  value={form.correoLocal}
                  onChange={(e) =>
                    actualizarCampo("correoLocal", e.target.value.trim())
                  }
                  placeholder="nombre.apellido"
                />
                <span className="input-group-suffix">{DOMINIO_CORREO}</span>
              </div>
              {errores.correoLocal && (
                <span className="campo-error">{errores.correoLocal}</span>
              )}
            </label>

            <label>
              Teléfono
              <input
                value={form.telefono}
                onChange={(e) =>
                  actualizarCampo(
                    "telefono",
                    soloDigitos(e.target.value).slice(0, 11),
                  )
                }
                inputMode="numeric"
                placeholder="04121234567"
              />
              {errores.telefono && (
                <span className="campo-error">{errores.telefono}</span>
              )}
            </label>

            <label>
              Sexo
              <select
                value={form.sexo}
                onChange={(e) => actualizarCampo("sexo", e.target.value)}
              >
                <option value="">Selecciona...</option>
                <option value="M">Masculino</option>
                <option value="F">Femenino</option>
                <option value="Otro">Otro</option>
              </select>
            </label>

            <label>
              Fecha de nacimiento
              <input
                type="date"
                value={form.fechaNacimiento}
                onChange={(e) =>
                  actualizarCampo("fechaNacimiento", e.target.value)
                }
              />
            </label>

            <label>
              Fecha de ingreso
              <input
                type="date"
                value={form.fechaIngreso}
                onChange={(e) =>
                  actualizarCampo("fechaIngreso", e.target.value)
                }
              />
            </label>

            <label>
              Cargo
              <select
                value={form.cargoId}
                onChange={(e) => actualizarCampo("cargoId", e.target.value)}
              >
                <option value="">Selecciona...</option>
                {cargos.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombreCargo}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Departamento
              <select
                value={form.departamentoId}
                onChange={(e) =>
                  actualizarCampo("departamentoId", e.target.value)
                }
              >
                <option value="">Selecciona...</option>
                {departamentos.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nombre}
                  </option>
                ))}
              </select>
            </label>

            {esEdicion ? (
              <>
                <label>
                  Turno asignado
                  <select
                    value={form.turnoId}
                    onChange={(e) => actualizarCampo("turnoId", e.target.value)}
                  >
                    <option value="">Sin turno asignado</option>
                    {turnos.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.nombre}
                      </option>
                    ))}
                  </select>
                </label>

                {String(form.turnoId) !== String(turnoIdOriginal) &&
                  form.turnoId && (
                    <label>
                      Turno efectivo desde
                      <input
                        type="date"
                        value={fechaDesdeTurno}
                        onChange={(e) => setFechaDesdeTurno(e.target.value)}
                      />
                    </label>
                  )}
              </>
            ) : (
              <label>
                Turno
                <input
                  value="Podrás asignar el turno después de crear al empleado."
                  readOnly
                  disabled
                />
              </label>
            )}
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate("/empleados")}
              disabled={guardando}
            >
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={guardando}>
              {guardando
                ? "Guardando..."
                : esEdicion
                  ? "Guardar cambios"
                  : "Crear empleado"}
            </button>
          </div>
        </form>
      </div>
    </>
  );
}

export default EmpleadoForm;
