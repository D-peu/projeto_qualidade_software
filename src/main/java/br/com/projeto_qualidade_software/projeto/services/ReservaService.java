package br.com.projeto_qualidade_software.projeto.services;

import br.com.projeto_qualidade_software.projeto.entities.Administrador;
import br.com.projeto_qualidade_software.projeto.entities.AlteracaoHorarioAplicada;
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
import br.com.projeto_qualidade_software.projeto.repositories.AlteracaoHorarioAplicadaRepository;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservaService.class);
    private static final Set<StatusReserva> STATUS_QUE_OCUPAM_HORARIO =
            EnumSet.of(
                    StatusReserva.SOLICITADA,
                    StatusReserva.APROVADA,
                    StatusReserva.EM_USO,
                    StatusReserva.CONCLUIDA,
                    StatusReserva.NAO_COMPARECEU
            );
    private static final Set<StatusReserva> STATUS_CONFIRMADOS =
            EnumSet.of(
                    StatusReserva.APROVADA,
                    StatusReserva.EM_USO,
                    StatusReserva.CONCLUIDA,
                    StatusReserva.NAO_COMPARECEU
            );

    private final ReservaRepository reservaRepository;
    private final AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaRepository;
    private final AuditoriaReservaRepository auditoriaReservaRepository;
    private final SalaRepository salaRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final ResponsavelRepository responsavelRepository;
    private final AdministradorRepository administradorRepository;
    private final MaterialRepository materialRepository;
    private final EntityManager entityManager;
    private final Clock clock;
    private final NotificacaoClient notificacaoClient;

    @Autowired
    public ReservaService(
            ReservaRepository reservaRepository,
            AlteracaoHorarioAplicadaRepository alteracaoHorarioAplicadaRepository,
            AuditoriaReservaRepository auditoriaReservaRepository,
            SalaRepository salaRepository,
            SolicitanteRepository solicitanteRepository,
            ResponsavelRepository responsavelRepository,
            AdministradorRepository administradorRepository,
            MaterialRepository materialRepository,
            EntityManager entityManager,
            Clock clock,
            NotificacaoClient notificacaoClient
    ) {
        this.reservaRepository = reservaRepository;
        this.alteracaoHorarioAplicadaRepository = alteracaoHorarioAplicadaRepository;
        this.auditoriaReservaRepository = auditoriaReservaRepository;
        this.salaRepository = salaRepository;
        this.solicitanteRepository = solicitanteRepository;
        this.responsavelRepository = responsavelRepository;
        this.administradorRepository = administradorRepository;
        this.materialRepository = materialRepository;
        this.entityManager = entityManager;
        this.clock = clock;
        this.notificacaoClient = notificacaoClient;
    }

    @Transactional
    public Reserva solicitarReserva(
            Long salaId,
            Long solicitanteId,
            TipoSolicitante tipoSolicitante,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        return solicitarReserva(
                salaId, solicitanteId, tipoSolicitante, inicio, fim, List.of(), List.of()
        );
    }

    @Transactional
    public Reserva solicitarReserva(
            Long salaId,
            Long solicitanteId,
            TipoSolicitante tipoSolicitante,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<Long> professorIds,
            List<Long> materialIds
    ) {
        validarIntervalo(inicio, fim);
        ContextoAutenticacao.exigirSolicitante(solicitanteId, tipoSolicitante);

        Sala sala = buscarSalaBloqueada(salaId);
        validarDisponibilidadeDaSala(sala);
        Solicitante solicitante = solicitanteRepository.findById(solicitanteId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Solicitante não encontrado: " + solicitanteId
                ));
        if (solicitante.getTipoSolicitante() != tipoSolicitante) {
            throw new AcessoNegadoException(
                    "O cargo informado não corresponde ao perfil cadastrado do solicitante."
            );
        }

        Set<Long> idsProfessores = new TreeSet<>(
                professorIds == null ? List.of() : professorIds
        );
        if (tipoSolicitante == TipoSolicitante.PROFESSOR) {
            idsProfessores.add(solicitanteId);
        }
        Set<Solicitante> professores = buscarProfessoresBloqueados(idsProfessores);
        Set<Material> materiais = buscarMateriaisBloqueados(materialIds);
        Set<Long> idsMateriais = materiais.stream()
                .map(Material::getId)
                .collect(Collectors.toCollection(TreeSet::new));

        validarAusenciaDeConflito(
                sala.getId(),
                idsProfessores,
                idsMateriais,
                inicio,
                fim,
                null,
                STATUS_QUE_OCUPAM_HORARIO
        );

        Reserva reserva = new Reserva();
        reserva.setSala(sala);
        reserva.setSolicitante(solicitante);
        reserva.setProfessores(professores);
        reserva.setMateriais(materiais);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setStatus(StatusReserva.SOLICITADA);
        Reserva criada = reservaRepository.save(reserva);
        registrarAuditoria(
                criada,
                null,
                StatusReserva.SOLICITADA,
                solicitanteId,
                PerfilUsuario.SOLICITANTE
        );
        return criada;
    }

    @Transactional
    public Reserva alterarHorarioReserva(
            Long reservaId,
            Long solicitanteId,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        validarIntervalo(inicio, fim);
        ContextoAutenticacao.exigirSolicitante(solicitanteId);
        Reserva reserva = buscarReservaComSalaBloqueada(reservaId);
        validarSolicitante(reserva, solicitanteId);
        if (reserva.getStatus() != StatusReserva.SOLICITADA) {
            throw new IllegalStateException(
                    "Somente reservas solicitadas podem ter o horário alterado."
            );
        }

        Sala sala = buscarSalaBloqueada(reserva.getSala().getId());
        validarDisponibilidadeDaSala(sala);
        Set<Long> professorIds = reserva.getProfessores().stream()
                .map(Solicitante::getId)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<Long> materialIds = reserva.getMateriais().stream()
                .map(Material::getId)
                .collect(Collectors.toCollection(TreeSet::new));
        buscarProfessoresBloqueados(professorIds);
        buscarMateriaisBloqueados(materialIds);
        validarAusenciaDeConflito(
                sala.getId(),
                professorIds,
                materialIds,
                inicio,
                fim,
                reservaId,
                STATUS_QUE_OCUPAM_HORARIO
        );
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva cancelarReserva(Long reservaId, Long solicitanteId) {
        ContextoAutenticacao.exigirSolicitante(solicitanteId);
        Reserva reserva = buscarReservaBloqueada(reservaId);
        validarSolicitante(reserva, solicitanteId);
        if (reserva.getStatus() != StatusReserva.SOLICITADA
                && reserva.getStatus() != StatusReserva.APROVADA) {
            throw new IllegalStateException(
                    "Somente reservas solicitadas ou aprovadas podem ser canceladas."
            );
        }

        StatusReserva anterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.CANCELADA);
        Reserva cancelada = reservaRepository.save(reserva);
        registrarAuditoria(
                cancelada,
                anterior,
                StatusReserva.CANCELADA,
                solicitanteId,
                PerfilUsuario.SOLICITANTE
        );
        notificarMudancaDeEstado(cancelada);
        return cancelada;
    }

    @Transactional
    public Reserva aprovarReserva(
            Long reservaId,
            Long usuarioId,
            TipoAprovadorReserva tipoAprovador
    ) {
        ContextoAutenticacao.exigirAprovador(usuarioId, tipoAprovador);
        if (tipoAprovador == TipoAprovadorReserva.RESPONSAVEL) {
            return aprovar(reservaId, buscarResponsavel(usuarioId));
        }
        if (tipoAprovador == TipoAprovadorReserva.ADMINISTRADOR) {
            throw new AcessoNegadoException(
                    "Somente Responsável pode aprovar reservas."
            );
        }
        throw new AcessoNegadoException(
                "Somente Responsável pode aprovar reservas."
        );
    }

    @Transactional
    public Reserva rejeitarReserva(
            Long reservaId,
            Long usuarioId,
            TipoAprovadorReserva tipoAprovador
    ) {
        ContextoAutenticacao.exigirAprovador(usuarioId, tipoAprovador);
        Reserva reserva = buscarReservaBloqueada(reservaId);
        StatusReserva anterior = reserva.getStatus();
        if (tipoAprovador == TipoAprovadorReserva.RESPONSAVEL) {
            reserva.rejeitar(buscarResponsavel(usuarioId));
        } else if (tipoAprovador == TipoAprovadorReserva.ADMINISTRADOR) {
            reserva.rejeitar(buscarAdministrador(usuarioId));
        } else {
            throw new AcessoNegadoException(
                    "Somente Responsável ou Administrador pode decidir reservas."
            );
        }
        Reserva rejeitada = reservaRepository.save(reserva);
        registrarAuditoria(
                rejeitada,
                anterior,
                StatusReserva.REJEITADA,
                usuarioId,
                perfilAprovador(tipoAprovador)
        );
        notificarMudancaDeEstado(rejeitada);
        return rejeitada;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarReservasDoSolicitante(Long solicitanteId) {
        ContextoAutenticacao.exigirAcessoListaSolicitante(solicitanteId);
        return reservaRepository.findBySolicitanteIdOrderByInicioDesc(solicitanteId);
    }

    @Transactional
    public List<Reserva> listarReservas(StatusReserva status) {
        expirarSolicitacoesVencidas();
        ContextoAutenticacao.exigirEquipe();
        return reservaRepository.findByStatusOrderBySolicitadoEmAsc(status);
    }

    @Transactional
    public List<Reserva> listarReservasPendentes() {
        return listarReservas(StatusReserva.SOLICITADA);
    }

    @Transactional
    public List<Reserva> listarReservas() {
        expirarSolicitacoesVencidas();
        return reservaRepository.findAll();
    }

    @Transactional
    public Optional<Reserva> buscarReserva(Long reservaId) {
        expirarSolicitacoesVencidas();
        Optional<Reserva> reserva = reservaRepository.findById(reservaId);
        reserva.ifPresent(ContextoAutenticacao::exigirAcessoReserva);
        return reserva;
    }

    @Transactional
    public Reserva aplicarAlteracaoAprovada(
            Long reservaId,
            LocalDateTime novoInicio,
            LocalDateTime novoFim,
            Responsavel responsavel
    ) {
        Long salaId = reservaRepository.findSalaIdByReservaId(reservaId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));

        Sala sala = entityManager.find(Sala.class, salaId, LockModeType.PESSIMISTIC_WRITE);
        if (sala == null) {
            throw new EntityNotFoundException("Sala da reserva não encontrada.");
        }

        List<Reserva> reservasDaSala = reservaRepository.findAllBySalaIdForUpdate(salaId);
        Reserva reserva = reservasDaSala.stream()
                .filter(item -> Objects.equals(item.getId(), reservaId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));

        if (reserva.getStatus() != StatusReserva.APROVADA) {
            throw new IllegalStateException(
                    "Somente reservas aprovadas podem ter o horário alterado."
            );
        }

        if (Objects.equals(reserva.getInicio(), novoInicio)
                && Objects.equals(reserva.getFim(), novoFim)) {
            return reserva;
        }

        validarNovoIntervalo(novoInicio, novoFim);

        if (sala.getStatusSala() == StatusSala.EM_MANUTENCAO
                || existeConflito(reserva, novoInicio, novoFim, reservasDaSala)) {
            throw new ReservaIndisponivelException(
                    "O recurso ou horário não está mais disponível para esta reserva."
            );
        }

        if (responsavel == null || responsavel.getId() == null) {
            throw new IllegalArgumentException(
                    "É necessário informar o responsável pela alteração."
            );
        }
        Responsavel responsavelPersistido = entityManager.find(
                Responsavel.class,
                responsavel.getId()
        );
        if (responsavelPersistido == null) {
            throw new EntityNotFoundException("Responsável pela alteração não encontrado.");
        }

        LocalDateTime inicioAnterior = reserva.getInicio();
        LocalDateTime fimAnterior = reserva.getFim();
        Instant aplicadoEm = clock.instant();
        AlteracaoHorarioAplicada alteracao = new AlteracaoHorarioAplicada(
                reserva,
                responsavelPersistido,
                inicioAnterior,
                fimAnterior,
                novoInicio,
                novoFim,
                aplicadoEm
        );

        int reservasAtualizadas = reservaRepository.atualizarHorarioAprovado(
                reservaId,
                StatusReserva.APROVADA,
                novoInicio,
                novoFim
        );
        if (reservasAtualizadas != 1) {
            throw new ReservaIndisponivelException(
                    "A reserva deixou de estar disponível para alteração."
            );
        }

        entityManager.refresh(reserva);
        alteracaoHorarioAplicadaRepository.save(alteracao);
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<AlteracaoHorarioAplicada> consultarHistorico(Long reservaId) {
        ContextoAutenticacao.exigirEquipe();
        if (!reservaRepository.existsById(reservaId)) {
            throw new EntityNotFoundException("Reserva não encontrada.");
        }
        return alteracaoHorarioAplicadaRepository
                .findAllByReserva_IdOrderByAppliedAtAscIdAsc(reservaId);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaReserva> consultarAuditoria(Long reservaId) {
        ContextoAutenticacao.exigirEquipe();
        if (!reservaRepository.existsById(reservaId)) {
            throw new EntityNotFoundException("Reserva não encontrada.");
        }
        return auditoriaReservaRepository.findByReservaIdOrderByDataHoraAsc(reservaId);
    }

    @Transactional
    public Reserva iniciarUtilizacao(Long reservaId) {
        return transicionarReserva(reservaId, StatusReserva.EM_USO, PerfilUsuario.RESPONSAVEL);
    }

    @Transactional
    public Reserva concluirUtilizacao(Long reservaId) {
        return transicionarReserva(reservaId, StatusReserva.CONCLUIDA, PerfilUsuario.RESPONSAVEL);
    }

    @Transactional
    public Reserva registrarNaoComparecimento(Long reservaId) {
        return transicionarReserva(
                reservaId, StatusReserva.NAO_COMPARECEU, PerfilUsuario.RESPONSAVEL
        );
    }

    @Transactional(noRollbackFor = ReservaExpiradaException.class)
    public Reserva aprovar(Long reservaId, Responsavel responsavel) {
        Reserva reserva = buscarReservaComSalaBloqueada(reservaId);
        StatusReserva anterior = reserva.getStatus();

        if (reserva.getStatus() == StatusReserva.SOLICITADA
                && !reserva.getInicio().isAfter(LocalDateTime.now(clock))) {
            reserva.setStatus(StatusReserva.EXPIRADA);
            reservaRepository.save(reserva);
            entityManager.flush();
            throw new ReservaExpiradaException(
                    "A solicitação expirou antes de ser aprovada."
            );
        }

        Sala sala = bloquearRecursosDaReserva(reserva);
        if (sala.getStatusSala() != StatusSala.DISPONIVEL) {
            throw new ReservaIndisponivelException(
                    "O recurso ou horário não está mais disponível para esta reserva."
            );
        }
        reservaRepository.findAllBySalaIdForUpdate(sala.getId());
        validarAusenciaDeConflito(
                sala.getId(),
                idsProfessores(reserva),
                idsMateriais(reserva),
                reserva.getInicio(),
                reserva.getFim(),
                reserva.getId(),
                STATUS_CONFIRMADOS
        );

        reserva.aprovar(responsavel);
        Reserva aprovada = reservaRepository.save(reserva);
        registrarAuditoria(
                aprovada,
                anterior,
                StatusReserva.APROVADA,
                responsavel.getId(),
                PerfilUsuario.RESPONSAVEL
        );
        notificarMudancaDeEstado(aprovada);
        return aprovada;
    }

    private void notificarMudancaDeEstado(Reserva reserva) {
        NotificacaoReserva notificacao = new NotificacaoReserva(
                reserva.getId(),
                reserva.getSala().getId(),
                reserva.getSolicitante().getId(),
                reserva.getStatus(),
                reserva.getInicio(),
                reserva.getFim()
        );
        try {
            notificacaoClient.notificar(notificacao);
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Falha ao notificar a reserva {}; estado atual {} foi mantido.",
                    reserva.getId(),
                    reserva.getStatus(),
                    exception
            );
        }
    }

    private Reserva transicionarReserva(
            Long reservaId,
            StatusReserva novoEstado,
            PerfilUsuario perfilPadrao
    ) {
        UsuarioAutenticado usuario = ContextoAutenticacao.exigirResponsavelOuAdministrador();
        Reserva reserva = buscarReservaBloqueada(reservaId);
        validarResponsavelOperacional(reserva, usuario);
        StatusReserva anterior = reserva.getStatus();

        switch (novoEstado) {
            case EM_USO -> reserva.iniciarUtilizacao();
            case CONCLUIDA -> reserva.concluirUtilizacao();
            case NAO_COMPARECEU -> reserva.registrarNaoComparecimento();
            default -> throw new IllegalArgumentException("Estado operacional inválido.");
        }

        Reserva atualizada = reservaRepository.save(reserva);
        registrarAuditoria(
                atualizada,
                anterior,
                novoEstado,
                usuario.id(),
                usuario.perfil() == null ? perfilPadrao : usuario.perfil()
        );
        return atualizada;
    }

    private void validarResponsavelOperacional(
            Reserva reserva,
            UsuarioAutenticado usuario
    ) {
        if (usuario.perfil() == PerfilUsuario.RESPONSAVEL
                && !Objects.equals(
                        reserva.getResponsavel() == null
                                ? null
                                : reserva.getResponsavel().getId(),
                        usuario.id()
                )) {
            throw new AcessoNegadoException(
                    "Somente o responsável pela reserva pode executar esta operação."
            );
        }
    }

    private void registrarAuditoria(
            Reserva reserva,
            StatusReserva estadoAnterior,
            StatusReserva novoEstado,
            Long usuarioIdPadrao,
            PerfilUsuario perfilPadrao
    ) {
        UsuarioAutenticado usuario = ContextoAutenticacao.usuarioAtual();
        boolean eventoSistema = perfilPadrao == PerfilUsuario.SISTEMA;
        Long usuarioId = eventoSistema || usuario == null ? usuarioIdPadrao : usuario.id();
        PerfilUsuario perfil = eventoSistema || usuario == null || usuario.perfil() == null
                ? perfilPadrao
                : usuario.perfil();
        auditoriaReservaRepository.save(new AuditoriaReserva(
                null,
                reserva,
                estadoAnterior,
                novoEstado,
                usuarioId,
                perfil,
                LocalDateTime.now(clock)
        ));
    }

    private PerfilUsuario perfilAprovador(TipoAprovadorReserva tipoAprovador) {
        return tipoAprovador == TipoAprovadorReserva.RESPONSAVEL
                ? PerfilUsuario.RESPONSAVEL
                : PerfilUsuario.ADMINISTRADOR;
    }

    private Reserva buscarReservaBloqueada(Long reservaId) {
        return reservaRepository.buscarPorIdComBloqueio(reservaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reserva não encontrada: " + reservaId
                ));
    }

    private Reserva buscarReservaComSalaBloqueada(Long reservaId) {
        Long salaId = reservaRepository.findSalaIdByReservaId(reservaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reserva não encontrada: " + reservaId
                ));
        buscarSalaBloqueada(salaId);
        return buscarReservaBloqueada(reservaId);
    }

    private Sala buscarSalaBloqueada(Long salaId) {
        return salaRepository.buscarPorIdComBloqueio(salaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sala não encontrada: " + salaId
                ));
    }

    private Sala bloquearRecursosDaReserva(Reserva reserva) {
        Sala sala = buscarSalaBloqueada(reserva.getSala().getId());
        buscarProfessoresBloqueados(idsProfessores(reserva));
        buscarMateriaisBloqueados(idsMateriais(reserva));
        return sala;
    }

    private Set<Long> idsProfessores(Reserva reserva) {
        return reserva.getProfessores().stream()
                .map(Solicitante::getId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Long> idsMateriais(Reserva reserva) {
        return reserva.getMateriais().stream()
                .map(Material::getId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Solicitante> buscarProfessoresBloqueados(Set<Long> professorIds) {
        Set<Solicitante> professores = new LinkedHashSet<>();
        for (Long professorId : professorIds) {
            Solicitante professor = solicitanteRepository.buscarProfessorPorIdComBloqueio(
                            professorId,
                            TipoSolicitante.PROFESSOR
                    )
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Professor não encontrado: " + professorId
                    ));
            professores.add(professor);
        }
        return professores;
    }

    private Set<Material> buscarMateriaisBloqueados(List<Long> materialIds) {
        return buscarMateriaisBloqueados(
                new TreeSet<>(materialIds == null ? List.of() : materialIds)
        );
    }

    private Set<Material> buscarMateriaisBloqueados(Set<Long> materialIds) {
        Set<Material> materiais = new LinkedHashSet<>();
        for (Long materialId : new TreeSet<>(materialIds)) {
            Material material = materialRepository.buscarPorIdComBloqueio(materialId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Material não encontrado: " + materialId
                    ));
            materiais.add(material);
        }
        return materiais;
    }

    private Responsavel buscarResponsavel(Long responsavelId) {
        return responsavelRepository.findById(responsavelId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Responsável não encontrado: " + responsavelId
                ));
    }

    private Administrador buscarAdministrador(Long administradorId) {
        return administradorRepository.findById(administradorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Administrador não encontrado: " + administradorId
                ));
    }

    private void validarSolicitante(Reserva reserva, Long solicitanteId) {
        if (!Objects.equals(reserva.getSolicitante().getId(), solicitanteId)) {
            throw new AcessoNegadoException(
                    "O solicitante não tem permissão para alterar esta reserva."
            );
        }
    }

    private void validarDisponibilidadeDaSala(Sala sala) {
        if (sala.getStatusSala() != StatusSala.DISPONIVEL) {
            throw new IllegalStateException(
                    "A sala não está disponível para novas reservas."
            );
        }
    }

    private void validarAusenciaDeConflito(
            Long salaId,
            Set<Long> professorIds,
            Set<Long> materialIds,
            LocalDateTime inicio,
            LocalDateTime fim,
            Long reservaId,
            Set<StatusReserva> estadosOcupados
    ) {
        if (reservaRepository.contarConflitos(
                salaId, estadosOcupados, inicio, fim, reservaId
        ) > 0) {
            throw new ReservaIndisponivelException(
                    "Já existe uma reserva para esta sala no intervalo informado."
            );
        }
        for (Long professorId : professorIds) {
            if (reservaRepository.contarConflitosPorProfessor(
                    professorId, estadosOcupados, inicio, fim, reservaId
            ) > 0) {
                throw new ReservaIndisponivelException(
                        "Já existe uma reserva para este professor no intervalo informado."
                );
            }
        }
        for (Long materialId : materialIds) {
            if (reservaRepository.contarConflitosPorMaterial(
                    materialId, estadosOcupados, inicio, fim, reservaId
            ) > 0) {
                throw new ReservaIndisponivelException(
                        "Já existe uma reserva para este material no intervalo informado."
                );
            }
        }
    }

    private void validarIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new IllegalArgumentException("O término deve ser posterior ao início.");
        }
        if (!inicio.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("O início deve estar no futuro.");
        }
        if (!minutosValidos(inicio) || !minutosValidos(fim)) {
            throw new IllegalArgumentException(
                    "Os horários devem respeitar incrementos de 30 minutos."
            );
        }
        if (!segundosENanosZerados(inicio) || !segundosENanosZerados(fim)) {
            throw new IllegalArgumentException("Os segundos e nanos devem ser zerados.");
        }
    }

    private boolean minutosValidos(LocalDateTime horario) {
        return horario.getMinute() == 0 || horario.getMinute() == 30;
    }

    private boolean segundosENanosZerados(LocalDateTime horario) {
        return horario.getSecond() == 0 && horario.getNano() == 0;
    }

    private void expirarSolicitacoesVencidas() {
        List<Reserva> vencidas = reservaRepository.buscarSolicitacoesVencidasComBloqueio(
                LocalDateTime.now(clock),
                StatusReserva.SOLICITADA
        );
        for (Reserva reserva : vencidas) {
            reserva.setStatus(StatusReserva.EXPIRADA);
            Reserva expirada = reservaRepository.save(reserva);
            registrarAuditoria(
                    expirada,
                    StatusReserva.SOLICITADA,
                    StatusReserva.EXPIRADA,
                    null,
                    PerfilUsuario.SISTEMA
            );
        }
    }

    private boolean existeConflito(Reserva reserva, List<Reserva> reservasDaSala) {
        return existeConflito(
                reserva,
                reserva.getInicio(),
                reserva.getFim(),
                reservasDaSala
        );
    }

    private boolean existeConflito(
            Reserva reserva,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<Reserva> reservasDaSala
    ) {
        return reservasDaSala.stream().anyMatch(outra ->
                !Objects.equals(outra.getId(), reserva.getId())
                        && STATUS_QUE_OCUPAM_HORARIO.contains(outra.getStatus())
                        && inicio.isBefore(outra.getFim())
                        && fim.isAfter(outra.getInicio())
        );
    }

    private void validarNovoIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new ReservaHorarioInvalidoException(
                    "O término deve ser posterior ao início."
            );
        }
        if (!inicio.isAfter(LocalDateTime.now(clock))) {
            throw new ReservaHorarioInvalidoException(
                    "O novo horário deve iniciar no futuro."
            );
        }
        if (inicio.getMinute() % 30 != 0 || fim.getMinute() % 30 != 0) {
            throw new ReservaHorarioInvalidoException(
                    "Os horários devem respeitar incrementos de 30 minutos."
            );
        }
        if (!segundosENanosZerados(inicio) || !segundosENanosZerados(fim)) {
            throw new ReservaHorarioInvalidoException(
                    "Os segundos e nanos devem ser zerados."
            );
        }
    }

    public static class ReservaHorarioInvalidoException extends IllegalArgumentException {

        public ReservaHorarioInvalidoException(String message) {
            super(message);
        }
    }

    public static class ReservaIndisponivelException extends IllegalStateException {

        public ReservaIndisponivelException(String message) {
            super(message);
        }
    }

    public static class ReservaExpiradaException extends IllegalStateException {

        public ReservaExpiradaException(String message) {
            super(message);
        }
    }
}
