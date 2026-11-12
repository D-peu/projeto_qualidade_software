package br.com.projeto_qualidade_software.projeto.api;

import br.com.projeto_qualidade_software.projeto.entities.Material;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CatalogoRecursosController.class,
        properties = "app.auth.enabled=false"
)
class CatalogoRecursosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitanteRepository solicitanteRepository;

    @MockitoBean
    private MaterialRepository materialRepository;

    @Test
    void listarProfessoresRetornaSomenteIdENome() throws Exception {
        Solicitante professor = new Solicitante(
                7L,
                "Professora de teste",
                "privado@teste.local",
                "nao-retornar",
                TipoSolicitante.PROFESSOR,
                LocalDate.now(),
                LocalDate.now()
        );
        when(solicitanteRepository.findByTipoSolicitanteOrderByNameAsc(
                TipoSolicitante.PROFESSOR
        )).thenReturn(List.of(professor));

        mockMvc.perform(get("/api/professores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nome").value("Professora de teste"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());

        verify(solicitanteRepository).findByTipoSolicitanteOrderByNameAsc(
                TipoSolicitante.PROFESSOR
        );
    }

    @Test
    void listarMateriaisRetornaCatalogoOrdenadoPeloRepositorio() throws Exception {
        when(materialRepository.findAllByOrderByNomeAsc())
                .thenReturn(List.of(new Material(12L, "Projetor multimídia")));

        mockMvc.perform(get("/api/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].nome").value("Projetor multimídia"));

        verify(materialRepository).findAllByOrderByNomeAsc();
    }
}
