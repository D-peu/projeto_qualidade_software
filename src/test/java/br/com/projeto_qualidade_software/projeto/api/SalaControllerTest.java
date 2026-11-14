package br.com.projeto_qualidade_software.projeto.api;

import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.services.SalaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SalaController.class, properties = "app.auth.enabled=false")
class SalaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalaService salaService;

    @Test
    @DisplayName("CP-89 / RF05 - Lista salas com os campos do contrato e sem dados internos")
    void listarSalasRetornaCamposPublicosDeCadaSala() throws Exception {
        when(salaService.listarSalas(null, null)).thenReturn(List.of(
                new Sala(101L, 30, StatusSala.DISPONIVEL),
                new Sala(204L, 24, StatusSala.EM_MANUTENCAO),
                new Sala(305L, 40, StatusSala.RESERVADA)
        ));

        mockMvc.perform(get("/api/salas"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"id": 101, "capacidade": 30, "statusSala": "DISPONIVEL"},
                          {"id": 204, "capacidade": 24, "statusSala": "EM_MANUTENCAO"},
                          {"id": 305, "capacidade": 40, "statusSala": "RESERVADA"}
                        ]
                        """, true));

        verify(salaService).listarSalas(null, null);
    }

    @Test
    @DisplayName("CP-90 / RF05 - Lista vazia quando não há salas cadastradas")
    void listarSalasRetornaListaVaziaQuandoNaoHaSalas() throws Exception {
        when(salaService.listarSalas(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/salas"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));

        verify(salaService).listarSalas(null, null);
    }

    @Test
    @DisplayName("CP-150 / RF05 - Filtra salas por estado e capacidade mínima")
    void listarSalasAplicaFiltrosRecebidos() throws Exception {
        when(salaService.listarSalas(StatusSala.EM_MANUTENCAO, 30))
                .thenReturn(List.of(new Sala(204L, 40, StatusSala.EM_MANUTENCAO)));

        mockMvc.perform(get("/api/salas")
                        .param("status", "EM_MANUTENCAO")
                        .param("capacidadeMinima", "30"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":204,"capacidade":40,"statusSala":"EM_MANUTENCAO"}]
                        """, true));

        verify(salaService).listarSalas(StatusSala.EM_MANUTENCAO, 30);
    }

    @Test
    @DisplayName("CP-120 / RF03 / RF02 - Cria sala e retorna identidade gerada e estado inicial")
    void criarSalaRetornaDadosCriados() throws Exception {
        when(salaService.criarSala(20))
                .thenReturn(new Sala(401L, 20, StatusSala.DISPONIVEL));

        mockMvc.perform(post("/api/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"capacidade": 20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(401))
                .andExpect(jsonPath("$.capacidade").value(20))
                .andExpect(jsonPath("$.statusSala").value("DISPONIVEL"));

        verify(salaService).criarSala(20);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"capacidade\":19}", "{\"capacidade\":null}"})
    @DisplayName("CP-121 / RF03 / INV-18 - Rejeita capacidade abaixo do mínimo ou ausente")
    void criarSalaRejeitaCapacidadeInvalida(String body) throws Exception {
        mockMvc.perform(post("/api/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(salaService, never()).criarSala(any());
    }

    @Test
    @DisplayName("CP-151 / RF04 / RF11 - Administrador altera estado operacional da sala")
    void alterarStatusSalaRetornaEstadoAtualizado() throws Exception {
        when(salaService.alterarStatus(401L, StatusSala.EM_MANUTENCAO))
                .thenReturn(new Sala(401L, 20, StatusSala.EM_MANUTENCAO));

        mockMvc.perform(put("/api/salas/401/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statusSala":"EM_MANUTENCAO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(401))
                .andExpect(jsonPath("$.statusSala").value("EM_MANUTENCAO"));

        verify(salaService).alterarStatus(401L, StatusSala.EM_MANUTENCAO);
    }

    @Test
    @DisplayName("CP-159 / RF04 / RF11 / INV-18 - Rejeita estado de sala ausente")
    void alterarStatusSalaRejeitaEstadoAusente() throws Exception {
        mockMvc.perform(put("/api/salas/401/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statusSala":null}
                                """))
                .andExpect(status().isBadRequest());

        verify(salaService, never()).alterarStatus(any(), any());
    }
}
