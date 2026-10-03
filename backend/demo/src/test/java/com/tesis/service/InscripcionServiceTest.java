package com.tesis.service;

import com.tesis.dto.InscripcionDTO.ChecklistUpdateRequestDTO;
import com.tesis.dto.InscripcionDTO.FormalizarInscripcionRequestDTO;
import com.tesis.dto.InscripcionDTO.InscripcionResponseDTO;
import com.tesis.entity.Estudiante;
import com.tesis.entity.Grado;
import com.tesis.entity.Matricula;
import com.tesis.entity.NivelEducativo;
import com.tesis.entity.AnioEscolar;
import com.tesis.entity.PeriodoMatricula;
import com.tesis.entity.RequisitoMatricula;
import com.tesis.entity.Roles;
import com.tesis.entity.Seccion;
import com.tesis.entity.TipoPeriodoMatricula;
import com.tesis.entity.Usuario;
import com.tesis.repository.ChecklistMatriculaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InscripcionServiceTest {

    @Autowired
    private InscripcionService inscripcionService;

    @Autowired
    private ChecklistMatriculaRepository checklistRepository;

    @Autowired
    private EntityManager entityManager;

        @Autowired
        private Clock clock;

    @Test
    void inicializaChecklistYFormalizaCuandoTodosLosObligatoriosEstanCumplidos() {
                crearVerificador();
        NivelEducativo nivel = crearNivel();
        RequisitoMatricula requisitoNivelObligatorio = crearRequisito(
                "Partida de nacimiento", true, nivel);
        RequisitoMatricula requisitoGlobalObligatorio = crearRequisito(
                "Foto", true, null);
        crearRequisito("Boletín de nuevo ingreso", true, nivel, "nuevo_ingreso");
        crearRequisito("Autorización opcional", false, nivel);
        Matricula matricula = crearMatricula(nivel);

        InscripcionResponseDTO checklistInicial = inscripcionService
                .prepararChecklist(matricula.getId());
        assertEquals("en_proceso", checklistInicial.getEstadoMatricula());
        assertEquals(3, checklistInicial.getChecklist().size());
        assertEquals(3, checklistRepository.findByMatricula_Id(matricula.getId()).size());

        inscripcionService.prepararChecklist(matricula.getId());
        assertEquals(3, checklistRepository.findByMatricula_Id(matricula.getId()).size());

        ResponseStatusException pendiente = assertThrows(ResponseStatusException.class,
                () -> inscripcionService.formalizar(
                        matricula.getId(), new FormalizarInscripcionRequestDTO("CONST-01", null)));
        assertEquals(HttpStatus.CONFLICT, pendiente.getStatusCode());

        marcarCumplido(matricula.getId(), requisitoNivelObligatorio.getId());
        ResponseStatusException faltaGlobal = assertThrows(ResponseStatusException.class,
                () -> inscripcionService.formalizar(
                        matricula.getId(), new FormalizarInscripcionRequestDTO("CONST-01", null)));
        assertEquals(HttpStatus.CONFLICT, faltaGlobal.getStatusCode());

        marcarCumplido(matricula.getId(), requisitoGlobalObligatorio.getId());
        InscripcionResponseDTO formalizada = inscripcionService.formalizar(
                matricula.getId(), new FormalizarInscripcionRequestDTO("CONST-01", null));

        assertEquals("completada", formalizada.getEstadoMatricula());
        assertEquals("CONST-01", formalizada.getNumeroConstancia());
        assertNotNull(formalizada.getFechaFormalizacion());
        assertEquals(2, formalizada.getRequisitosObligatoriosCumplidos());
        assertEquals(2, formalizada.getRequisitosObligatorios());
        assertNotNull(formalizada.getChecklist().stream()
                .filter(item -> requisitoNivelObligatorio.getId().equals(item.getRequisitoId()))
                .findFirst().orElseThrow().getVerificadoPorId());
        assertFalse(formalizada.getChecklist().stream()
                .filter(item -> "Autorización opcional".equals(item.getNombreRequisito()))
                .findFirst().orElseThrow().getCumplido());
    }

    @Test
    void permiteConsultarSeguimientoAunqueElPeriodoEsteCerrado() {
        Matricula matricula = crearMatricula(crearNivel());
        entityManager.createQuery("update PeriodoMatricula p set p.activo = false "
                        + "where p.anioEscolar = :anio and p.tipo = :tipo")
                .setParameter("anio", (short) 2026)
                .setParameter("tipo", TipoPeriodoMatricula.inscripcion)
                .executeUpdate();

        assertEquals("preinscrito", inscripcionService.obtenerSeguimiento(matricula.getId())
                .getEstadoMatricula());
    }

        @Test
        void generaChecklistSoloConRequisitosAplicablesAlTipoDeIngreso() {
                NivelEducativo nivel = crearNivel();
                NivelEducativo otroNivel = new NivelEducativo();
                otroNivel.setNombre("Nivel distinto para aplicabilidad");
                otroNivel.setOrdinal((short) 112);
                entityManager.persist(otroNivel);
                crearRequisito("Requisito regular", true, nivel, "regular");
                crearRequisito("Requisito de nuevo ingreso", true, nivel, "nuevo_ingreso");
                crearRequisito("Requisito de otro nivel", true, otroNivel, "regular");
                Matricula matricula = crearMatricula(nivel);

                InscripcionResponseDTO response = inscripcionService.prepararChecklist(matricula.getId());

                assertEquals(1, response.getChecklist().size());
                assertEquals("Requisito regular", response.getChecklist().getFirst().getNombreRequisito());
        }

    private void marcarCumplido(Integer matriculaId, Integer requisitoId) {
        ChecklistUpdateRequestDTO request = new ChecklistUpdateRequestDTO(
                                true, "Verificado", null);
                inscripcionService.actualizarRequisito(matriculaId, requisitoId, request, "verificador@test.local");
    }

        private void crearVerificador() {
                Roles rol = new Roles();
                rol.setNombreRol("Administrador");
                entityManager.persist(rol);

                Usuario usuario = new Usuario();
                usuario.setNombreUsuario("verificador");
                usuario.setEmail("verificador@test.local");
                usuario.setPasswordHash("hash");
                usuario.setRol(rol);
                entityManager.persist(usuario);
                entityManager.flush();
        }

    private NivelEducativo crearNivel() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Nivel inscripción test");
        nivel.setOrdinal((short) 111);
        entityManager.persist(nivel);
        return nivel;
    }

    private RequisitoMatricula crearRequisito(String nombre,
                                               boolean obligatorio,
                                               NivelEducativo nivel) {
                return crearRequisito(nombre, obligatorio, nivel, null);
        }

        private RequisitoMatricula crearRequisito(String nombre,
                                                                                           boolean obligatorio,
                                                                                           NivelEducativo nivel,
                                                                                           String tiposAplicables) {
        RequisitoMatricula requisito = new RequisitoMatricula();
        requisito.setNombre(nombre);
        requisito.setObligatorio(obligatorio);
        requisito.setNivelEducativo(nivel);
                requisito.setAplicaTipoIngreso(tiposAplicables);
        entityManager.persist(requisito);
        return requisito;
    }

    private Matricula crearMatricula(NivelEducativo nivel) {
                asegurarCalendario();
        Grado grado = new Grado();
        grado.setNivelEducativo(nivel);
        grado.setNumeroGrado((short) 1);
        entityManager.persist(grado);

        Seccion seccion = new Seccion();
        seccion.setGrado(grado);
        seccion.setLetraSeccion("I");
        seccion.setAnioEscolar((short) 2026);
        entityManager.persist(seccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setCedula("INSCRIPCION-TEST-01");
        estudiante.setNombre("Estudiante");
        estudiante.setApellido("Inscripción");
        estudiante.setFechaNacimiento(LocalDate.of(2018, 1, 1));
        estudiante.setSexo("F");
        entityManager.persist(estudiante);

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setSeccion(seccion);
        matricula.setAnioEscolar((short) 2026);
        matricula.setFechaSolicitud(LocalDate.now());
        matricula.setEstadoMatricula("preinscrito");
        entityManager.persist(matricula);
        entityManager.flush();
        return matricula;
    }

        private void asegurarCalendario() {
                boolean anioExiste = !entityManager.createQuery(
                                                "select a.id from AnioEscolar a where a.anio = :anio", Integer.class)
                                .setParameter("anio", (short) 2026)
                                .getResultList().isEmpty();
                if (!anioExiste) {
                        AnioEscolar anio = new AnioEscolar();
                        anio.setAnio((short) 2026);
                        anio.setNombre("2026-2027");
                        anio.setFechaInicio(LocalDate.of(2026, 9, 9));
                        anio.setFechaFin(LocalDate.of(2027, 7, 30));
                        anio.setActivo(true);
                        entityManager.persist(anio);
                }

                boolean periodoExiste = !entityManager.createQuery(
                                                "select p.id from PeriodoMatricula p where p.anioEscolar = :anio "
                                                                + "and p.tipo = :tipo", Integer.class)
                                .setParameter("anio", (short) 2026)
                                .setParameter("tipo", TipoPeriodoMatricula.inscripcion)
                                .getResultList().isEmpty();
                if (!periodoExiste) {
                        LocalDate hoy = LocalDate.now(clock);
                        PeriodoMatricula periodo = new PeriodoMatricula();
                        periodo.setAnioEscolar((short) 2026);
                        periodo.setTipo(TipoPeriodoMatricula.inscripcion);
                        periodo.setFechaInicio(hoy.minusYears(1));
                        periodo.setFechaFin(hoy.plusYears(1));
                        periodo.setActivo(true);
                        entityManager.persist(periodo);
                }
        }
}