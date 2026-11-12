package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.AuditoriaReserva;
import br.com.projeto_qualidade_software.projeto.entities.Material;
import br.com.projeto_qualidade_software.projeto.entities.Reserva;
import br.com.projeto_qualidade_software.projeto.entities.Responsavel;
import br.com.projeto_qualidade_software.projeto.entities.Sala;
import br.com.projeto_qualidade_software.projeto.entities.Solicitante;
import br.com.projeto_qualidade_software.projeto.enums.StatusReserva;
import br.com.projeto_qualidade_software.projeto.enums.StatusSala;
import br.com.projeto_qualidade_software.projeto.enums.TipoAprovadorReserva;
import br.com.projeto_qualidade_software.projeto.enums.TipoSolicitante;
import br.com.projeto_qualidade_software.projeto.exceptions.AcessoNegadoException;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.AuditoriaReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.ContextoAutenticacao;
import br.com.projeto_qualidade_software.projeto.security.PerfilUsuario;
import br.com.projeto_qualidade_software.projeto.security.UsuarioAutenticado;
import jakarta.persistence.EntityNotFoundException;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReservaServiceTest {

    private static final Long SALA_ID = 1L;
    private static final Long SOLICITANTE_ID = 2L;
    private static final Long RESERVA_ID = 3L;
    private static final Long APROVADOR_ID = 4L;
    private static final Long MATERIAL_ID = 5L;
    private static final LocalDateTime AGORA = LocalDateTime.of(2040, 1, 1, 10, 0);
    private static final LocalDateTime INICIO = LocalDateTime.of(2040, 1, 2, 10, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2040, 1, 2, 11, 0);

    private ReservaRepository reservaRepository;
    private AuditoriaReservaRepository auditoriaReservaRepository;
    private SalaRepository salaRepository;
    private SolicitanteRepository solicitanteRepository;
    private ResponsavelRepository responsavelRepository;
    private AdministradorRepository administradorRepository;
    private MaterialRepository materialRepository;
    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        reservaRepository = mock(ReservaRepository.class);
        auditoriaReservaRepository = mock(AuditoriaReservaRepository.class);
        salaRepository = mock(SalaRepository.class);
        solicitanteRepository = mock(SolicitanteRepository.class);
        responsavelRepository = mock(ResponsavelRepository.class);
        administradorRepository = mock(AdministradorRepository.class);
        materialRepository = mock(MaterialRepository.class);
        reservaService = new ReservaService(
                reservaRepository,
                auditoriaReservaRepository,
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository,
                materialRepository,
                Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    @AfterEach
    void limparContextoAutenticacao() {
        ContextoAutenticacao.limpar();
    }

    @Test
    @DisplayName("CB-60 / RF12 - Lista reservas solicitadas ordenadas pelo repository")
    void listarReservasPendentesUsaConsultaOrdenadaPorDataSolicitacao() {
        List<Reserva> pendentes = List.of(new Reserva(), new Reserva());
        when(reservaRepository.findByStatusOrderBySolicitadoEmAsc(
                StatusReserva.SOLICITADA
        )).thenReturn(pendentes);

        assertEquals(pendentes, reservaService.listarReservasPendentes());

        verify(reservaRepository).findByStatusOrderBySolicitadoEmAsc(
                StatusReserva.SOLICITADA
        );
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void buscarReservaExistenteRetornaReservaEncontrada() {
        Reserva reserva = new Reserva();
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));

        assertSame(reserva, reservaService.buscarReserva(RESERVA_ID));

        verify(reservaRepository).findById(RESERVA_ID);
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void buscarReservaInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.buscarReserva(RESERVA_ID)
        );

        verify(reservaRepository).findById(RESERVA_ID);
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void listarReservasDoSolicitanteComResultadosRetornaReservasDoRepositorio() {
        List<Reserva> reservas = List.of(new Reserva(), new Reserva());
        when(reservaRepository.findBySolicitanteIdOrderByInicioDesc(SOLICITANTE_ID))
                .thenReturn(reservas);

        assertSame(reservas, reservaService.listarReservasDoSolicitante(SOLICITANTE_ID));

        verify(reservaRepository).findBySolicitanteIdOrderByInicioDesc(SOLICITANTE_ID);
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void listarReservasDoSolicitanteSemResultadosRetornaListaVazia() {
        when(reservaRepository.findBySolicitanteIdOrderByInicioDesc(SOLICITANTE_ID))
                .thenReturn(List.of());

        assertEquals(
                List.of(),
                reservaService.listarReservasDoSolicitante(SOLICITANTE_ID)
        );

        verify(reservaRepository).findBySolicitanteIdOrderByInicioDesc(SOLICITANTE_ID);
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void solicitarReservaComInicioEFimIguaisLancaIllegalArgumentException() {
        LocalDateTime inicioEFim = INICIO;

        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        1L,
                        1L,
                        TipoSolicitante.PROFESSOR,
                        inicioEFim,
                        inicioEFim
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @Test
    void solicitarReservaComInicioNuloLancaIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, null, FIM
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @Test
    void solicitarReservaComFimNuloLancaIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, INICIO, null
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @Test
    void solicitarReservaComFimAnteriorAoInicioLancaIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, FIM, INICIO
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-101–113 / RF07 / RN01 - rejeita intervalos inválidos na criação")
    @MethodSource("intervalosInvalidosRF07")
    void solicitarReservaRejeitaIntervalosInvalidosRF07(
            String caso,
            LocalDateTime inicio,
            LocalDateTime fim,
            String mensagem
    ) {
        prepararSolicitacaoSemConflito(inicio, fim);

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, inicio, fim
                )
        );

        assertTrue(erro.getMessage().contains(mensagem), caso);
        verificarNenhumRepositorioConsultado();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-114–115 / RF07 / RN01 - aceita intervalos futuros válidos na criação")
    @MethodSource("intervalosValidosRF07")
    void solicitarReservaAceitaIntervalosFuturosEmMeiaHora(
            String caso,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        prepararSolicitacaoSemConflito(inicio, fim);

        Reserva resultado = reservaService.solicitarReserva(
                SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, inicio, fim
        );

        assertEquals(inicio, resultado.getInicio(), caso);
        assertEquals(fim, resultado.getFim(), caso);
    }

    @Test
    void solicitarReservaComIntervaloValidoSalvaReservaSolicitada() {
        Sala sala = salaDisponivel();
        Solicitante solicitante = solicitanteProfessor();
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID)).thenReturn(Optional.of(sala));
        when(solicitanteRepository.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitante));
        when(solicitanteRepository.buscarProfessorPorIdComBloqueio(
                SOLICITANTE_ID, TipoSolicitante.PROFESSOR
        ))
                .thenReturn(Optional.of(solicitante));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(INICIO),
                eq(FIM),
                isNull()
        )).thenReturn(0L);
        doAnswer(invocation -> invocation.getArgument(0, Reserva.class))
                .when(reservaRepository).save(any(Reserva.class));

        Reserva resultado = reservaService.solicitarReserva(
                SALA_ID,
                SOLICITANTE_ID,
                TipoSolicitante.PROFESSOR,
                INICIO,
                FIM
        );

        assertNotNull(resultado);
        assertSame(sala, resultado.getSala());
        assertSame(solicitante, resultado.getSolicitante());
        assertEquals(INICIO, resultado.getInicio());
        assertEquals(FIM, resultado.getFim());
        assertEquals(StatusReserva.SOLICITADA, resultado.getStatus());
        verify(reservaRepository).save(resultado);
        ArgumentCaptor<AuditoriaReserva> evento = ArgumentCaptor.forClass(AuditoriaReserva.class);
        verify(auditoriaReservaRepository).save(evento.capture());
        assertNull(evento.getValue().getEstadoAnterior());
        assertEquals(StatusReserva.SOLICITADA, evento.getValue().getNovoEstado());
        assertEquals(SOLICITANTE_ID, evento.getValue().getUsuarioId());
        assertEquals(PerfilUsuario.SOLICITANTE, evento.getValue().getPerfilUsuario());
        assertEquals(AGORA, evento.getValue().getDataHora());
    }

    @Test
    void solicitarReservaComSalaInexistenteLancaEntityNotFoundException() {
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, INICIO, FIM
                )
        );

        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verifyNoInteractions(
                reservaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void solicitarReservaComSalaIndisponivelLancaIllegalStateException() {
        Sala sala = new Sala(SALA_ID, 30, StatusSala.EM_MANUTENCAO);
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID)).thenReturn(Optional.of(sala));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, INICIO, FIM
                )
        );

        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verifyNoInteractions(
                reservaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void solicitarReservaComSolicitanteInexistenteLancaEntityNotFoundException() {
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(solicitanteRepository.findById(SOLICITANTE_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, INICIO, FIM
                )
        );

        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verify(solicitanteRepository).findById(SOLICITANTE_ID);
        verifyNoInteractions(reservaRepository, responsavelRepository, administradorRepository);
    }

    @Test
    void solicitarReservaComTipoDiferenteDoCadastroLancaAcessoNegadoException() {
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(solicitanteRepository.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteProfessor()));
        when(solicitanteRepository.buscarProfessorPorIdComBloqueio(
                SOLICITANTE_ID, TipoSolicitante.PROFESSOR
        ))
                .thenReturn(Optional.of(solicitanteProfessor()));

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.COORDENADOR, INICIO, FIM
                )
        );

        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verify(solicitanteRepository).findById(SOLICITANTE_ID);
        verifyNoInteractions(reservaRepository, responsavelRepository, administradorRepository);
    }

    @Test
    void solicitarReservaComConflitoLancaIllegalStateException() {
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(solicitanteRepository.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteProfessor()));
        when(solicitanteRepository.buscarProfessorPorIdComBloqueio(
                SOLICITANTE_ID, TipoSolicitante.PROFESSOR
        ))
                .thenReturn(Optional.of(solicitanteProfessor()));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(INICIO),
                eq(FIM),
                isNull()
        )).thenReturn(1L);

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.solicitarReserva(
                        SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, INICIO, FIM
                )
        );

        verify(reservaRepository).contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(INICIO),
                eq(FIM),
                isNull()
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void alterarHorarioReservaComIntervaloValidoAtualizaEDevolveReservaSalva() {
        LocalDateTime novoInicio = LocalDateTime.of(2040, 1, 2, 12, 0);
        LocalDateTime novoFim = LocalDateTime.of(2040, 1, 2, 13, 0);
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);

        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(novoInicio),
                eq(novoFim),
                eq(RESERVA_ID)
        )).thenReturn(0L);
        doAnswer(invocation -> invocation.getArgument(0, Reserva.class))
                .when(reservaRepository).save(any(Reserva.class));

        Reserva resultado = reservaService.alterarHorarioReserva(
                RESERVA_ID, SOLICITANTE_ID, novoInicio, novoFim
        );

        assertSame(reserva, resultado);
        assertEquals(novoInicio, resultado.getInicio());
        assertEquals(novoFim, resultado.getFim());
        assertSame(StatusReserva.SOLICITADA, resultado.getStatus());
        verify(reservaRepository).save(reserva);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-101–113 / RF07 / RN01 - rejeita intervalos inválidos na alteração")
    @MethodSource("intervalosInvalidosRF07")
    void alterarHorarioReservaRejeitaIntervalosInvalidosRF07(
            String caso,
            LocalDateTime inicio,
            LocalDateTime fim,
            String mensagem
    ) {
        prepararAlteracaoSemConflito(inicio, fim);

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.alterarHorarioReserva(RESERVA_ID, SOLICITANTE_ID, inicio, fim)
        );

        assertTrue(erro.getMessage().contains(mensagem), caso);
        verificarNenhumRepositorioConsultado();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-114–115 / RF07 / RN01 - aceita intervalos futuros válidos na alteração")
    @MethodSource("intervalosValidosRF07")
    void alterarHorarioReservaAceitaIntervalosFuturosEmMeiaHora(
            String caso,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(inicio),
                eq(fim),
                eq(RESERVA_ID)
        )).thenReturn(0L);
        doAnswer(invocation -> invocation.getArgument(0, Reserva.class))
                .when(reservaRepository).save(any(Reserva.class));

        Reserva resultado = reservaService.alterarHorarioReserva(
                RESERVA_ID, SOLICITANTE_ID, inicio, fim
        );

        assertEquals(inicio, resultado.getInicio(), caso);
        assertEquals(fim, resultado.getFim(), caso);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("CP-117–119 / RF07 / RN01 - valida limites do dia no fuso de São Paulo")
    @MethodSource("intervalosLimiteFusoSaoPaulo")
    void solicitarReservaValidaLimitesDoDiaNoFusoSaoPaulo(
            String caso,
            LocalDateTime agora,
            LocalDateTime inicio,
            LocalDateTime fim,
            boolean valido
    ) {
        ZoneId saoPaulo = ZoneId.of("America/Sao_Paulo");
        Clock clock = Clock.fixed(agora.atZone(saoPaulo).toInstant(), saoPaulo);
        ReservaService service = criarReservaService(clock);

        if (valido) {
            prepararSolicitacaoSemConflito(inicio, fim);
            Reserva reserva = service.solicitarReserva(
                    SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, inicio, fim
            );
            assertEquals(inicio, reserva.getInicio(), caso);
            assertEquals(fim, reserva.getFim(), caso);
        } else {
            IllegalArgumentException erro = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.solicitarReserva(
                            SALA_ID, SOLICITANTE_ID, TipoSolicitante.PROFESSOR, inicio, fim
                    ),
                    caso
            );
            assertTrue(erro.getMessage().contains("futuro"), caso);
            verificarNenhumRepositorioConsultado();
        }
    }

    @Test
    void alterarHorarioReservaComInicioNuloLancaIllegalArgumentExceptionSemConsultarRepositorios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, null, FIM
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @Test
    void alterarHorarioReservaComFimNuloLancaIllegalArgumentExceptionSemConsultarRepositorios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, INICIO, null
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @Test
    void alterarHorarioReservaComInicioIgualAoFimLancaIllegalArgumentExceptionSemConsultarRepositorios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, INICIO, INICIO
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @Test
    void alterarHorarioReservaComFimAnteriorAoInicioLancaIllegalArgumentExceptionSemConsultarRepositorios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, FIM, INICIO
                )
        );

        verificarNenhumRepositorioConsultado();
    }

    @Test
    void alterarHorarioReservaComReservaInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, INICIO, FIM
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).contarConflitos(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void alterarHorarioReservaPorSolicitanteDiferenteDoDonoLancaAcessoNegadoException() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        Solicitante outroSolicitante = new Solicitante();
        outroSolicitante.setId(99L);
        reserva.setSolicitante(outroSolicitante);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, INICIO, FIM
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).contarConflitos(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void alterarHorarioReservaComEstadoDiferenteDeSolicitadaLancaIllegalStateException() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.APROVADA);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, INICIO, FIM
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).contarConflitos(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void alterarHorarioReservaComSalaInexistenteLancaEntityNotFoundException() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, INICIO, FIM
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verify(reservaRepository, never()).contarConflitos(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(solicitanteRepository, responsavelRepository, administradorRepository);
    }

    @Test
    void alterarHorarioReservaComSalaIndisponivelLancaIllegalStateException() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        Sala salaIndisponivel = new Sala(SALA_ID, 30, StatusSala.RESERVADA);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaIndisponivel));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, INICIO, FIM
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verify(reservaRepository, never()).contarConflitos(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(solicitanteRepository, responsavelRepository, administradorRepository);
    }

    @Test
    void alterarHorarioReservaComConflitoLancaIllegalStateExceptionESemAlterarReserva() {
        LocalDateTime novoInicio = LocalDateTime.of(2040, 1, 2, 12, 0);
        LocalDateTime novoFim = LocalDateTime.of(2040, 1, 2, 13, 0);
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);

        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(novoInicio),
                eq(novoFim),
                eq(RESERVA_ID)
        )).thenReturn(1L);

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.alterarHorarioReserva(
                        RESERVA_ID, SOLICITANTE_ID, novoInicio, novoFim
                )
        );

        assertEquals(INICIO, reserva.getInicio());
        assertEquals(FIM, reserva.getFim());
        verify(reservaRepository).contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(novoInicio),
                eq(novoFim),
                eq(RESERVA_ID)
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(solicitanteRepository, responsavelRepository, administradorRepository);
    }

    @Test
    void cancelarReservaSolicitadaAlteraEstadoESalvaReserva() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.cancelarReserva(RESERVA_ID, SOLICITANTE_ID);

        assertSame(reserva, resultado);
        assertEquals(StatusReserva.CANCELADA, resultado.getStatus());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository).save(reserva);
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void cancelarReservaAprovadaAlteraEstadoESalvaReserva() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.APROVADA);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.cancelarReserva(RESERVA_ID, SOLICITANTE_ID);

        assertSame(reserva, resultado);
        assertEquals(StatusReserva.CANCELADA, resultado.getStatus());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository).save(reserva);
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void cancelarReservaInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.cancelarReserva(RESERVA_ID, SOLICITANTE_ID)
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void cancelarReservaDeOutroSolicitanteLancaAcessoNegadoException() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        Solicitante outroSolicitante = new Solicitante();
        outroSolicitante.setId(99L);
        reserva.setSolicitante(outroSolicitante);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.cancelarReserva(RESERVA_ID, SOLICITANTE_ID)
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusReserva.class,
            names = {"EM_USO", "CONCLUIDA", "REJEITADA", "CANCELADA", "NAO_COMPARECEU"}
    )
    void cancelarReservaEmEstadoNaoPermitidoLancaIllegalStateException(
            StatusReserva status
    ) {
        Reserva reserva = reservaParaAlteracao(status);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.cancelarReserva(RESERVA_ID, SOLICITANTE_ID)
        );

        assertEquals(status, reserva.getStatus());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    @DisplayName("CP-54 / RF09 / RN03 - Responsável aprova após revalidar recursos")
    void aprovarReservaPorResponsavelAtualizaReservaESalva() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        Responsavel responsavel = responsavelDeTeste();
        reserva.setAdministrador(administradorDeTeste());
        prepararAprovacaoSemConflito(reserva);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(responsavelRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(responsavel));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.aprovarReserva(
                RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
        );

        assertSame(reserva, resultado);
        assertEquals(StatusReserva.APROVADA, resultado.getStatus());
        assertSame(responsavel, resultado.getResponsavel());
        assertNull(resultado.getAdministrador());
        assertNotNull(resultado.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verificarRevalidacaoAprovacao();
        verify(responsavelRepository).findById(APROVADOR_ID);
        verify(reservaRepository).save(reserva);
        verificarAuditoria(
                StatusReserva.SOLICITADA,
                StatusReserva.APROVADA,
                AGORA,
                PerfilUsuario.RESPONSAVEL,
                APROVADOR_ID
        );
        verifyNoInteractions(administradorRepository);
    }

    @Test
    @DisplayName("CP-55 / RF09 / RN03 - Administrador aprova após revalidar recursos")
    void aprovarReservaPorAdministradorAtualizaReservaESalva() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        Administrador administrador = administradorDeTeste();
        reserva.setResponsavel(responsavelDeTeste());
        prepararAprovacaoSemConflito(reserva);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(administradorRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(administrador));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.aprovarReserva(
                RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.ADMINISTRADOR
        );

        assertSame(reserva, resultado);
        assertEquals(StatusReserva.APROVADA, resultado.getStatus());
        assertSame(administrador, resultado.getAdministrador());
        assertNull(resultado.getResponsavel());
        assertNotNull(resultado.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verificarRevalidacaoAprovacao();
        verify(administradorRepository).findById(APROVADOR_ID);
        verify(reservaRepository).save(reserva);
        verificarAuditoria(
                StatusReserva.SOLICITADA,
                StatusReserva.APROVADA,
                AGORA,
                PerfilUsuario.ADMINISTRADOR,
                APROVADOR_ID
        );
        verifyNoInteractions(responsavelRepository);
    }

    @Test
    @DisplayName("CP-124 / RF11 / RN04 / INV-05 - Recusa aprovação se sala ficou indisponível")
    void aprovarReservaComSalaIndisponivelLancaExcecaoSemSalvar() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(responsavelRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(responsavelDeTeste()));
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(new Sala(SALA_ID, 30, StatusSala.EM_MANUTENCAO)));

        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                () -> reservaService.aprovarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        assertTrue(erro.getMessage().contains("não está disponível"));
        assertEquals(StatusReserva.SOLICITADA, reserva.getStatus());
        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verify(reservaRepository, never()).contarConflitos(
                any(), any(), any(), any(), any()
        );
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(responsavelRepository).findById(APROVADOR_ID);
        verifyNoInteractions(solicitanteRepository, materialRepository);
    }

    @ParameterizedTest(name = "CP-125–127 / RF08-RF09 / RN02-RN03 - Conflito de {0} impede aprovação")
    @EnumSource(TipoConflitoAprovacao.class)
    void aprovarReservaComConflitoDeRecursoLancaExcecaoSemSalvar(
            TipoConflitoAprovacao tipoConflito
    ) {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        prepararAprovacaoSemConflito(reserva);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(responsavelRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(responsavelDeTeste()));
        simularConflito(tipoConflito);

        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                () -> reservaService.aprovarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        assertTrue(erro.getMessage().contains(tipoConflito.mensagem));
        assertEquals(StatusReserva.SOLICITADA, reserva.getStatus());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(responsavelRepository).findById(APROVADOR_ID);
    }

    @Test
    @DisplayName("CP-129 / CB-74 / RF15 / RN06-RN07 - Inicia no horário reservado e audita")
    void iniciarUtilizacaoNoInstanteDeInicioMudaEstadoEAudita() {
        Reserva reserva = reservaOperacional(StatusReserva.APROVADA);
        definirRelogio(INICIO);
        autenticarResponsavel(APROVADOR_ID);
        prepararReservaOperacional(reserva);

        Reserva resultado = reservaService.iniciarUtilizacao(RESERVA_ID);

        assertEquals(StatusReserva.EM_USO, resultado.getStatus());
        verificarAuditoria(StatusReserva.APROVADA, StatusReserva.EM_USO, INICIO);
    }

    @Test
    @DisplayName("CP-130 / CB-75 / RF15 / RN06 - Inicia imediatamente antes do fim")
    void iniciarUtilizacaoImediatamenteAntesDoFimEPermitido() {
        Reserva reserva = reservaOperacional(StatusReserva.APROVADA);
        definirRelogio(FIM.minusNanos(1));
        autenticarResponsavel(APROVADOR_ID);
        prepararReservaOperacional(reserva);

        Reserva resultado = reservaService.iniciarUtilizacao(RESERVA_ID);

        assertEquals(StatusReserva.EM_USO, resultado.getStatus());
    }

    @ParameterizedTest(name = "CP-131 / CB-76 / RF15 / RN06 - Início recusado às {0}")
    @MethodSource("limitesInicioInvalidos")
    void iniciarUtilizacaoForaDoIntervaloReservadoERecusado(LocalDateTime agora) {
        Reserva reserva = reservaOperacional(StatusReserva.APROVADA);
        definirRelogio(agora);
        autenticarResponsavel(APROVADOR_ID);
        prepararReservaOperacional(reserva);

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.iniciarUtilizacao(RESERVA_ID)
        );

        assertEquals(StatusReserva.APROVADA, reserva.getStatus());
        verificarNenhumaAuditoriaOuPersistencia();
    }

    @Test
    @DisplayName("CP-132 / CB-77 / RF15 / RN06-RN07 - Ausência exatamente no fim")
    void registrarNaoComparecimentoNoFimExatoEAuditado() {
        Reserva reserva = reservaOperacional(StatusReserva.APROVADA);
        definirRelogio(FIM);
        autenticarResponsavel(APROVADOR_ID);
        prepararReservaOperacional(reserva);

        Reserva resultado = reservaService.registrarNaoComparecimento(RESERVA_ID);

        assertEquals(StatusReserva.NAO_COMPARECEU, resultado.getStatus());
        verificarAuditoria(
                StatusReserva.APROVADA, StatusReserva.NAO_COMPARECEU, FIM
        );
    }

    @Test
    @DisplayName("CP-133 / CB-78 / RF15 / RN06 - Ausência recusada antes do fim")
    void registrarNaoComparecimentoAntesDoFimERecusado() {
        Reserva reserva = reservaOperacional(StatusReserva.APROVADA);
        definirRelogio(FIM.minusNanos(1));
        autenticarResponsavel(APROVADOR_ID);
        prepararReservaOperacional(reserva);

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.registrarNaoComparecimento(RESERVA_ID)
        );

        assertEquals(StatusReserva.APROVADA, reserva.getStatus());
        verificarNenhumaAuditoriaOuPersistencia();
    }

    @Test
    @DisplayName("CP-134 / CB-79 / RF15 / RN06-RN07 - Conclui antes do fim e audita")
    void concluirUtilizacaoAntesDoFimEPermitido() {
        Reserva reserva = reservaOperacional(StatusReserva.EM_USO);
        definirRelogio(INICIO.plusMinutes(30));
        autenticarResponsavel(APROVADOR_ID);
        prepararReservaOperacional(reserva);

        Reserva resultado = reservaService.concluirUtilizacao(RESERVA_ID);

        assertEquals(StatusReserva.CONCLUIDA, resultado.getStatus());
        verificarAuditoria(
                StatusReserva.EM_USO, StatusReserva.CONCLUIDA, INICIO.plusMinutes(30)
        );
    }

    @Test
    @DisplayName("CP-135 / CB-80 / RF02 / RF15 - Solicitante não opera transições")
    void solicitanteNaoPodeIniciarUtilizacao() {
        Reserva reserva = reservaOperacional(StatusReserva.APROVADA);
        definirRelogio(INICIO);
        ContextoAutenticacao.definir(new UsuarioAutenticado(
                SOLICITANTE_ID, "Solicitante", null, PerfilUsuario.SOLICITANTE,
                TipoSolicitante.PROFESSOR
        ));
        prepararReservaOperacional(reserva);

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.iniciarUtilizacao(RESERVA_ID)
        );

        verificarNenhumaAuditoriaOuPersistencia();
    }

    @Test
    @DisplayName("CP-136 / CB-81 / RF02 / RF15 - Responsável não opera reserva alheia")
    void responsavelDiferenteDoAprovadorNaoPodeConcluirUtilizacao() {
        Reserva reserva = reservaOperacional(StatusReserva.EM_USO);
        definirRelogio(INICIO.plusMinutes(30));
        autenticarResponsavel(99L);
        prepararReservaOperacional(reserva);

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.concluirUtilizacao(RESERVA_ID)
        );

        verificarNenhumaAuditoriaOuPersistencia();
    }

    @Test
    @DisplayName("CP-137 / CB-82 / RF02 / RF15 - Administrador opera qualquer reserva")
    void administradorPodeIniciarUtilizacaoEmQualquerReserva() {
        Reserva reserva = reservaOperacional(StatusReserva.APROVADA);
        reserva.setResponsavel(null);
        definirRelogio(INICIO);
        ContextoAutenticacao.definir(new UsuarioAutenticado(
                99L, "Administrador", null, PerfilUsuario.ADMINISTRADOR, null
        ));
        prepararReservaOperacional(reserva);

        Reserva resultado = reservaService.iniciarUtilizacao(RESERVA_ID);

        assertEquals(StatusReserva.EM_USO, resultado.getStatus());
        ArgumentCaptor<AuditoriaReserva> evento = ArgumentCaptor.forClass(AuditoriaReserva.class);
        verify(auditoriaReservaRepository).save(evento.capture());
        assertEquals(99L, evento.getValue().getUsuarioId());
        assertEquals(PerfilUsuario.ADMINISTRADOR, evento.getValue().getPerfilUsuario());
    }

    @Test
    void rejeitarReservaPorResponsavelAtualizaReservaESalva() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        Responsavel responsavel = responsavelDeTeste();
        reserva.setAdministrador(administradorDeTeste());
        reserva.setAprovadoEm(INICIO);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(responsavelRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(responsavel));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.rejeitarReserva(
                RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
        );

        assertSame(reserva, resultado);
        assertEquals(StatusReserva.REJEITADA, resultado.getStatus());
        assertSame(responsavel, resultado.getResponsavel());
        assertNull(resultado.getAdministrador());
        assertEquals(INICIO, resultado.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(responsavelRepository).findById(APROVADOR_ID);
        verify(reservaRepository).save(reserva);
        verifyNoInteractions(salaRepository, solicitanteRepository, administradorRepository);
    }

    @Test
    void rejeitarReservaPorAdministradorAtualizaReservaESalva() {
        Reserva reserva = reservaParaAlteracao(StatusReserva.SOLICITADA);
        Administrador administrador = administradorDeTeste();
        reserva.setResponsavel(responsavelDeTeste());
        reserva.setAprovadoEm(INICIO);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(administradorRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(administrador));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        Reserva resultado = reservaService.rejeitarReserva(
                RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.ADMINISTRADOR
        );

        assertSame(reserva, resultado);
        assertEquals(StatusReserva.REJEITADA, resultado.getStatus());
        assertSame(administrador, resultado.getAdministrador());
        assertNull(resultado.getResponsavel());
        assertEquals(INICIO, resultado.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(administradorRepository).findById(APROVADOR_ID);
        verify(reservaRepository).save(reserva);
        verifyNoInteractions(salaRepository, solicitanteRepository, responsavelRepository);
    }

    @Test
    void aprovarReservaInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.aprovarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void rejeitarReservaInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.rejeitarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void aprovarReservaComResponsavelInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reservaParaAlteracao(StatusReserva.SOLICITADA)));
        when(responsavelRepository.findById(APROVADOR_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.aprovarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(responsavelRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, administradorRepository);
    }

    @Test
    void rejeitarReservaComResponsavelInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reservaParaAlteracao(StatusReserva.SOLICITADA)));
        when(responsavelRepository.findById(APROVADOR_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.rejeitarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(responsavelRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, administradorRepository);
    }

    @Test
    void aprovarReservaComAdministradorInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reservaParaAlteracao(StatusReserva.SOLICITADA)));
        when(administradorRepository.findById(APROVADOR_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.aprovarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.ADMINISTRADOR
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(administradorRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, responsavelRepository);
    }

    @Test
    void rejeitarReservaComAdministradorInexistenteLancaEntityNotFoundException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reservaParaAlteracao(StatusReserva.SOLICITADA)));
        when(administradorRepository.findById(APROVADOR_ID)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> reservaService.rejeitarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.ADMINISTRADOR
                )
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(administradorRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, responsavelRepository);
    }

    @Test
    void aprovarReservaComTipoNuloLancaAcessoNegadoException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reservaParaAlteracao(StatusReserva.SOLICITADA)));

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.aprovarReserva(RESERVA_ID, APROVADOR_ID, null)
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @Test
    void rejeitarReservaComTipoNuloLancaAcessoNegadoException() {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reservaParaAlteracao(StatusReserva.SOLICITADA)));

        assertThrows(
                AcessoNegadoException.class,
                () -> reservaService.rejeitarReserva(RESERVA_ID, APROVADOR_ID, null)
        );

        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    @ParameterizedTest(name = "aprovação por responsável com estado {0}")
    @EnumSource(value = StatusReserva.class, mode = EnumSource.Mode.EXCLUDE, names = "SOLICITADA")
    void aprovarReservaPorResponsavelComEstadoIncompativelPreservaReserva(
            StatusReserva status
    ) {
        Reserva reserva = reservaParaAlteracao(status);
        Responsavel responsavelInicial = responsavelDeTeste();
        Administrador administradorInicial = administradorDeTeste();
        LocalDateTime aprovadoEmInicial = INICIO.minusDays(1);
        reserva.setResponsavel(responsavelInicial);
        reserva.setAdministrador(administradorInicial);
        reserva.setAprovadoEm(aprovadoEmInicial);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(responsavelRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(responsavelDeTeste()));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.aprovarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        assertEquals(status, reserva.getStatus());
        assertSame(responsavelInicial, reserva.getResponsavel());
        assertSame(administradorInicial, reserva.getAdministrador());
        assertEquals(aprovadoEmInicial, reserva.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(responsavelRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, administradorRepository);
    }

    @ParameterizedTest(name = "aprovação por administrador com estado {0}")
    @EnumSource(value = StatusReserva.class, mode = EnumSource.Mode.EXCLUDE, names = "SOLICITADA")
    void aprovarReservaPorAdministradorComEstadoIncompativelPreservaReserva(
            StatusReserva status
    ) {
        Reserva reserva = reservaParaAlteracao(status);
        Responsavel responsavelInicial = responsavelDeTeste();
        Administrador administradorInicial = administradorDeTeste();
        LocalDateTime aprovadoEmInicial = INICIO.minusDays(1);
        reserva.setResponsavel(responsavelInicial);
        reserva.setAdministrador(administradorInicial);
        reserva.setAprovadoEm(aprovadoEmInicial);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(administradorRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(administradorDeTeste()));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.aprovarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.ADMINISTRADOR
                )
        );

        assertEquals(status, reserva.getStatus());
        assertSame(responsavelInicial, reserva.getResponsavel());
        assertSame(administradorInicial, reserva.getAdministrador());
        assertEquals(aprovadoEmInicial, reserva.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(administradorRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, responsavelRepository);
    }

    @ParameterizedTest(name = "rejeição por responsável com estado {0}")
    @EnumSource(value = StatusReserva.class, mode = EnumSource.Mode.EXCLUDE, names = "SOLICITADA")
    void rejeitarReservaPorResponsavelComEstadoIncompativelPreservaReserva(
            StatusReserva status
    ) {
        Reserva reserva = reservaParaAlteracao(status);
        Responsavel responsavelInicial = responsavelDeTeste();
        Administrador administradorInicial = administradorDeTeste();
        LocalDateTime aprovadoEmInicial = INICIO.minusDays(1);
        reserva.setResponsavel(responsavelInicial);
        reserva.setAdministrador(administradorInicial);
        reserva.setAprovadoEm(aprovadoEmInicial);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(responsavelRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(responsavelDeTeste()));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.rejeitarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.RESPONSAVEL
                )
        );

        assertEquals(status, reserva.getStatus());
        assertSame(responsavelInicial, reserva.getResponsavel());
        assertSame(administradorInicial, reserva.getAdministrador());
        assertEquals(aprovadoEmInicial, reserva.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(responsavelRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, administradorRepository);
    }

    @ParameterizedTest(name = "rejeição por administrador com estado {0}")
    @EnumSource(value = StatusReserva.class, mode = EnumSource.Mode.EXCLUDE, names = "SOLICITADA")
    void rejeitarReservaPorAdministradorComEstadoIncompativelPreservaReserva(
            StatusReserva status
    ) {
        Reserva reserva = reservaParaAlteracao(status);
        Responsavel responsavelInicial = responsavelDeTeste();
        Administrador administradorInicial = administradorDeTeste();
        LocalDateTime aprovadoEmInicial = INICIO.minusDays(1);
        reserva.setResponsavel(responsavelInicial);
        reserva.setAdministrador(administradorInicial);
        reserva.setAprovadoEm(aprovadoEmInicial);
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(administradorRepository.findById(APROVADOR_ID))
                .thenReturn(Optional.of(administradorDeTeste()));

        assertThrows(
                IllegalStateException.class,
                () -> reservaService.rejeitarReserva(
                        RESERVA_ID, APROVADOR_ID, TipoAprovadorReserva.ADMINISTRADOR
                )
        );

        assertEquals(status, reserva.getStatus());
        assertSame(responsavelInicial, reserva.getResponsavel());
        assertSame(administradorInicial, reserva.getAdministrador());
        assertEquals(aprovadoEmInicial, reserva.getAprovadoEm());
        verify(reservaRepository).buscarPorIdComBloqueio(RESERVA_ID);
        verify(administradorRepository).findById(APROVADOR_ID);
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(salaRepository, solicitanteRepository, responsavelRepository);
    }

    private void verificarNenhumRepositorioConsultado() {
        verifyNoInteractions(
                reservaRepository,
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository
        );
    }

    private Sala salaDisponivel() {
        return new Sala(SALA_ID, 30, StatusSala.DISPONIVEL);
    }

    private void prepararSolicitacaoSemConflito(LocalDateTime inicio, LocalDateTime fim) {
        Sala sala = salaDisponivel();
        Solicitante solicitante = solicitanteProfessor();
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID)).thenReturn(Optional.of(sala));
        when(solicitanteRepository.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitante));
        when(solicitanteRepository.buscarProfessorPorIdComBloqueio(
                SOLICITANTE_ID, TipoSolicitante.PROFESSOR
        )).thenReturn(Optional.of(solicitante));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(inicio),
                eq(fim),
                isNull()
        )).thenReturn(0L);
        doAnswer(invocation -> invocation.getArgument(0, Reserva.class))
                .when(reservaRepository).save(any(Reserva.class));
    }

    private void prepararAprovacaoSemConflito(Reserva reserva) {
        Solicitante professor = solicitanteProfessor();
        Material material = new Material(MATERIAL_ID, "Projetor");
        reserva.setProfessores(Set.of(professor));
        reserva.setMateriais(Set.of(material));

        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(solicitanteRepository.buscarProfessorPorIdComBloqueio(
                SOLICITANTE_ID, TipoSolicitante.PROFESSOR
        )).thenReturn(Optional.of(professor));
        when(materialRepository.buscarPorIdComBloqueio(MATERIAL_ID))
                .thenReturn(Optional.of(material));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID), eq(EnumSet.of(
                        StatusReserva.APROVADA, StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA, StatusReserva.NAO_COMPARECEU
                )),
                eq(INICIO), eq(FIM), eq(RESERVA_ID)
        )).thenReturn(0L);
        when(reservaRepository.contarConflitosPorProfessor(
                eq(SOLICITANTE_ID), eq(EnumSet.of(
                        StatusReserva.APROVADA, StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA, StatusReserva.NAO_COMPARECEU
                )),
                eq(INICIO), eq(FIM), eq(RESERVA_ID)
        )).thenReturn(0L);
        when(reservaRepository.contarConflitosPorMaterial(
                eq(MATERIAL_ID), eq(EnumSet.of(
                        StatusReserva.APROVADA, StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA, StatusReserva.NAO_COMPARECEU
                )),
                eq(INICIO), eq(FIM), eq(RESERVA_ID)
        )).thenReturn(0L);
    }

    private Reserva reservaOperacional(StatusReserva status) {
        Reserva reserva = reservaParaAlteracao(status);
        reserva.setResponsavel(responsavelDeTeste());
        return reserva;
    }

    private void prepararReservaOperacional(Reserva reserva) {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.save(reserva)).thenReturn(reserva);
    }

    private void definirRelogio(LocalDateTime agora) {
        reservaService = criarReservaService(
                Clock.fixed(agora.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    private void autenticarResponsavel(Long id) {
        ContextoAutenticacao.definir(new UsuarioAutenticado(
                id, "Responsável", null, PerfilUsuario.RESPONSAVEL, null
        ));
    }

    private void verificarAuditoria(
            StatusReserva estadoAnterior,
            StatusReserva novoEstado
    ) {
        verificarAuditoria(estadoAnterior, novoEstado, AGORA);
    }

    private void verificarAuditoria(
            StatusReserva estadoAnterior,
            StatusReserva novoEstado,
            LocalDateTime dataHora
    ) {
        verificarAuditoria(
                estadoAnterior, novoEstado, dataHora,
                PerfilUsuario.RESPONSAVEL, APROVADOR_ID
        );
    }

    private void verificarAuditoria(
            StatusReserva estadoAnterior,
            StatusReserva novoEstado,
            LocalDateTime dataHora,
            PerfilUsuario perfil,
            Long usuarioId
    ) {
        ArgumentCaptor<AuditoriaReserva> evento = ArgumentCaptor.forClass(AuditoriaReserva.class);
        verify(auditoriaReservaRepository).save(evento.capture());
        assertEquals(RESERVA_ID, evento.getValue().getReserva().getId());
        assertEquals(estadoAnterior, evento.getValue().getEstadoAnterior());
        assertEquals(novoEstado, evento.getValue().getNovoEstado());
        assertEquals(usuarioId, evento.getValue().getUsuarioId());
        assertEquals(perfil, evento.getValue().getPerfilUsuario());
        assertEquals(dataHora, evento.getValue().getDataHora());
    }

    private void verificarNenhumaAuditoriaOuPersistencia() {
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(auditoriaReservaRepository, never()).save(any(AuditoriaReserva.class));
    }

    private void verificarRevalidacaoAprovacao() {
        Set<StatusReserva> statusConfirmados = EnumSet.of(
                StatusReserva.APROVADA,
                StatusReserva.EM_USO,
                StatusReserva.CONCLUIDA,
                StatusReserva.NAO_COMPARECEU
        );
        verify(salaRepository).buscarPorIdComBloqueio(SALA_ID);
        verify(solicitanteRepository).buscarProfessorPorIdComBloqueio(
                SOLICITANTE_ID, TipoSolicitante.PROFESSOR
        );
        verify(materialRepository).buscarPorIdComBloqueio(MATERIAL_ID);
        verify(reservaRepository).contarConflitos(
                SALA_ID, statusConfirmados, INICIO, FIM, RESERVA_ID
        );
        verify(reservaRepository).contarConflitosPorProfessor(
                SOLICITANTE_ID, statusConfirmados, INICIO, FIM, RESERVA_ID
        );
        verify(reservaRepository).contarConflitosPorMaterial(
                MATERIAL_ID, statusConfirmados, INICIO, FIM, RESERVA_ID
        );
    }

    private void simularConflito(TipoConflitoAprovacao tipoConflito) {
        Set<StatusReserva> statusConfirmados = EnumSet.of(
                StatusReserva.APROVADA,
                StatusReserva.EM_USO,
                StatusReserva.CONCLUIDA,
                StatusReserva.NAO_COMPARECEU
        );
        switch (tipoConflito) {
            case SALA -> when(reservaRepository.contarConflitos(
                    eq(SALA_ID), eq(statusConfirmados), eq(INICIO), eq(FIM), eq(RESERVA_ID)
            )).thenReturn(1L);
            case PROFESSOR -> when(reservaRepository.contarConflitosPorProfessor(
                    eq(SOLICITANTE_ID), eq(statusConfirmados),
                    eq(INICIO), eq(FIM), eq(RESERVA_ID)
            )).thenReturn(1L);
            case MATERIAL -> when(reservaRepository.contarConflitosPorMaterial(
                    eq(MATERIAL_ID), eq(statusConfirmados),
                    eq(INICIO), eq(FIM), eq(RESERVA_ID)
            )).thenReturn(1L);
        }
    }

    private ReservaService criarReservaService(Clock clock) {
        return new ReservaService(
                reservaRepository,
                auditoriaReservaRepository,
                salaRepository,
                solicitanteRepository,
                responsavelRepository,
                administradorRepository,
                materialRepository,
                clock
        );
    }

    private void prepararAlteracaoSemConflito(LocalDateTime inicio, LocalDateTime fim) {
        when(reservaRepository.buscarPorIdComBloqueio(RESERVA_ID))
                .thenReturn(Optional.of(reservaParaAlteracao(StatusReserva.SOLICITADA)));
        when(salaRepository.buscarPorIdComBloqueio(SALA_ID))
                .thenReturn(Optional.of(salaDisponivel()));
        when(reservaRepository.contarConflitos(
                eq(SALA_ID),
                eq(EnumSet.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.EM_USO,
                        StatusReserva.CONCLUIDA,
                        StatusReserva.NAO_COMPARECEU
                )),
                eq(inicio),
                eq(fim),
                eq(RESERVA_ID)
        )).thenReturn(0L);
        doAnswer(invocation -> invocation.getArgument(0, Reserva.class))
                .when(reservaRepository).save(any(Reserva.class));
    }

    private static Stream<Arguments> intervalosInvalidosRF07() {
        LocalDateTime diaFuturo = AGORA.plusDays(1);
        return Stream.of(
                Arguments.of(
                        "data passada", AGORA.minusDays(1), AGORA.minusDays(1).plusHours(1),
                        "futuro"
                ),
                Arguments.of("exatamente agora", AGORA, AGORA.plusHours(1), "futuro"),
                Arguments.of(
                        "um minuto no futuro", AGORA.plusMinutes(1),
                        AGORA.plusMinutes(1).plusHours(1), "30 minutos"
                ),
                Arguments.of(
                        "início no minuto 15", diaFuturo.withMinute(15),
                        diaFuturo.withMinute(15).plusHours(1), "30 minutos"
                ),
                Arguments.of(
                        "início no minuto 45", diaFuturo.withMinute(45),
                        diaFuturo.withMinute(45).plusHours(1), "30 minutos"
                ),
                Arguments.of(
                        "fim no minuto 15", diaFuturo, diaFuturo.plusHours(1).withMinute(15),
                        "30 minutos"
                ),
                Arguments.of(
                        "fim no minuto 45", diaFuturo, diaFuturo.plusHours(1).withMinute(45),
                        "30 minutos"
                ),
                Arguments.of(
                        "segundos no início", diaFuturo.withSecond(1),
                        diaFuturo.plusHours(1), "segundos e nanos"
                ),
                Arguments.of(
                        "segundos no fim", diaFuturo,
                        diaFuturo.plusHours(1).withSecond(1), "segundos e nanos"
                ),
                Arguments.of(
                        "nanos no início", diaFuturo.withNano(1),
                        diaFuturo.plusHours(1), "segundos e nanos"
                ),
                Arguments.of(
                        "nanos no fim", diaFuturo,
                        diaFuturo.plusHours(1).withNano(1), "segundos e nanos"
                ),
                Arguments.of("fim igual ao início", diaFuturo, diaFuturo, "posterior"),
                Arguments.of(
                        "fim anterior ao início", diaFuturo.plusHours(1), diaFuturo,
                        "posterior"
                )
        );
    }

    private static Stream<Arguments> limitesInicioInvalidos() {
        return Stream.of(
                Arguments.of(INICIO.minusNanos(1)),
                Arguments.of(FIM)
        );
    }

    private static Stream<Arguments> intervalosValidosRF07() {
        LocalDateTime diaFuturo = AGORA.plusDays(1);
        return Stream.of(
                Arguments.of("10:00-11:00", diaFuturo, diaFuturo.plusHours(1)),
                Arguments.of(
                        "10:30-12:00", diaFuturo.plusMinutes(30),
                        diaFuturo.plusHours(2)
                )
        );
    }

    private static Stream<Arguments> intervalosLimiteFusoSaoPaulo() {
        return Stream.of(
                Arguments.of(
                        "23:50 rejeita 23:30 do mesmo dia",
                        LocalDateTime.of(2026, 11, 12, 23, 50),
                        LocalDateTime.of(2026, 11, 12, 23, 30),
                        LocalDateTime.of(2026, 11, 13, 0, 0),
                        false
                ),
                Arguments.of(
                        "23:50 aceita 00:00 do dia seguinte",
                        LocalDateTime.of(2026, 11, 12, 23, 50),
                        LocalDateTime.of(2026, 11, 13, 0, 0),
                        LocalDateTime.of(2026, 11, 13, 1, 0),
                        true
                ),
                Arguments.of(
                        "01:00 rejeita 00:30 do mesmo dia",
                        LocalDateTime.of(2026, 11, 12, 1, 0),
                        LocalDateTime.of(2026, 11, 12, 0, 30),
                        LocalDateTime.of(2026, 11, 12, 1, 0),
                        false
                )
        );
    }

    private Solicitante solicitanteProfessor() {
        Solicitante solicitante = new Solicitante();
        solicitante.setId(SOLICITANTE_ID);
        solicitante.setTipoSolicitante(TipoSolicitante.PROFESSOR);
        return solicitante;
    }

    private Reserva reservaParaAlteracao(StatusReserva status) {
        Reserva reserva = new Reserva();
        reserva.setId(RESERVA_ID);
        reserva.setSala(salaDisponivel());
        reserva.setSolicitante(solicitanteProfessor());
        reserva.setInicio(INICIO);
        reserva.setFim(FIM);
        reserva.setStatus(status);
        return reserva;
    }

    private Responsavel responsavelDeTeste() {
        Responsavel responsavel = new Responsavel();
        responsavel.setId(APROVADOR_ID);
        return responsavel;
    }

    private Administrador administradorDeTeste() {
        Administrador administrador = new Administrador();
        administrador.setId(APROVADOR_ID);
        return administrador;
    }

    private enum TipoConflitoAprovacao {
        SALA("sala"),
        PROFESSOR("professor"),
        MATERIAL("material");

        private final String mensagem;

        TipoConflitoAprovacao(String mensagem) {
            this.mensagem = mensagem;
        }
    }
}
