package br.com.projeto_qualidade_software.projeto.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.seed.enabled=false"
)
@Testcontainers(disabledWithoutDocker = true)
class AplicacaoBancoLimpoPostgresTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    @DisplayName("CP-158 / RNF-ISO-08 - inicia API com PostgreSQL sem schema prévio")
    void iniciaAplicacaoEAplicaMigracoesEmBancoVazio() {
        assertEquals(
                HttpStatus.OK,
                restTemplate.getForEntity("/api/auth/csrf", String.class).getStatusCode()
        );
        assertTrue(tabelaExiste("reservas"));
        assertTrue(tabelaExiste("alteracoes_horario_aplicadas"));
        assertEquals(1, colunaExiste("materiais", "restrito"));
    }

    private boolean tabelaExiste(String nome) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL",
                Boolean.class,
                "public." + nome
        ));
    }

    private int colunaExiste(String tabela, String coluna) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tabela, coluna);
    }
}
