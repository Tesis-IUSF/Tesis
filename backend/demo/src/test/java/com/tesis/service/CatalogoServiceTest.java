package com.tesis.service;

import com.tesis.entity.Departamento;
import com.tesis.entity.NivelEducativo;
import com.tesis.repository.CargoRepository;
import com.tesis.repository.DepartamentoRepository;
import com.tesis.repository.GradoRepository;
import com.tesis.repository.NivelEducativoRepository;
import com.tesis.repository.RequisitoMatriculaRepository;
import com.tesis.repository.RolesRepository;
import com.tesis.repository.SeccionRepository;
import com.tesis.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private RolesRepository rolesRepository;
    @Mock
    private NivelEducativoRepository nivelEducativoRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private SeccionRepository seccionRepository;
    @Mock
    private RequisitoMatriculaRepository requisitoMatriculaRepository;

    private CatalogoService catalogoService;

    @BeforeEach
    void setUp() {
        catalogoService = new CatalogoService(
                departamentoRepository,
                cargoRepository,
                turnoRepository,
                rolesRepository,
                nivelEducativoRepository,
                gradoRepository,
                seccionRepository,
                requisitoMatriculaRepository
        );
    }

    @Test
    void crearDepartamentoGuardaLaEntidad() {
        Departamento departamento = new Departamento();
        departamento.setNombre("Administración");
        when(departamentoRepository.save(any(Departamento.class))).thenAnswer(invocation -> {
            Departamento value = invocation.getArgument(0);
            value.setId(1);
            return value;
        });

        Departamento resultado = catalogoService.crearDepartamento(departamento);

        assertNotNull(resultado.getId());
        assertEquals("Administración", resultado.getNombre());
    }

    @Test
    void listarNivelesEducativosRetornaListado() {
        NivelEducativo nivel = new NivelEducativo();
        nivel.setNombre("Primaria");
        when(nivelEducativoRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(nivel), PageRequest.of(0, 25), 1));

        List<NivelEducativo> resultado = catalogoService
            .listarNivelesEducativos(PageRequest.of(0, 25)).getContent();

        assertEquals(1, resultado.size());
        assertEquals("Primaria", resultado.get(0).getNombre());
    }
}
