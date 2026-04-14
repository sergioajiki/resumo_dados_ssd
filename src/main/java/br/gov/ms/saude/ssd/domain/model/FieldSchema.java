package br.gov.ms.saude.ssd.domain.model;

import java.util.List;

/**
 * Descreve um campo (coluna) dentro de uma tabela da fonte de dados.
 *
 * <p>Record imutável que representa a estrutura de um campo individual,
 * incluindo tipo, cardinalidade e metadados adicionais fornecidos pela fonte.
 * Compõe {@link TableSchema}.</p>
 *
 * @param nome         nome do campo conforme definido na fonte de dados (ex: "MUN_RESIDENCIA")
 * @param tipo         tipo de dado inferido pela fonte (ex: "text", "numeric", "timestamp")
 * @param cardinalidade número de valores distintos presentes neste campo;
 *                      útil para identificar campos de alta seletividade
 * @param isPrimaryKey {@code true} se este campo é identificado como chave primária
 *                     ou tem comportamento equivalente (cardinalidade = total de registros)
 * @param tags         lista de tags/marcadores associados ao campo pela fonte de dados;
 *                     pode incluir metadados como "hidden", "mandatory", "$date", etc.
 */
public record FieldSchema(
        String nome,
        String tipo,
        int cardinalidade,
        boolean isPrimaryKey,
        List<String> tags
) {
}
