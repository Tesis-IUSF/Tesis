![CI Status](https://github.com/Tesis-IUSF/Tesis/actions/workflows/ci.yml/badge.svg)

# 🏫 Sistema de Gestión y Portal Web Escolar

Este repositorio contiene el código fuente de una plataforma web integral diseñada para digitalizar, centralizar y optimizar los procesos administrativos y de comunicación de un plantel educativo. 

El sistema busca modernizar la institución reemplazando procesos manuales (como el uso de carpetas físicas y planillas de asistencia a bolígrafo) por flujos digitales automatizados y seguros.

---

## 🎯 Objetivos del Proyecto

- **Administración Eficiente:** Agilizar el registro de nuevos ingresos y la inscripción de estudiantes regulares.
- **Control de Personal:** Proveer un sistema digital para el marcaje de entrada/salida y la gestión de la ficha técnica de los empleados.
- **Transparencia y Análisis:** Brindar a la directiva paneles de estadísticas en tiempo real y reportes exportables.
- **Comunicación Efectiva:** Mantener informados a los representantes y visitantes a través de un blog de noticias centralizado.

---

## 🧩 Módulos Principales (Épicas)

El sistema está compuesto por los siguientes módulos principales, basados en el alcance del proyecto:

### 🔒 1. Autenticación y Seguridad (`SW-4: Login`)
Protección integral del sistema.
- Validación segura de credenciales de usuario.
- Redirección y bloqueo de rutas internas para usuarios no autenticados.

### 🎓 2. Gestión de Matrícula y Censo (`SW-3: Matricula`)
Digitalización de cupos y expedientes.
- **Censo y Nuevos Ingresos:** Registro de aspirantes validando duplicidad de cédulas y disponibilidad de cupos en el grado solicitado.
- **Inscripciones:** Formalización de inscripciones (regulares y nuevos) con vinculación automática de datos.
- **Checklist de Requisitos:** Control digital de documentos entregados (examen cardiovascular, fotos, expediente físico).

### ⏱️ 3. Asistencia y Directorio de Personal (`SW-2: Asistencia`)
Sustitución de las planillas impresas tradicionales.
- **Directorio:** Gestión de fichas técnicas de empleados (cédula, nombre, cargo: obrero/cocina/docente/vigilante/administrativo y turno).
- **Control de Asistencia:** Marcaje digital diario de entrada y salida protegido con la hora exacta del servidor, evitando marcajes duplicados y permitiendo agregar observaciones por incidencias.

#### Carnet QR de empleados

- `POST /api/empleados/{id}/carnet` emite un carnet PDF tamaño tarjeta con un QR firmado. Solo los roles `Administrador` y `Director` pueden emitirlo.
- `POST /api/asistencias/qr` recibe `{"qrToken":"..."}` y solo admite usuarios con rol `Escaner`. El token no contiene datos personales, tiene vigencia predeterminada de 365 días y se revoca al emitir un carnet nuevo.
- `GET /api/asistencias/hoy` devuelve empleados con asistencia registrada hoy, sus horas de entrada/salida y duración trabajada en minutos y formato `H:mm`. Si aún no han marcado salida, la duración se calcula hasta la hora actual del servidor.
- `GET /api/asistencias/historico?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` consulta el histórico. Admite filtros opcionales `empleadoId`, `departamentoId`, `turnoId` y `estado` (por ejemplo, `tardanza`); el rango máximo es de 366 días.
- `GET /api/asistencias/ausencias?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` busca faltas en fechas pasadas o hasta hoy, con filtros opcionales `empleadoId`, `departamentoId` y `turnoId`. Solo considera empleados activos con asignación vigente cuyo turno aplique ese día y sin registro de asistencia; días futuros y días libres no se cuentan.
- La vigencia se configura con `app.qr.expiration-days`; el nombre impreso de la institución con `app.institucion.nombre`.
- Antes de desplegar en producción, aplicar una vez `backend/demo/src/main/resources/db/manual/V20260929__create_credenciales_qr.sql`; el perfil de producción valida el esquema y no crea tablas.

El QR impreso es estático: una fotografía del carnet contiene el mismo token y no puede distinguirse criptográficamente del carnet original. La firma evita crear o modificar credenciales, y la revocación invalida carnets reemplazados; para verificar que la persona presente sea el titular hace falta un control adicional en el puesto de escaneo.

### 📢 4. Blog y Portal Público (`SW-5: Blog`)
Centro de información oficial del plantel hacia la comunidad.
- **Tablón de Anuncios:** Publicación de noticias, eventos y avisos urgentes organizados por fecha.
- **Acceso Libre:** Los representantes pueden leer comunicados y descargar planillas sin necesidad de iniciar sesión.
- **Comunicación Unidireccional:** Emisión de información oficial sin distracciones o foros innecesarios.

### 📊 5. Reportes y Estadísticas (`SW-55: Reportes`)
Herramientas analíticas para la toma de decisiones directivas.
- **Panel de Matrícula (Tiempo Real):** Métricas de cupos restantes por nivel y sección, desglosado por sexo y estatus de inscripción.
- **Reportes de Asistencia:** Consolidado de inasistencias, llegadas tardías y observaciones, filtrable por rango de fechas, cargo y turno.
- **Exportación:** Generación de reportes listos para imprimir o compartir en formato PDF o Excel.

---

## 👥 Roles de Usuario

El sistema adapta sus funciones dependiendo del rol del usuario que inicie sesión:

1. **Director:** Acceso integral a los paneles de estadísticas (matrícula) y exportación de reportes consolidados (asistencia).
2. **Administrativo:** Gestión de inscripciones, control de cupos, y administración de la ficha técnica del personal.
3. **Coordinador:** Encargado de la redacción y publicación de comunicados oficiales en el blog.
4. **Empleado:** Acceso al sistema de marcaje diario de asistencia.
5. **Visitante / Representante:** Usuario sin sesión iniciada que consume el blog de noticias e inicia procesos de censo público.

---

## 🛠️ Tecnologías y Configuración (`SW-1: Infraestructura`)

*(Nota: Sustituye o completa esta sección con las tecnologías que estés usando en tu stack)*

- **Frontend:** [Ej: React / Vue / Angular]
- **Backend:** [Ej: Node.js / Python / PHP]
- **Base de Datos:** [Ej: PostgreSQL / MySQL / MongoDB]

### Instalación Local

1. Clona el repositorio:
   ```bash
   git clone [https://github.com/tu-usuario/tu-repo.git](https://github.com/tu-usuario/tu-repo.git)```

2. Crea la configuración local desde la plantilla y ajusta sus valores:
   ```bash
   cp .env.example .env
   ```
   El contrato único para la base de datos es `DB_NAME`, `DB_USER`, `DB_PASS` y `DB_ROOT_PASSWORD`. En desarrollo, `DB_PORT` es el puerto publicado en el host; el backend dentro de Compose se conecta a `db:3306`. No uses `MYSQL_DATABASE` ni `DB_PASSWORD` en `.env`: `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD` son variables internas que Compose asigna al contenedor oficial de MySQL.

   Cambia las contraseñas de ejemplo antes de usar el entorno. Para producción usa credenciales fuertes y valores reales para `DOCKER_REGISTRY`, `IMAGE_TAG` y `VITE_API_URL`. Los contenedores MySQL solo aplican estas credenciales al inicializar un volumen vacío; si ya existe un volumen de base de datos, cambiar `.env` no cambia los usuarios ni contraseñas almacenados en MySQL.

### Primer administrador y datos iniciales

Al iniciar, el backend crea de forma idempotente los roles base (`ADMINISTRADOR`, `DIRECTOR`, `ADMINISTRATIVO` y `ESCANER`). No crea catálogos escolares ni datos del plantel, porque dependen de cada institución.

Antes de iniciar el backend, configura `INITIAL_ADMIN_SETUP_TOKEN` en `.env` con un secreto aleatorio de al menos 32 caracteres. Puedes generarlo con:

```bash
openssl rand -hex 32
```

Cuando la base de datos esté disponible, consulta si todavía no hay usuarios y crea la primera cuenta:

```bash
export INITIAL_ADMIN_SETUP_TOKEN='<el mismo valor guardado en .env>'

curl http://localhost:8080/api/setup/status

curl -X POST http://localhost:8080/api/setup/initial-admin \
  -H "Content-Type: application/json" \
  -H "X-Initial-Admin-Token: $INITIAL_ADMIN_SETUP_TOKEN" \
  -d '{"nombreUsuario":"admin","email":"admin@ejemplo.com","password":"cambia-esta-clave-segura"}'
```

La contraseña debe tener entre 12 y 72 caracteres. El endpoint solo funciona mientras no exista ningún usuario, devuelve `409` al completarse la configuración y requiere el token secreto; conserva ese token fuera del repositorio y elimínalo de `.env` después de crear el administrador. Luego inicia sesión en `/api/auth/login`. El antiguo registro público `/api/auth/registrar` está deshabilitado.

### Despliegue en producción

En el servidor, instala Docker Engine y Docker Compose v2, clona o actualiza el repositorio y configura `.env` con valores reales de producción. No uses las credenciales de ejemplo. Si las imágenes del registro son privadas, inicia sesión antes del despliegue con `docker login`.

Desde la raíz del proyecto, ejecuta:

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

El script verifica `.env`, valida el archivo Compose, descarga las imágenes `backend` y `frontend` usando `DOCKER_REGISTRY` e `IMAGE_TAG` configurados, actualiza los servicios y espera hasta 180 segundos por sus healthchecks. No ejecuta `docker compose down`, por lo que evita la interrupción innecesaria que causaba la versión anterior y conserva los volúmenes persistentes de MySQL y de imágenes. Al finalizar, muestra el estado de los servicios.

Si falla, el script informa cómo revisar el estado y los últimos logs de backend/frontend. El despliegue no revierte automáticamente imágenes si un servicio queda unhealthy: revisa los logs antes de volver a intentarlo. Tampoco aplica cambios de esquema de base de datos; cualquier DDL requerido debe respaldarse y aplicarse siguiendo sus instrucciones específicas antes de desplegar una versión que lo necesite.

Para que un despliegue sea repetible, es preferible establecer `IMAGE_TAG` con una etiqueta inmutable publicada por CI (por ejemplo, la etiqueta con el SHA del commit) en lugar de `latest`. El mismo valor se usa para backend y frontend; confirma que ambas imágenes se publicaron con esa etiqueta.

### Fotos de estudiantes, representantes y empleados

`POST /api/fotos` recibe una imagen multipart en el campo `archivo` y devuelve una URL autenticada. Solo admite imágenes JPG, PNG o WebP verificadas por su contenido real, con un máximo de 2 MB. Para mostrar una imagen desde el frontend, solicítala con Axios usando `responseType: "blob"`; una etiqueta `<img>` no adjunta el token JWT.

Las imágenes se guardan en `app.uploads.dir` (configurable mediante `UPLOADS_DIR`). Los entornos Docker montan un volumen persistente distinto para desarrollo y producción en `/app/uploads`.

Para las imágenes de comunicados, el frontend puede subir el archivo con `POST /api/fotos` y enviar el `url` devuelto en el campo `imagenUrl` al crear o editar el comunicado (`POST`/`PUT /api/blog/admin/comunicados`). El contenido HTML de `contenidoHtml` se limpia en escritura y también antes de incluirlo en respuestas: conserva formato básico, listas, encabezados, tablas y enlaces HTTP(S)/mailto; elimina scripts, eventos, imágenes embebidas y otros elementos/atributos no permitidos. Las imágenes se muestran por separado con `imagenUrl`.

Antes de activar el perfil de producción, añadir la columna de foto al personal si todavía no existe:

```sql
ALTER TABLE personal
    ADD COLUMN foto_url VARCHAR(500) NULL;
```

La entidad de representantes también usa `foto_url`; si la base de producción aún no tiene esa columna, aplicar:

```sql
ALTER TABLE representantes
    ADD COLUMN foto_url VARCHAR(500) NULL;
```

### Configuración institucional

Los perfiles `dev` y `prod` definen `app.institucion.nombre`, `director`, `rif`, `codigo`, `direccion`, `telefono` y `correo`, con valores provistos mediante las variables de entorno `INSTITUCION_NOMBRE`, `INSTITUCION_DIRECTOR`, `INSTITUCION_RIF`, `INSTITUCION_CODIGO`, `INSTITUCION_DIRECCION`, `INSTITUCION_TELEFONO` e `INSTITUCION_CORREO`. El nombre tiene como valor predeterminado el nombre configurado para el plantel; los demás datos deben completarse con los valores oficiales en `.env`/el entorno de despliegue. Docker Compose de producción reenvía esas variables al backend. No se inventaron los datos del director, RIF ni dirección.

Actualmente los PDFs usan `app.institucion.nombre`; los demás valores quedan disponibles como configuración para plantillas institucionales. Los escudos/logos existentes están en `src/main/resources/constancias/`.

### Filtros de listados de personal

- `GET /api/empleados` admite `cargoId`, `turnoId` y `activo` (`true` para personal activo, `false` para personal dado de baja), además de paginación. Si `activo` se omite, devuelve ambos estados. `turnoId` filtra asignaciones activas y vigentes para la fecha actual.
- La cédula de empleado se acepta como `V-` seguido de 7 u 8 dígitos desde 6.000.000, o `E-` seguido de 8 dígitos desde 80.000.000. El backend quita espacios al inicio/final y convierte a mayúsculas antes de comprobar duplicados y guardar; por ejemplo, ` v-12345678 ` se almacena como `V-12345678`. Formatos sin prefijo o fuera de rango producen HTTP 400 y duplicados HTTP 409.
- `POST /api/empleados` requiere `turnoId` y `fechaIngreso`; el turno debe estar activo. Al guardar el empleado, el backend crea su asignación en la misma transacción, con `fechaDesde = fechaIngreso` y `fechaHasta = null`.
- `DELETE /api/empleados/{id}` realiza una baja lógica: marca `activo = false`, establece `fechaEgreso` si no estaba definida, cierra en esa fecha las asignaciones vigentes y revoca las credenciales QR activas. En `PUT /api/empleados/{id}`, enviar `activo = false` aplica la misma baja. El registro del empleado y su historial se conservan.
- `GET /api/empleados/carnets` admite también `turnoId` junto con sus filtros existentes `q`, `departamentoId`, `cargoId` y `estadoQr`.
- `GET /api/asistencias/hoy` y `GET /api/asistencias/historico` aceptan `cargoId` y `turnoId`; el histórico mantiene también sus filtros de empleado y departamento.
- `GET /api/asistencias/ausencias` admite `cargoId` además de los filtros existentes de empleado, departamento y turno.

### Gestión de años y períodos de matrícula

La administración del calendario está disponible bajo `/api/matriculas/gestion/**` y requiere rol `ADMINISTRADOR` o `DIRECTOR`.

- `GET /api/matriculas/gestion/anios`: lista años y sus períodos.
- `POST /api/matriculas/gestion/anios`: crea un año en preparación con `anio`, `nombre`, `fechaInicio` y `fechaFin`. Si hay un año anterior con períodos activos, copia esas ventanas desplazando las fechas la diferencia de años. Si no hay calendario previo, los períodos se agregan manualmente.
- `GET` y `PUT /api/matriculas/gestion/anios/{anio}`: consulta y edita nombre y fechas; el número del año es inmutable.
- `POST /api/matriculas/gestion/anios/{anio}/periodos`: agrega un período con `tipo`, `fechaInicio` y `fechaFin`.
- `PUT /api/matriculas/gestion/periodos/{id}`: corrige fechas o tipo. `PATCH .../{id}/activar` y `PATCH .../{id}/desactivar` habilitan o conservan una ventana como histórica.
- `POST /api/matriculas/gestion/anios/{anio}/activar`: activa el año si tiene al menos un período activo de preinscripción, inscripción y extemporánea. Al cambiar de año activo, cierra las ventanas de preinscripción e inscripción del anterior; conserva su extemporánea.
- `DELETE /api/matriculas/gestion/periodos/{id}` elimina permanentemente solo períodos no usados de un año futuro en preparación. En los demás casos hay que desactivarlos.

Se permiten solapamientos entre tipos diferentes, pero no entre ventanas del mismo tipo dentro de un año. La preinscripción nueva solo admite el año activo; los trámites anteriores en estado `en_proceso` pueden continuar durante una ventana extemporánea. El endpoint de calendario público-interno informa el año y las ventanas activas.

### Corrección de datos de matrícula

- `GET /api/matriculas/{id}/preinscripcion` devuelve el cuerpo de `PreinscripcionRequestDTO` para rellenar el formulario existente.
- `PUT /api/matriculas/{id}/preinscripcion` actualiza estudiante, representante, relación principal, institución de procedencia y sección. Solo admite matrículas `preinscrito` o `en_proceso`; este último estado se conserva porque representa una matrícula ya iniciada con requisitos pendientes.
- El cambio valida cédula duplicada, sección activa y del mismo año, cupo disponible y edad para el grado elegido. La matrícula no puede cambiar de año escolar.
- Si cambia el nivel o el tipo de ingreso, se sincroniza el checklist: los requisitos que siguen aplicando conservan su avance, los nuevos se agregan pendientes y los que dejan de aplicar se quitan.
- Las respuestas de los listados de matrícula incluyen `estudianteCedula`.
- `GET /api/matriculas/listado` lista todos los estados con búsqueda opcional `q`, paginación (`page`, `size`) y filtro opcional `estado`. Un `estado` vacío equivale a no filtrar; los valores aceptados son `preinscrito`, `en_proceso`, `completada`, `retirada` y `anulada`.
- `en_proceso` se asigna al iniciar el checklist o actualizar un requisito; puede mantenerse mientras falten requisitos obligatorios. La formalización solo se permite cuando todos los obligatorios aplicables están cumplidos; los opcionales pendientes no la bloquean. Para clasificar una nueva matrícula como `regular`, basta una matrícula `en_proceso` o `completada` del año escolar inmediatamente anterior (salvo que se indique una institución de procedencia, que se clasifica como traslado).

### Retiros y cancelación de preinscripciones

`POST /api/matriculas/{id}/retiros` distingue la acción por el estado de la matrícula actual, no por el tipo de ingreso ni por el historial académico:

- Si está `preinscrito`, elimina únicamente la fila de esa matrícula pendiente. No elimina al estudiante ni al representante. Por ahora no guarda el motivo ni el solicitante como historial permanente; la respuesta devuelve `operacion: "preinscripcion_eliminada"` con HTTP 200.
- Si está `en_proceso` o `completada`, registra el retiro formal y cambia la matrícula a `retirada`; responde con `operacion: "retiro_formal"` y HTTP 201.
- Las continuidades generadas automáticamente también nacen como `preinscrito`, pero se distinguen por su matrícula de origen. Al retirarlas, se conserva esa continuidad con estado `retirada` y se registra el retiro formal, sin bloquear por requisitos pendientes del año anterior.
- Una matrícula preinscrita con checklist o retiros anteriores asociados no se elimina; responde HTTP 409 para proteger el historial.
- El estado `en_proceso` significa que el estudiante ya está inscrito, aunque aún pueda tener requisitos pendientes.

### Continuidades y egresos de fin de año

Cuando el año de origen está cerrado (desactivado al activar el siguiente, o ya pasó su `fechaFin`), un usuario `ADMINISTRADOR` o `DIRECTOR` puede ejecutar `POST /api/matriculas/gestion/anios/{anioOrigen}/continuidades`. El año de destino inmediato debe existir. El backend prepara continuidades para los estudiantes cuya matrícula más reciente del año de origen esté `en_proceso` o `completada`; estudiantes cuya matrícula más reciente esté `retirada` quedan excluidos.

- Promueve automáticamente un grado dentro del mismo nivel. Se asigna a una sección activa del año siguiente con cupo; entre varias, se elige la de mayor disponibilidad y se va balanceando la distribución.
- Las continuidades se crean como `preinscrito`, tipo de ingreso `regular`, ligadas a su matrícula de origen y ocupan cupo. Se pueden corregir de grado/sección con `PUT /api/matriculas/{id}/preinscripcion` antes de formalizar.
- Si no existe el grado siguiente o no hay sección/cupo, no se excede la capacidad: el estudiante se devuelve en `asignacionPendiente`. Al corregir la configuración, se puede volver a ejecutar la operación; no duplica continuidades ya creadas.
- Los estudiantes del último grado activo quedan con una resolución pendiente; estar en ese grado no los marca automáticamente como egresados. La lista está en `GET /api/matriculas/gestion/anios/{anioOrigen}/egresos-pendientes`. Administración o Dirección puede resolver cada caso como `egresar` o `repetir` mediante `PATCH /api/matriculas/gestion/egresos/{egresoId}/resolver` con `{ "decision": "egresar" }` o `{ "decision": "repetir", "seccionId": 123 }`. Repetir crea una preinscripción en el año siguiente y el mismo grado; si no se indica sección, el backend elige una sección activa con cupo. Ambas decisiones guardan quién resolvió y cuándo. El sistema no genera títulos.
- Para una base donde aún no existen las continuidades ni los egresos pendientes, aplicar una sola vez `backend/demo/src/main/resources/db/manual/V20261024__continuidades_y_egresos_pendientes.sql`. Si ya se aplicó la versión anterior de ese DDL que creó `egresos_pendientes` con las columnas `confirmado`, `confirmado_por` y `confirmado_en`, no volver a ejecutar el `CREATE TABLE`: aplicar una sola vez `backend/demo/src/main/resources/db/manual/V20261024_1__resolver_egresos_pendientes.sql`, que conserva las confirmaciones existentes y actualiza la tabla. Hacer respaldo antes; producción usa `ddl-auto=validate`.
