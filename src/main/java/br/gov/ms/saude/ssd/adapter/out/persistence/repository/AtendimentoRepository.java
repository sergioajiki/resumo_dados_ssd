package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.AtendimentoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Repositório Spring Data para a entidade {@link AtendimentoEntity}.
 *
 * <p>Fornece acesso ao banco H2 para leitura e escrita de atendimentos.
 * Os métodos de consulta filtrada são usados pelo {@code ConsultarAtendimentosService}
 * para implementar os endpoints da API REST com filtros opcionais.</p>
 *
 * <p>Princípio aplicado: DIP — o serviço de aplicação depende desta interface,
 * nunca de uma implementação concreta de persistência.</p>
 */
@Repository
public interface AtendimentoRepository extends JpaRepository<AtendimentoEntity, Long> {

    /**
     * Consulta paginada de atendimentos com filtros opcionais.
     *
     * <p>Parâmetros {@code null} são ignorados pela cláusula JPQL — o {@code IS NULL OR}
     * garante que campos não informados não restrinjam o resultado.</p>
     *
     * @param municipio      filtro por nome do município (parcial, case-insensitive); {@code null} para ignorar
     * @param dataInicio     filtro por data de agendamento a partir desta data; {@code null} para ignorar
     * @param dataFim        filtro por data de agendamento até esta data; {@code null} para ignorar
     * @param especialidade  filtro por especialidade médica (parcial, case-insensitive); {@code null} para ignorar
     * @param statusConsulta filtro por status da consulta; {@code null} para ignorar
     * @param faixaEtaria    filtro por faixa etária do paciente; {@code null} para ignorar
     * @param racaPaciente   filtro por raça/cor do paciente; {@code null} para ignorar
     * @param pageable       configuração de paginação e ordenação
     * @return página de entidades que atendem aos critérios informados
     */
    @Query("""
            SELECT a FROM AtendimentoEntity a
            WHERE (:municipio IS NULL OR LOWER(a.municipio) LIKE LOWER(CONCAT('%', :municipio, '%')))
              AND (:dataInicio IS NULL OR CAST(a.dtAgendamento AS localdate) >= :dataInicio)
              AND (:dataFim IS NULL OR CAST(a.dtAgendamento AS localdate) <= :dataFim)
              AND (:especialidade IS NULL OR LOWER(a.especialidade) LIKE LOWER(CONCAT('%', :especialidade, '%')))
              AND (:statusConsulta IS NULL OR a.statusConsulta = :statusConsulta)
              AND (:faixaEtaria IS NULL OR a.faixaEtaria = :faixaEtaria)
              AND (:racaPaciente IS NULL OR a.raca = :racaPaciente)
            """)
    Page<AtendimentoEntity> findByFiltros(
            @Param("municipio") String municipio,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("especialidade") String especialidade,
            @Param("statusConsulta") String statusConsulta,
            @Param("faixaEtaria") String faixaEtaria,
            @Param("racaPaciente") String racaPaciente,
            Pageable pageable);

    /**
     * Conta atendimentos agrupados por município, com filtros opcionais.
     *
     * <p>Retorna lista de arrays {@code [nomeMunicipio, count]} para construção
     * do mapa de indicadores no serviço de aplicação.</p>
     *
     * @param municipio      filtro por município; {@code null} para todos
     * @param dataInicio     filtro por data início; {@code null} para sem limite inferior
     * @param dataFim        filtro por data fim; {@code null} para sem limite superior
     * @param especialidade  filtro por especialidade; {@code null} para todas
     * @param statusConsulta filtro por status; {@code null} para todos
     * @return lista de pares [municipio, total]
     */
    @Query("""
            SELECT a.municipio, COUNT(a) FROM AtendimentoEntity a
            WHERE (:municipio IS NULL OR LOWER(a.municipio) LIKE LOWER(CONCAT('%', :municipio, '%')))
              AND (:dataInicio IS NULL OR CAST(a.dtAgendamento AS localdate) >= :dataInicio)
              AND (:dataFim IS NULL OR CAST(a.dtAgendamento AS localdate) <= :dataFim)
              AND (:especialidade IS NULL OR LOWER(a.especialidade) LIKE LOWER(CONCAT('%', :especialidade, '%')))
              AND (:statusConsulta IS NULL OR a.statusConsulta = :statusConsulta)
            GROUP BY a.municipio
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByMunicipio(
            @Param("municipio") String municipio,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("especialidade") String especialidade,
            @Param("statusConsulta") String statusConsulta);

    /**
     * Conta atendimentos agrupados por especialidade, com filtros opcionais.
     *
     * @param municipio  filtro por município; {@code null} para todos
     * @param dataInicio filtro por data início; {@code null} para sem limite inferior
     * @param dataFim    filtro por data fim; {@code null} para sem limite superior
     * @return lista de pares [especialidade, total]
     */
    @Query("""
            SELECT a.especialidade, COUNT(a) FROM AtendimentoEntity a
            WHERE (:municipio IS NULL OR LOWER(a.municipio) LIKE LOWER(CONCAT('%', :municipio, '%')))
              AND (:dataInicio IS NULL OR CAST(a.dtAgendamento AS localdate) >= :dataInicio)
              AND (:dataFim IS NULL OR CAST(a.dtAgendamento AS localdate) <= :dataFim)
            GROUP BY a.especialidade
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByEspecialidade(
            @Param("municipio") String municipio,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /**
     * Conta atendimentos agrupados por status da consulta, com filtros opcionais.
     *
     * @param municipio  filtro por município; {@code null} para todos
     * @param dataInicio filtro por data início; {@code null} para sem limite inferior
     * @param dataFim    filtro por data fim; {@code null} para sem limite superior
     * @return lista de pares [statusConsulta, total]
     */
    @Query("""
            SELECT a.statusConsulta, COUNT(a) FROM AtendimentoEntity a
            WHERE (:municipio IS NULL OR LOWER(a.municipio) LIKE LOWER(CONCAT('%', :municipio, '%')))
              AND (:dataInicio IS NULL OR CAST(a.dtAgendamento AS localdate) >= :dataInicio)
              AND (:dataFim IS NULL OR CAST(a.dtAgendamento AS localdate) <= :dataFim)
            GROUP BY a.statusConsulta
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByStatus(
            @Param("municipio") String municipio,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /** DIAGNÓSTICO — contar registros com desfecho preenchido. */
    long countByDesfechoIsNotNull();

    /** DIAGNÓSTICO — contar registros com CID preenchido. */
    long countByCidIsNotNull();

    /** DIAGNÓSTICO — contar registros com tipo de conclusão preenchido (V10). */
    long countByTpNwConclusaoIsNotNull();

    /** DIAGNÓSTICO — contar registros com CNES de estabelecimento preenchido (V10). */
    long countByCnesEstabelecimentoIsNotNull();

    /** DIAGNÓSTICO — contar registros com endereço completo preenchido. */
    long countByEnderecoCompletoIsNotNull();

    /** DIAGNÓSTICO — contar registros com descrição de consulta preenchida. */
    long countByDescricaoConsultaIsNotNull();
}
