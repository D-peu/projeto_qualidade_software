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
import br.com.projeto_qualidade_software.projeto.security.PerfilUsuario;
import br.com.projeto_qualidade_software.projeto.security.UsuarioAutenticado;
import br.com.projeto_qualidade_software.projeto.exceptions.AcessoNegadoException;
import br.com.projeto_qualidade_software.projeto.repositories.AdministradorRepository;
import br.com.projeto_qualidade_software.projeto.repositories.AuditoriaReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.MaterialRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ReservaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.ResponsavelRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SalaRepository;
import br.com.projeto_qualidade_software.projeto.repositories.SolicitanteRepository;
import br.com.projeto_qualidade_software.projeto.security.ContextoAutenticacao;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private static final Set<StatusReserva> STATUS_QUE_OCUPAM_SALA = EnumSet.of(
            StatusReserva.SOLICITADA,
            StatusReserva.APROVADA,
            StatusReserva.EM_USO,
            StatusReserva.CONCLUIDA,
            StatusReserva.NAO_COMPARECEU
    );
    private static final Set<StatusReserva> STATUS_QUE_BLOQUEIAM_APROVACAO = EnumSet.of(
            StatusReserva.APROVADA,
            StatusReserva.EM_USO,
            StatusReserva.CONCLUIDA,
            StatusReserva.NAO_COMPARECEU
    );

    private final ReservaRepository reservaRepository;
    private final AuditoriaReservaRepository auditoriaReservaRepository;
    private final SalaRepository salaRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final ResponsavelRepository responsavelRepository;
    private final AdministradorRepository administradorRepository;
    private final MaterialRepository materialRepository;
    private final Clock clock;

    public ReservaService(
            ReservaRepository reservaRepository,
            AuditoriaReservaRepository auditoriaReservaRepository,
            SalaRepository salaRepository,
            SolicitanteRepository solicitanteRepository,
            ResponsavelRepository responsavelRepository,
            AdministradorRepository administradorRepository,
            MaterialRepository materialRepository,
            Clock clock
    ) {
        this.reservaRepository = reservaRepository;
        this.auditoriaReservaRepository = auditoriaReservaRepository;
        this.salaRepository = salaRepository;
        this.solicitanteRepository = solicitanteRepository;
        this.responsavelRepository = responsavelRepository;
        this.administradorRepository = administradorRepository;
        this.materialRepository = materialRepository;
        this.clock = clock;
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
        ContextoAutenticacao.exigirSolicitante(solicitanteId, tipoSolicitante);
        UsuarioAutenticado ator = atorDaSolicitacao(solicitanteId, tipoSolicitante);
        validarIntervalo(inicio, fim);
        Sala sala = buscarSalaBloqueada(salaId);
        validarDisponibilidadeDaSala(sala);
        Solicitante solicitante = solicitanteRepository.findById(solicitanteId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Solicitante não encontrado: " + solicitanteId
                ));
        if (!Objects.equals(solicitante.getTipoSolicitante(), tipoSolicitante)) {
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
        validarAusenciaDeConflito(
                sala.getId(),
                idsProfessores,
                materiais.stream().map(Material::getId)
                        .collect(Collectors.toCollection(TreeSet::new)),
                inicio, fim, null
        );

        Reserva reserva = new Reserva();
        reserva.setSala(sala);
        reserva.setSolicitante(solicitante);
        reserva.setProfessores(professores);
        reserva.setMateriais(materiais);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setStatus(StatusReserva.SOLICITADA);
        LocalDateTime agora = LocalDateTime.now(clock);
        reserva.setSolicitadoEm(agora);
        Reserva salva = reservaRepository.save(reserva);
        registrarAuditoria(salva, null, StatusReserva.SOLICITADA, ator, agora);
        return salva;
    }

    @Transactional
    public Reserva alterarHorarioReserva(
            Long reservaId,
            Long solicitanteId,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        ContextoAutenticacao.exigirSolicitante(solicitanteId);
        validarIntervalo(inicio, fim);
        Reserva reserva = buscarReservaBloqueada(reservaId);
        validarSolicitante(reserva, solicitanteId);
        if (reserva.getStatus() != StatusReserva.SOLICITADA) {
            throw new IllegalStateException(
                    "Somente reservas solicitadas podem ter o horário alterado."
            );
        }

        Sala sala = buscarSalaBloqueada(reserva.getSala().getId());
        validarDisponibilidadeDaSala(sala);
        Set<Long> professorIds = new TreeSet<>();
        reserva.getProfessores().forEach(professor -> professorIds.add(professor.getId()));
        Set<Long> materialIds = new TreeSet<>();
        reserva.getMateriais().forEach(material -> materialIds.add(material.getId()));
        buscarProfessoresBloqueados(professorIds);
        buscarMateriaisBloqueados(materialIds);
        validarAusenciaDeConflito(
                sala.getId(), professorIds, materialIds, inicio, fim, reservaId
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

        StatusReserva estadoAnterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.CANCELADA);
        UsuarioAutenticado ator = atorDaSessaoOuSolicitante(solicitanteId);
        return salvarTransicao(reserva, estadoAnterior, ator);
    }

    @Transactional
    public Reserva aprovarReserva(
            Long reservaId,
            Long usuarioId,
            TipoAprovadorReserva tipoAprovador
    ) {
        ContextoAutenticacao.exigirAprovador(usuarioId, tipoAprovador);
        Reserva reserva = buscarReservaBloqueada(reservaId);
        UsuarioAutenticado ator = atorDoAprovador(usuarioId, tipoAprovador);
        Responsavel responsavel = null;
        Administrador administrador = null;
        if (tipoAprovador == TipoAprovadorReserva.RESPONSAVEL) {
            responsavel = buscarResponsavel(usuarioId);
        } else if (tipoAprovador == TipoAprovadorReserva.ADMINISTRADOR) {
            administrador = buscarAdministrador(usuarioId);
        } else {
            throw new AcessoNegadoException(
                    "Somente Responsável ou Administrador pode aprovar reservas."
            );
        }

        reserva.validarSolicitadaParaDecisao();
        Sala sala = buscarSalaBloqueada(reserva.getSala().getId());
        validarDisponibilidadeDaSala(sala);

        Set<Long> professorIds = new TreeSet<>();
        reserva.getProfessores().forEach(professor -> professorIds.add(professor.getId()));
        Set<Long> materialIds = new TreeSet<>();
        reserva.getMateriais().forEach(material -> materialIds.add(material.getId()));
        buscarProfessoresBloqueados(professorIds);
        buscarMateriaisBloqueados(materialIds);
        validarAusenciaDeConflito(
                sala.getId(),
                professorIds,
                materialIds,
                reserva.getInicio(),
                reserva.getFim(),
                reserva.getId(),
                STATUS_QUE_BLOQUEIAM_APROVACAO
        );

        StatusReserva estadoAnterior = reserva.getStatus();
        if (responsavel != null) {
            reserva.aprovar(responsavel);
        } else {
            reserva.aprovar(administrador);
        }
        return salvarTransicao(reserva, estadoAnterior, ator);
    }

    @Transactional
    public Reserva rejeitarReserva(
            Long reservaId,
            Long usuarioId,
            TipoAprovadorReserva tipoAprovador
    ) {
        ContextoAutenticacao.exigirAprovador(usuarioId, tipoAprovador);
        Reserva reserva = buscarReservaBloqueada(reservaId);
        UsuarioAutenticado ator = atorDoAprovador(usuarioId, tipoAprovador);
        StatusReserva estadoAnterior = reserva.getStatus();
        if (tipoAprovador == TipoAprovadorReserva.RESPONSAVEL) {
            reserva.rejeitar(buscarResponsavel(usuarioId));
        } else if (tipoAprovador == TipoAprovadorReserva.ADMINISTRADOR) {
            reserva.rejeitar(buscarAdministrador(usuarioId));
        } else {
            throw new AcessoNegadoException(
                    "Somente Responsável ou Administrador pode decidir reservas."
            );
        }
        return salvarTransicao(reserva, estadoAnterior, ator);
    }

    @Transactional
    public Reserva iniciarUtilizacao(Long reservaId) {
        UsuarioAutenticado ator = ContextoAutenticacao.exigirResponsavelOuAdministrador();
        Reserva reserva = buscarReservaBloqueada(reservaId);
        validarPermissaoOperacional(reserva, ator);
        reserva.validarPodeIniciarUtilizacao();

        LocalDateTime agora = LocalDateTime.now(clock);
        if (agora.isBefore(reserva.getInicio()) || !agora.isBefore(reserva.getFim())) {
            throw new IllegalStateException(
                    "A utilização só pode iniciar durante o intervalo reservado."
            );
        }

        StatusReserva estadoAnterior = reserva.getStatus();
        reserva.iniciarUtilizacao();
        return salvarTransicao(reserva, estadoAnterior, ator, agora);
    }

    @Transactional
    public Reserva concluirUtilizacao(Long reservaId) {
        UsuarioAutenticado ator = ContextoAutenticacao.exigirResponsavelOuAdministrador();
        Reserva reserva = buscarReservaBloqueada(reservaId);
        validarPermissaoOperacional(reserva, ator);
        reserva.validarPodeConcluirUtilizacao();

        StatusReserva estadoAnterior = reserva.getStatus();
        reserva.concluirUtilizacao();
        return salvarTransicao(reserva, estadoAnterior, ator);
    }

    @Transactional
    public Reserva registrarNaoComparecimento(Long reservaId) {
        UsuarioAutenticado ator = ContextoAutenticacao.exigirResponsavelOuAdministrador();
        Reserva reserva = buscarReservaBloqueada(reservaId);
        validarPermissaoOperacional(reserva, ator);
        reserva.validarPodeRegistrarNaoComparecimento();

        LocalDateTime agora = LocalDateTime.now(clock);
        if (agora.isBefore(reserva.getFim())) {
            throw new IllegalStateException(
                    "O não comparecimento só pode ser registrado após o fim reservado."
            );
        }

        StatusReserva estadoAnterior = reserva.getStatus();
        reserva.registrarNaoComparecimento();
        return salvarTransicao(reserva, estadoAnterior, ator, agora);
    }

    @Transactional(readOnly = true)
    public Reserva buscarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reserva não encontrada: " + reservaId
                ));
        ContextoAutenticacao.exigirAcessoReserva(reserva);
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarReservasDoSolicitante(Long solicitanteId) {
        ContextoAutenticacao.exigirAcessoListaSolicitante(solicitanteId);
        return reservaRepository.findBySolicitanteIdOrderByInicioDesc(solicitanteId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarReservas(StatusReserva status) {
        ContextoAutenticacao.exigirEquipe();
        return reservaRepository.findByStatusOrderBySolicitadoEmAsc(status);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarReservasPendentes() {
        return listarReservas(StatusReserva.SOLICITADA);
    }

    private void validarPermissaoOperacional(Reserva reserva, UsuarioAutenticado ator) {
        if (ator.perfil() == PerfilUsuario.RESPONSAVEL
                && (reserva.getResponsavel() == null
                || !Objects.equals(reserva.getResponsavel().getId(), ator.id()))) {
            throw new AcessoNegadoException(
                    "O Responsável só pode operar reservas sob sua responsabilidade."
            );
        }
    }

    private UsuarioAutenticado atorDaSolicitacao(
            Long solicitanteId,
            TipoSolicitante tipoSolicitante
    ) {
        UsuarioAutenticado ator = ContextoAutenticacao.usuarioAtual();
        return ator == null
                ? new UsuarioAutenticado(
                        solicitanteId, null, null, PerfilUsuario.SOLICITANTE, tipoSolicitante
                )
                : ator;
    }

    private UsuarioAutenticado atorDaSessaoOuSolicitante(Long solicitanteId) {
        UsuarioAutenticado ator = ContextoAutenticacao.usuarioAtual();
        return ator == null
                ? new UsuarioAutenticado(
                        solicitanteId, null, null, PerfilUsuario.SOLICITANTE, null
                )
                : ator;
    }

    private UsuarioAutenticado atorDoAprovador(
            Long usuarioId,
            TipoAprovadorReserva tipoAprovador
    ) {
        if (tipoAprovador == null) {
            throw new AcessoNegadoException(
                    "Somente Responsável ou Administrador pode decidir reservas."
            );
        }
        PerfilUsuario perfil = switch (tipoAprovador) {
            case RESPONSAVEL -> PerfilUsuario.RESPONSAVEL;
            case ADMINISTRADOR -> PerfilUsuario.ADMINISTRADOR;
        };
        UsuarioAutenticado ator = ContextoAutenticacao.usuarioAtual();
        return ator == null
                ? new UsuarioAutenticado(usuarioId, null, null, perfil, null)
                : ator;
    }

    private Reserva salvarTransicao(
            Reserva reserva,
            StatusReserva estadoAnterior,
            UsuarioAutenticado ator
    ) {
        return salvarTransicao(reserva, estadoAnterior, ator, LocalDateTime.now(clock));
    }

    private Reserva salvarTransicao(
            Reserva reserva,
            StatusReserva estadoAnterior,
            UsuarioAutenticado ator,
            LocalDateTime dataHora
    ) {
        Reserva salva = reservaRepository.save(reserva);
        registrarAuditoria(salva, estadoAnterior, salva.getStatus(), ator, dataHora);
        return salva;
    }

    private void registrarAuditoria(
            Reserva reserva,
            StatusReserva estadoAnterior,
            StatusReserva novoEstado,
            UsuarioAutenticado ator,
            LocalDateTime dataHora
    ) {
        auditoriaReservaRepository.save(new AuditoriaReserva(
                null,
                reserva,
                estadoAnterior,
                novoEstado,
                ator.id(),
                ator.perfil(),
                dataHora
        ));
    }

    private Reserva buscarReservaBloqueada(Long reservaId) {
        return reservaRepository.buscarPorIdComBloqueio(reservaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reserva não encontrada: " + reservaId
                ));
    }

    private Sala buscarSalaBloqueada(Long salaId) {
        return salaRepository.buscarPorIdComBloqueio(salaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sala não encontrada: " + salaId
                ));
    }

    private Set<Solicitante> buscarProfessoresBloqueados(Set<Long> professorIds) {
        Set<Solicitante> professores = new LinkedHashSet<>();
        for (Long professorId : professorIds) {
            Solicitante professor = solicitanteRepository
                    .buscarProfessorPorIdComBloqueio(
                            professorId, TipoSolicitante.PROFESSOR
                    )
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Professor não encontrado: " + professorId
                    ));
            professores.add(professor);
        }
        return professores;
    }

    private Set<Material> buscarMateriaisBloqueados(List<Long> materialIds) {
        Set<Long> idsOrdenados = new TreeSet<>(
                materialIds == null ? List.of() : materialIds
        );
        return buscarMateriaisBloqueados(idsOrdenados);
    }

    private Set<Material> buscarMateriaisBloqueados(Set<Long> materialIds) {
        Set<Material> materiais = new LinkedHashSet<>();
        for (Long materialId : materialIds) {
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
            Long reservaId
    ) {
        validarAusenciaDeConflito(
                salaId, professorIds, materialIds, inicio, fim, reservaId,
                STATUS_QUE_OCUPAM_SALA
        );
    }

    private void validarAusenciaDeConflito(
            Long salaId,
            Set<Long> professorIds,
            Set<Long> materialIds,
            LocalDateTime inicio,
            LocalDateTime fim,
            Long reservaId,
            Set<StatusReserva> statusQueConflitam
    ) {
        if (reservaRepository.contarConflitos(
                salaId,
                statusQueConflitam,
                inicio,
                fim,
                reservaId
        ) > 0) {
            throw new IllegalStateException(
                    "Já existe uma reserva para esta sala no intervalo informado."
            );
        }
        for (Long professorId : professorIds) {
            if (reservaRepository.contarConflitosPorProfessor(
                    professorId,
                    statusQueConflitam,
                    inicio,
                    fim,
                    reservaId
            ) > 0) {
                throw new IllegalStateException(
                        "Já existe uma reserva para este professor no intervalo informado."
                );
            }
        }
        for (Long materialId : materialIds) {
            if (reservaRepository.contarConflitosPorMaterial(
                    materialId,
                    statusQueConflitam,
                    inicio,
                    fim,
                    reservaId
            ) > 0) {
                throw new IllegalStateException(
                        "Já existe uma reserva para este material no intervalo informado."
                );
            }
        }
    }

    private void validarIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException(
                    "O início e o término são obrigatórios."
            );
        }
        if (!inicio.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException(
                    "A data e hora de início devem estar no futuro."
            );
        }
        if (inicio.getMinute() % 30 != 0 || fim.getMinute() % 30 != 0) {
            throw new IllegalArgumentException(
                    "O início e o término devem estar em incrementos de 30 minutos "
                            + "(minutos 00 ou 30)."
            );
        }
        if (inicio.getSecond() != 0 || inicio.getNano() != 0
                || fim.getSecond() != 0 || fim.getNano() != 0) {
            throw new IllegalArgumentException(
                    "O início e o término devem ter segundos e nanos zerados."
            );
        }
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException(
                    "O término deve ser posterior ao início."
            );
        }
    }
}