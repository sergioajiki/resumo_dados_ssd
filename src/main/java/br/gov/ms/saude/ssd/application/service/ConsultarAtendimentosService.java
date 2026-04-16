package br.gov.ms.saude.ssd.application.service;

import br.gov.ms.saude.ssd.adapter.in.rest.dto.AtendimentoDTO;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.AtendimentoEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.AtendimentoRepository;
import br.gov.ms.saude.ssd.application.usecase.ConsultarAtendimentosUseCase;
import br.gov.ms.saude.ssd.domain.model.AtendimentoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço de aplicação que implementa {@link ConsultarAtendimentosUseCase}.
 *
 * <p>Lê os atendimentos armazenados no banco H2 local e os converte para
 * {@link AtendimentoDTO} para exposição pela API REST. Não acessa a fonte Qlik
 * diretamente — trabalha exclusivamente sobre os dados já sincronizados pelo
 * pipeline ETL.</p>
 *
 * <p>A conversão entidade → DTO é feita inline por um método privado
 * ({@link #toDto(AtendimentoEntity)}), mantendo o código simples sem
 * dependência de MapStruct para este caso de uso direto.</p>
 *
 * @see AtendimentoRepository
 * @see LoaderService
 */
@Service
@Transactional(readOnly = true)
public class ConsultarAtendimentosService implements ConsultarAtendimentosUseCase {

    private final AtendimentoRepository atendimentoRepository;

    /**
     * Injeta o repositório via construtor.
     *
     * @param atendimentoRepository repositório Spring Data dos atendimentos
     */
    public ConsultarAtendimentosService(AtendimentoRepository atendimentoRepository) {
        this.atendimentoRepository = atendimentoRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delega para {@link AtendimentoRepository#findByFiltros} com todos os
     * campos do filtro. Campos {@code null} são ignorados pela query JPQL.</p>
     */
    @Override
    public Page<AtendimentoDTO> consultar(AtendimentoFilter filter, Pageable pageable) {
        return atendimentoRepository.findByFiltros(
                filter.municipio(),
                filter.dataInicio(),
                filter.dataFim(),
                filter.especialidade(),
                filter.statusConsulta(),
                filter.faixaEtaria(),
                filter.racaPaciente(),
                pageable
        ).map(this::toDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AtendimentoDTO> buscarPorId(Long id) {
        return atendimentoRepository.findById(id).map(this::toDto);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Converte a lista de arrays {@code [municipio, count]} retornada pelo
     * repository em um mapa ordenado por contagem decrescente.</p>
     */
    @Override
    public Map<String, Long> contarPorMunicipio(AtendimentoFilter filter) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        atendimentoRepository.countByMunicipio(
                filter.municipio(), filter.dataInicio(), filter.dataFim(),
                filter.especialidade(), filter.statusConsulta()
        ).forEach(row -> resultado.put(
                row[0] == null ? "Não informado" : row[0].toString(),
                ((Number) row[1]).longValue()
        ));
        return resultado;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Long> contarPorEspecialidade(AtendimentoFilter filter) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        atendimentoRepository.countByEspecialidade(
                filter.municipio(), filter.dataInicio(), filter.dataFim()
        ).forEach(row -> resultado.put(
                row[0] == null ? "Não informado" : row[0].toString(),
                ((Number) row[1]).longValue()
        ));
        return resultado;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Long> contarPorStatus(AtendimentoFilter filter) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        atendimentoRepository.countByStatus(
                filter.municipio(), filter.dataInicio(), filter.dataFim()
        ).forEach(row -> resultado.put(
                row[0] == null ? "Não informado" : row[0].toString(),
                ((Number) row[1]).longValue()
        ));
        return resultado;
    }

    // -------------------------------------------------------------------------
    // Conversão entidade → DTO
    // -------------------------------------------------------------------------

    /**
     * Converte uma {@link AtendimentoEntity} para {@link AtendimentoDTO}.
     *
     * <p>Os campos {@code cboCodigo} e {@code especialidade} do DTO são preenchidos
     * a partir de {@code cboMedico} e {@code especialidade} da entidade respectivamente.
     * O campo {@code faixaEtaria} usa o valor já calculado e persistido pelo ETL.</p>
     *
     * @param e entidade JPA lida do banco H2
     * @return DTO pronto para serialização JSON pela API REST
     */
    private AtendimentoDTO toDto(AtendimentoEntity e) {
        return new AtendimentoDTO(
                e.getId(),
                e.getCnsPaciente(),
                e.getDtNascimento(),
                e.getFaixaEtaria(),
                e.getRaca(),
                e.getEtnia(),
                e.getMunicipio(),
                e.getIbge(),
                e.getDtAgendamento(),
                e.getHrAgendamento(),
                e.getNomeMedico(),
                e.getCboMedico(),
                e.getEspecialidade(),
                e.getStatusConsulta(),
                e.getClassifConclusao(),
                e.getDesfecho(),
                e.getCid(),
                e.getDtSolicitacao(),
                e.getTipoZona(),
                e.getTipoServico(),
                e.getDtCriacao(),
                e.getCnesEstabelecimento(),
                e.getIdEstabelecimento(),
                e.getIdMedico(),
                e.getClassificCor(),
                e.getTpNwConclusao(),
                e.getIdDigsaudeRef(),
                e.getTelefone(),
                e.getCepPaciente(),
                e.getRuaPaciente(),
                e.getNumPaciente(),
                e.getBairroPaciente(),
                e.getComplementoEnd(),
                e.getDescricaoEndereco(),
                e.getEnderecoCompleto(),
                e.getDescricaoConsulta()
        );
    }
}
