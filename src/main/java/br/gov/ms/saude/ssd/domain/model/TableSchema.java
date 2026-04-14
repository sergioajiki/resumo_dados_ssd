package br.gov.ms.saude.ssd.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Descreve a estrutura de uma tabela da fonte de dados: nome, total de registros e campos.
 *
 * <p>Record imutável que representa o schema de uma tabela individual.
 * Compõe {@link DataSchema} e agrega uma lista de {@link FieldSchema}.</p>
 *
 * @param nome            nome da tabela conforme definido na fonte (ex: "DB_DIGSAUDE")
 * @param totalRegistros  total de linhas presentes na tabela no momento da última carga
 * @param campos          lista de campos (colunas) que compõem esta tabela;
 *                        a ordem reflete a ordem retornada pela fonte de dados
 */
public record TableSchema(
        String nome,
        long totalRegistros,
        List<FieldSchema> campos
) {

    /**
     * Busca um campo pelo nome, ignorando diferenças de maiúsculas/minúsculas.
     *
     * <p>Útil para localizar a definição de um campo específico antes de
     * iniciar a extração, sem precisar iterar manualmente sobre {@link #campos()}.</p>
     *
     * @param nome nome do campo a localizar (case-insensitive)
     * @return {@link Optional} com o {@link FieldSchema} correspondente,
     *         ou {@link Optional#empty()} se o campo não existir nesta tabela
     */
    public Optional<FieldSchema> findCampo(String nome) {
        return campos.stream()
                .filter(f -> f.nome().equalsIgnoreCase(nome))
                .findFirst();
    }
}
