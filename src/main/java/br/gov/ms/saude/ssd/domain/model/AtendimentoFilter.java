package br.gov.ms.saude.ssd.domain.model;

import java.time.LocalDate;

/**
 * Critérios de filtro para consulta de atendimentos do Núcleo de Telessaúde MS.
 *
 * <p>Record imutável que encapsula todos os parâmetros opcionais de filtragem
 * utilizados por {@code ConsultarAtendimentosUseCase#consultar}. Campos com
 * valor {@code null} são ignorados na construção da consulta — equivalem a
 * "sem restrição" para aquela dimensão.</p>
 *
 * <p>Use {@link #vazio()} para consultar todos os atendimentos sem filtros,
 * ou construa diretamente via construtor canônico especificando apenas os
 * campos desejados.</p>
 *
 * @param municipio       código ou nome do município de residência do paciente;
 *                        {@code null} para não filtrar por município
 * @param dataInicio      data de início do período de interesse (inclusiva);
 *                        {@code null} para sem limite inferior de data
 * @param dataFim         data de fim do período de interesse (inclusiva);
 *                        {@code null} para sem limite superior de data
 * @param especialidade   nome da especialidade médica do atendimento;
 *                        {@code null} para todas as especialidades
 * @param statusConsulta  status da consulta (ex: "Realizado", "Cancelado", "Agendado");
 *                        {@code null} para todos os status
 * @param faixaEtaria     faixa etária do paciente (ex: "0-17", "18-59", "60+");
 *                        {@code null} para todas as faixas
 * @param racaPaciente    raça/cor autodeclarada do paciente conforme classificação IBGE;
 *                        {@code null} para todas as raças
 */
public record AtendimentoFilter(
        String municipio,
        LocalDate dataInicio,
        LocalDate dataFim,
        String especialidade,
        String statusConsulta,
        String faixaEtaria,
        String racaPaciente
) {

    /**
     * Cria um filtro vazio, sem nenhum critério de restrição.
     *
     * <p>Equivale a consultar todos os atendimentos sem qualquer filtragem.
     * Todos os campos são {@code null}.</p>
     *
     * @return {@link AtendimentoFilter} com todos os campos nulos
     */
    public static AtendimentoFilter vazio() {
        return new AtendimentoFilter(null, null, null, null, null, null, null);
    }
}

