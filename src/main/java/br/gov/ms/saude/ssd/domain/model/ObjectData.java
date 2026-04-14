package br.gov.ms.saude.ssd.domain.model;

import java.util.List;

/**
 * Dados extraídos de um objeto específico da fonte de dados.
 *
 * <p>Record imutável que encapsula o resultado da consulta a um objeto
 * (gráfico, KPI, tabela ou filtro) identificado por {@code objectId}.
 * Retornado por {@code DataSourcePort#getObjectData(String, QueryOptions)}.</p>
 *
 * <p>A estrutura segue o modelo linha/coluna: {@link #headers()} define os
 * nomes das colunas e cada elemento de {@link #rows()} corresponde a uma linha,
 * onde a posição de cada valor corresponde à posição do cabeçalho equivalente.</p>
 *
 * @param objectId ID do objeto na fonte de dados que originou estes dados
 * @param headers  lista com os nomes das colunas, na ordem em que os valores
 *                 aparecem em cada linha de {@link #rows()}
 * @param rows     lista de linhas, onde cada linha é uma lista de valores ({@link Object});
 *                 os tipos concretos dependem dos dados retornados pela fonte
 */
public record ObjectData(
        String objectId,
        List<String> headers,
        List<List<Object>> rows
) {

    /**
     * Retorna o número total de linhas de dados disponíveis.
     *
     * @return quantidade de linhas em {@link #rows()}; zero se não houver dados
     */
    public int totalLinhas() {
        return rows.size();
    }

    /**
     * Indica se este objeto não contém nenhuma linha de dados.
     *
     * @return {@code true} se {@link #rows()} estiver vazia; {@code false} caso contrário
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
