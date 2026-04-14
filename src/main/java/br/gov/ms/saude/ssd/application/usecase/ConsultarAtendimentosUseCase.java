package br.gov.ms.saude.ssd.application.usecase;

import br.gov.ms.saude.ssd.adapter.in.rest.dto.AtendimentoDTO;
import br.gov.ms.saude.ssd.domain.model.AtendimentoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

/**
 * Porta de entrada (use case) para consulta de atendimentos do Núcleo de Telessaúde MS.
 *
 * <p>Define o contrato para recuperação dos atendimentos armazenados no repositório local H2,
 * com suporte a filtragem multidimensional (município, período, especialidade, raça, etc.),
 * paginação nativa via Spring Data e agregações por dimensão para geração de indicadores.</p>
 *
 * <p>Implementado pela classe de serviço na camada de aplicação e consumido pelos
 * adaptadores de entrada (controllers REST e views Thymeleaf). Segue o princípio
 * da Inversão de Dependência: os adaptadores dependem desta abstração, nunca da
 * implementação concreta.</p>
 *
 * @see AtendimentoFilter
 * @see AtendimentoDTO
 */
public interface ConsultarAtendimentosUseCase {

    /**
     * Consulta atendimentos aplicando os critérios de filtro fornecidos, com paginação.
     *
     * <p>Campos {@code null} no {@link AtendimentoFilter} são ignorados —
     * use {@link AtendimentoFilter#vazio()} para retornar todos os atendimentos
     * sem restrição de filtro.</p>
     *
     * @param filter   critérios de filtragem (município, período, especialidade, etc.);
     *                 use {@link AtendimentoFilter#vazio()} para sem filtros
     * @param pageable configuração de paginação e ordenação (Spring Data)
     * @return página de {@link AtendimentoDTO} com os atendimentos que atendem aos critérios
     */
    Page<AtendimentoDTO> consultar(AtendimentoFilter filter, Pageable pageable);

    /**
     * Busca um atendimento específico pelo seu identificador único no repositório local.
     *
     * @param id identificador do atendimento no banco H2 local
     * @return {@link Optional} com o {@link AtendimentoDTO} correspondente,
     *         ou {@link Optional#empty()} se o atendimento não existir
     */
    Optional<AtendimentoDTO> buscarPorId(Long id);

    /**
     * Conta os atendimentos agrupados por município, aplicando os filtros fornecidos.
     *
     * <p>Retorna um mapa onde a chave é o nome do município e o valor é a quantidade
     * de atendimentos naquele município que atendem aos critérios do filtro.
     * Municípios com zero atendimentos não aparecem no resultado.</p>
     *
     * @param filter critérios de filtragem a aplicar antes da agregação;
     *               use {@link AtendimentoFilter#vazio()} para agregar todos os atendimentos
     * @return mapa de {@code nomeMunicipio → quantidade}, nunca {@code null}
     */
    Map<String, Long> contarPorMunicipio(AtendimentoFilter filter);

    /**
     * Conta os atendimentos agrupados por especialidade médica, aplicando os filtros fornecidos.
     *
     * <p>Retorna um mapa onde a chave é o nome da especialidade e o valor é a quantidade
     * de atendimentos naquela especialidade que atendem aos critérios do filtro.
     * Especialidades com zero atendimentos não aparecem no resultado.</p>
     *
     * @param filter critérios de filtragem a aplicar antes da agregação;
     *               use {@link AtendimentoFilter#vazio()} para agregar todos os atendimentos
     * @return mapa de {@code especialidade → quantidade}, nunca {@code null}
     */
    Map<String, Long> contarPorEspecialidade(AtendimentoFilter filter);

    /**
     * Conta os atendimentos agrupados por status da consulta, aplicando os filtros fornecidos.
     *
     * <p>Retorna um mapa onde a chave é o status da consulta (ex: "Realizado", "Cancelado")
     * e o valor é a quantidade de atendimentos naquele status que atendem aos critérios.
     * Útil para geração de indicadores operacionais de produtividade.</p>
     *
     * @param filter critérios de filtragem a aplicar antes da agregação;
     *               use {@link AtendimentoFilter#vazio()} para agregar todos os atendimentos
     * @return mapa de {@code statusConsulta → quantidade}, nunca {@code null}
     */
    Map<String, Long> contarPorStatus(AtendimentoFilter filter);
}
