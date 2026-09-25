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
   git clone [https://github.com/tu-usuario/tu-repo.git](https://github.com/tu-usuario/tu-repo.git)