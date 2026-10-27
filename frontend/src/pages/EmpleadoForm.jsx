import AnimatedSelect from "../components/AnimatedSelect";
// src/pages/EmpleadoForm.jsx
import { useState, useEffect, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../utils/api";
import { useToast } from "../context/ToastContext";
import { extractErrorMessage } from "../utils/errors";
import { useCargos, useDepartamentos, useTurnos } from "../hooks/useCatalogos";
import { subirFoto } from "../utils/fotos";
import {
  TELEFONO_VENEZOLANO,
  limpiarEntradaTelefono,
} from "../utils/validacionesEntrada";
import FotoCarnet from "../components/FotoCarnet";
import { Icon } from "@iconify/react";
import { ICONOS } from "../utils/iconos";
import "./EmpleadoForm.css";

const VACIO = {
  nombre: "",
  apellido: "",
  cedulaTipo: "",
  cedulaNumero: "",
  correo: "",
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

const hoyISO = () => {
  const fecha = new Date();
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, "0")}-${String(fecha.getDate()).padStart(2, "0")}`;
};

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const soloDigitos = (valor) => valor.replace(/\D/g, "");

const separarCedula = (valor) => {
  const cedula = String(valor || "")
    .trim()
    .toUpperCase();
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
  const [foto, setFoto] = useState(null); // File elegido; se sube al guardar
  const [fotoUrlActual, setFotoUrlActual] = useState(null); // foto ya guardada
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
        const cedula = separarCedula(data.cedula);
        setForm({
          nombre: data.nombre || "",
          apellido: data.apellido || "",
          ...cedula,
          correo: data.correo || "",
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
        setFotoUrlActual(data.fotoUrl || null);
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

    if (form.telefono && !TELEFONO_VENEZOLANO.test(form.telefono.trim())) {
      nuevosErrores.telefono = "Ingresa un teléfono venezolano válido.";
    }

    if (form.correo.trim() && !EMAIL.test(form.correo.trim())) {
      nuevosErrores.correo = "Correo no válido.";
    } else if (form.correo.trim().length > 150) {
      nuevosErrores.correo = "El correo no puede superar 150 caracteres.";
    }

    if (form.fechaNacimiento && form.fechaNacimiento > hoyISO()) {
      nuevosErrores.fechaNacimiento =
        "La fecha de nacimiento no puede ser futura.";
    }

    if (!form.fechaIngreso) {
      nuevosErrores.fechaIngreso = "La fecha de ingreso es obligatoria.";
    }

    if (esEdicion && !form.activo) {
      if (!form.fechaEgreso) {
        nuevosErrores.fechaEgreso = "La fecha de egreso es obligatoria.";
      } else if (form.fechaIngreso && form.fechaEgreso < form.fechaIngreso) {
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
      correo: form.correo.trim() || null,
      telefono: form.telefono.trim() || null,
      sexo: form.sexo || null,
      cargoId: form.cargoId ? Number(form.cargoId) : null,
      departamentoId: form.departamentoId ? Number(form.departamentoId) : null,
      fechaNacimiento: form.fechaNacimiento || null,
      fechaIngreso: form.fechaIngreso,
    };

    try {
      // Foto nueva: se sube primero; si no hay, se conserva la que ya tenía
      if (foto) {
        payload.fotoUrl = await subirFoto(foto);
      } else if (fotoUrlActual) {
        payload.fotoUrl = fotoUrlActual;
      }

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
              await api.put(`/asignaciones-turnos/empleado/${id}/vigente`, {
                turnoId: Number(form.turnoId),
                fechaDesde: fechaDesdeTurno,
              });
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
          <div className="ef-con-foto">
            <FotoCarnet
              etiqueta="Foto del empleado"
              archivo={foto}
              urlActual={fotoUrlActual}
              onChange={setFoto}
              disabled={guardando}
            />

            <div className="form-grid-2">
              <label>
                Nombre *
                <input
                  maxLength={100}
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
                  maxLength={100}
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
                  <AnimatedSelect
                    value={form.cedulaTipo}
                    onChange={(e) =>
                      actualizarCampo("cedulaTipo", e.target.value)
                    }
                    aria-label="Tipo de cédula"
                  >
                    <option value="">Tipo</option>
                    <option value="V">Venezolana (V)</option>
                    <option value="E">Extranjera (E)</option>
                  </AnimatedSelect>
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
                <input
                  type="email"
                  maxLength={150}
                  value={form.correo}
                  onChange={(e) =>
                    actualizarCampo("correo", e.target.value.trim())
                  }
                  placeholder="nombre@dominio.com"
                />
                {errores.correo && (
                  <span className="campo-error">{errores.correo}</span>
                )}
              </label>

              <label>
                Teléfono
                <input
                  type="tel"
                  inputMode="tel"
                  value={form.telefono}
                  onChange={(e) =>
                    actualizarCampo("telefono", limpiarEntradaTelefono(e.target.value))
                  }
                  maxLength={20}
                  placeholder="04121234567 o +58 4121234567"
                />
                {errores.telefono && (
                  <span className="campo-error">{errores.telefono}</span>
                )}
              </label>

              <label>
                Sexo
                <AnimatedSelect
                  value={form.sexo}
                  onChange={(e) => actualizarCampo("sexo", e.target.value)}
                >
                  <option value="">Selecciona...</option>
                  <option value="M">Masculino</option>
                  <option value="F">Femenino</option>
                  <option value="Otro">Otro</option>
                </AnimatedSelect>
              </label>

              <label>
                Fecha de nacimiento
                <input
                  type="date"
                  max={hoyISO()}
                  value={form.fechaNacimiento}
                  onChange={(e) =>
                    actualizarCampo("fechaNacimiento", e.target.value)
                  }
                />
                {errores.fechaNacimiento && (
                  <span className="campo-error">
                    {errores.fechaNacimiento}
                  </span>
                )}
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
                <AnimatedSelect
                  value={form.cargoId}
                  onChange={(e) => actualizarCampo("cargoId", e.target.value)}
                >
                  <option value="">Selecciona un cargo...</option>
                  {cargos.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nombreCargo}
                    </option>
                  ))}
                </AnimatedSelect>
                {errores.cargoId && (
                  <span className="campo-error">{errores.cargoId}</span>
                )}
              </label>

              <label>
                Departamento
                <AnimatedSelect
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
                </AnimatedSelect>
              </label>

              {esEdicion ? (
                <>
                  <label>
                    Turno asignado
                    <AnimatedSelect
                      value={form.turnoId}
                      onChange={(e) =>
                        actualizarCampo("turnoId", e.target.value)
                      }
                      disabled={!form.activo}
                    >
                      <option value="">Sin turno asignado</option>
                      {turnosDisponibles.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.nombre} {!t.activo ? "(Inactivo)" : ""}
                        </option>
                      ))}
                    </AnimatedSelect>
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
                    <AnimatedSelect
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
                    </AnimatedSelect>
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
                  <AnimatedSelect
                    value={form.turnoId}
                    onChange={(e) => actualizarCampo("turnoId", e.target.value)}
                  >
                    <option value="">Selecciona un turno activo...</option>
                    {turnosDisponibles.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.nombre}
                      </option>
                    ))}
                  </AnimatedSelect>
                  {errores.turnoId && (
                    <span className="campo-error">{errores.turnoId}</span>
                  )}
                </label>
              )}
            </div>
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
