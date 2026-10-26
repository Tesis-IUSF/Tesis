// src/pages/EmpleadoForm.jsx
import { useState, useEffect, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { useCargos, useDepartamentos, useTurnos } from "../hooks/useCatalogos";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";
import "./EmpleadoForm.css";

const VACIO = {
  nombre: "",
  apellido: "",
  cedulaTipo: "",
  cedulaNumero: "",
  correoLocal: "",
  telefono: "",
  fechaNacimiento: "",
  sexo: "",
  cargoId: "",
  departamentoId: "",
  fechaIngreso: "",
  turnoId: "",
  activo: true,
  fechaEgreso: "",
};

const hoyISO = () => new Date().toISOString().split("T")[0];

const DOMINIO_CORREO = "@asansa.local";

const soloDigitos = (valor) => valor.replace(/\D/g, "");

const separarCedula = (valor) => {
  const cedula = String(valor || "").trim().toUpperCase();
  const conPrefijo = cedula.match(/^([VE])\s*-\s*(\d+)$/);
  if (conPrefijo) {
    return { cedulaTipo: conPrefijo[1], cedulaNumero: conPrefijo[2] };
  }

  return { cedulaTipo: "", cedulaNumero: soloDigitos(cedula).slice(0, 8) };
};

const validarCedula = (tipo, numero) => {
  if (!tipo) return "Selecciona el tipo de cédula.";
  if (tipo === "V" && /^(?:[6-9]\d{6}|[1-9]\d{7})$/.test(numero)) return "";
  if (tipo === "E" && /^[89]\d{7}$/.test(numero)) return "";
  if (tipo === "V") {
    return "La cédula V debe estar entre 6.000.000 y 99.999.999.";
  }
  return "La cédula E debe estar entre 80.000.000 y 99.999.999.";
};

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
  const [historialTurnos, setHistorialTurnos] = useState([]);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);

  const cargarHistorialTurnos = useCallback(() => {
    if (!id) return;
    setCargandoHistorial(true);
    api
      .get(`/asignaciones-turnos/empleado/${id}`)
      .then(({ data }) => setHistorialTurnos(data))
      .catch(() => {})
      .finally(() => setCargandoHistorial(false));
  }, [id]);

  useEffect(() => {
    if (!esEdicion) return;
    api
      .get(`/empleados/${id}`)
      .then(({ data }) => {
        const correoCompleto = data.correo || "";
        const [local] = correoCompleto.split("@");
        const cedula = separarCedula(data.cedula);
        setForm({
          nombre: data.nombre || "",
          apellido: data.apellido || "",
          ...cedula,
          correoLocal: local || "",
          telefono: data.telefono || "",
          fechaNacimiento: data.fechaNacimiento || "",
          sexo: data.sexo || "",
          cargoId: data.cargoId ?? "",
          departamentoId: data.departamentoId ?? "",
          fechaIngreso: data.fechaIngreso || "",
          turnoId: data.turnoId ?? "",
          activo: data.activo !== false,
          fechaEgreso: data.fechaEgreso || "",
        });
        setTurnoIdOriginal(data.turnoId ?? "");
      })
      .catch((err) => {
        toast.error(extractErrorMessage(err, "No se pudo cargar el empleado."));
        navigate("/empleados");
      })
      .finally(() => setCargandoDatos(false));
    cargarHistorialTurnos();
  }, [id, esEdicion, navigate, toast, cargarHistorialTurnos]);

  const turnosDisponibles = turnos.filter(
    (t) =>
      t.activo !== false ||
      (esEdicion && String(t.id) === String(turnoIdOriginal)),
  );

  const actualizarCampo = (campo, valor) => {
    setForm((prev) => ({ ...prev, [campo]: valor }));
    setErrores((prev) => ({
      ...prev,
      [campo]: undefined,
      ...(campo === "cedulaTipo" || campo === "cedulaNumero"
        ? { cedula: undefined }
        : {}),
    }));
  };

  const validar = () => {
    const nuevosErrores = {};
    if (!form.nombre.trim()) nuevosErrores.nombre = "El nombre es obligatorio.";
    if (!form.apellido.trim())
      nuevosErrores.apellido = "El apellido es obligatorio.";

    const errorCedula = validarCedula(form.cedulaTipo, form.cedulaNumero);
    if (errorCedula) nuevosErrores.cedula = errorCedula;

    if (form.telefono && form.telefono.length !== 11) {
      nuevosErrores.telefono =
        "El teléfono debe tener 11 dígitos (ej: 04121234567).";
    }

    if (form.correoLocal && !/^[a-zA-Z0-9._-]+$/.test(form.correoLocal)) {
      nuevosErrores.correoLocal =
        "Solo letras, números, puntos, guiones y guion bajo.";
    }

    if (!form.fechaIngreso) {
      nuevosErrores.fechaIngreso = "La fecha de ingreso es obligatoria.";
    }

    if (esEdicion && !form.activo) {
      if (!form.fechaEgreso) {
        nuevosErrores.fechaEgreso = "La fecha de egreso es obligatoria.";
      } else if (
        form.fechaIngreso &&
        form.fechaEgreso < form.fechaIngreso
      ) {
        nuevosErrores.fechaEgreso =
          "La fecha de egreso no puede ser anterior a la fecha de ingreso.";
      }
    }

    if (!form.cargoId) {
      nuevosErrores.cargoId = "El cargo es obligatorio.";
    }

    if (!esEdicion && !form.turnoId) {
      nuevosErrores.turnoId = "El turno es obligatorio.";
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
      cedula: `${form.cedulaTipo}-${form.cedulaNumero}`.trim().toUpperCase(),
      correo: form.correoLocal ? `${form.correoLocal}${DOMINIO_CORREO}` : null,
      telefono: form.telefono || null,
      sexo: form.sexo || null,
      cargoId: form.cargoId ? Number(form.cargoId) : null,
      departamentoId: form.departamentoId ? Number(form.departamentoId) : null,
      fechaNacimiento: form.fechaNacimiento || null,
      fechaIngreso: form.fechaIngreso,
    };

    try {
      if (esEdicion) {
        payload.activo = form.activo;
        payload.fechaEgreso = !form.activo ? form.fechaEgreso || null : null;
        await api.put(`/empleados/${id}`, payload);
        toast.success("Empleado actualizado correctamente.");

        // Cambio de turno: solo si el empleado sigue activo y cambió el turno
        if (
          form.activo &&
          form.turnoId &&
          String(form.turnoId) !== String(turnoIdOriginal)
        ) {
          try {
            if (turnoIdOriginal) {
              // Ya tenía turno → reemplazar la asignación vigente
              await api.put(
                `/asignaciones-turnos/empleado/${id}/vigente`,
                {
                  turnoId: Number(form.turnoId),
                  fechaDesde: fechaDesdeTurno,
                },
              );
            } else {
              // Sin turno previo → crear asignación directamente
              await api.post("/asignaciones-turnos", {
                empleadoId: Number(id),
                turnoId: Number(form.turnoId),
                fechaDesde: fechaDesdeTurno,
              });
            }
            toast.success("Turno actualizado correctamente.");
            // Refrescar historial tras el cambio
            cargarHistorialTurnos();
          } catch (errTurno) {
            toast.error(
              extractErrorMessage(
                errTurno,
                "No se pudo actualizar el turno del empleado.",
              ),
            );
            // No navegar: dejar al usuario corregir la fechaDesde
            setGuardando(false);
            return;
          }
        }
      } else {
        payload.turnoId = Number(form.turnoId);
        await api.post("/empleados", payload);
        toast.success("Empleado creado correctamente.");
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
        <div className="mod-main">
          <p>Cargando empleado...</p>
        </div>
      </>
    );
  }

  return (
    <>
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
              <div className="cedula-controls">
                <select
                  value={form.cedulaTipo}
                  onChange={(e) =>
                    actualizarCampo("cedulaTipo", e.target.value)
                  }
                  aria-label="Tipo de cédula"
                >
                  <option value="">Tipo</option>
                  <option value="V">Venezolana (V)</option>
                  <option value="E">Extranjera (E)</option>
                </select>
                <input
                  value={form.cedulaNumero}
                  onChange={(e) =>
                    actualizarCampo(
                      "cedulaNumero",
                      soloDigitos(e.target.value).slice(0, 8),
                    )
                  }
                  inputMode="numeric"
                  maxLength={8}
                  placeholder="12345678"
                  aria-label="Número de cédula"
                />
              </div>
              <span className="campo-ayuda">
                V: desde 6.000.000 · E: desde 80.000.000.
              </span>
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
              Fecha de ingreso *
              <input
                type="date"
                value={form.fechaIngreso}
                onChange={(e) =>
                  actualizarCampo("fechaIngreso", e.target.value)
                }
              />
              {errores.fechaIngreso && (
                <span className="campo-error">{errores.fechaIngreso}</span>
              )}
            </label>

            <label>
              Cargo *
              <select
                value={form.cargoId}
                onChange={(e) => actualizarCampo("cargoId", e.target.value)}
              >
                <option value="">Selecciona un cargo...</option>
                {cargos.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombreCargo}
                  </option>
                ))}
              </select>
              {errores.cargoId && (
                <span className="campo-error">{errores.cargoId}</span>
              )}
            </label>

            <label>
              Departamento
              <select
                value={form.departamentoId}
                onChange={(e) =>
                  actualizarCampo("departamentoId", e.target.value)
                }
              >
                <option value="">Sin departamento (Opcional)</option>
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
                    disabled={!form.activo}
                  >
                    <option value="">Sin turno asignado</option>
                    {turnosDisponibles.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.nombre} {!t.activo ? "(Inactivo)" : ""}
                      </option>
                    ))}
                  </select>
                </label>

                {form.activo &&
                  String(form.turnoId) !== String(turnoIdOriginal) &&
                  form.turnoId && (
                    <label>
                      Turno efectivo desde *
                      <input
                        type="date"
                        value={fechaDesdeTurno}
                        onChange={(e) => setFechaDesdeTurno(e.target.value)}
                        required
                      />
                      <span className="campo-ayuda">
                        La asignación vigente se cerrará el día anterior.
                      </span>
                    </label>
                  )}

                <label>
                  Estado
                  <select
                    value={form.activo ? "true" : "false"}
                    onChange={(e) => {
                      const nuevoActivo = e.target.value === "true";
                      actualizarCampo("activo", nuevoActivo);
                      if (nuevoActivo) {
                        actualizarCampo("fechaEgreso", "");
                      } else if (!form.fechaEgreso) {
                        actualizarCampo("fechaEgreso", hoyISO());
                      }
                    }}
                  >
                    <option value="true">Activo</option>
                    <option value="false">Inactivo (Dado de baja)</option>
                  </select>
                </label>

                {!form.activo && (
                  <label>
                    Fecha de egreso
                    <input
                      type="date"
                      value={form.fechaEgreso}
                      min={form.fechaIngreso || undefined}
                      required
                      onChange={(e) =>
                        actualizarCampo("fechaEgreso", e.target.value)
                      }
                    />
                    {errores.fechaEgreso && (
                      <span className="campo-error">
                        {errores.fechaEgreso}
                      </span>
                    )}
                  </label>
                )}
              </>
            ) : (
              <label>
                Turno *
                <select
                  value={form.turnoId}
                  onChange={(e) => actualizarCampo("turnoId", e.target.value)}
                >
                  <option value="">Selecciona un turno activo...</option>
                  {turnosDisponibles.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.nombre}
                    </option>
                  ))}
                </select>
                {errores.turnoId && (
                  <span className="campo-error">{errores.turnoId}</span>
                )}
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

        {/* Historial de asignaciones de turno (solo en edición) */}
        {esEdicion && (
          <section className="historial-turnos">
            <div className="historial-heading">
              <h2>
                <Icon icon={ICONOS.reloj} className="icono-inline" />
                Historial de turnos
              </h2>
            </div>

            {cargandoHistorial ? (
              <p className="historial-estado">Cargando historial...</p>
            ) : historialTurnos.length === 0 ? (
              <p className="historial-estado">Sin asignaciones registradas.</p>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Turno</th>
                      <th>Desde</th>
                      <th>Hasta</th>
                    </tr>
                  </thead>
                  <tbody>
                    {historialTurnos.map((a) => (
                      <tr key={a.id}>
                        <td data-label="Turno">{a.turnoNombre}</td>
                        <td data-label="Desde">{a.fechaDesde}</td>
                        <td data-label="Hasta">
                          {a.fechaHasta ? (
                            a.fechaHasta
                          ) : (
                            <span className="badge-activo-turno">Vigente</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}
      </div>
    </>
  );
}

export default EmpleadoForm;
