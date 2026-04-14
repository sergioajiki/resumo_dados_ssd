package br.gov.ms.saude.ssd.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Representa o schema completo da fonte de dados: conjunto de todas as tabelas disponíveis.
 *
 * <p>Record imutável que agrega a estrutura de todas as tabelas retornadas pela fonte.
 * Populado via {@code DataSourcePort#getDataSchema()} e exposto pelo
 * {@code ConsultarSchemaUseCase}.</p>
 *
 * <p>Utilizado principalmente para:</p>
 * <ul>
 *   <li>Inspeção do modelo de dados via API antes de iniciar extração</li>
 *   <li>Validação de campos disponíveis em {@link DataExtractorPort}</li>
 *   <li>Descoberta automática de tabelas pelo pipeline ETL</li>
 * </ul>
 *
 * @param tabelas lista de tabelas disponíveis na fonte de dados;
 *                nunca {@code null} — pode ser vazia se a fonte não expuser tabelas
 */
public record DataSchema(
        List<TableSchema> tabelas
) {

    /**
     * Busca uma tabela pelo nome, ignorando diferenças de maiúsculas/minúsculas.
     *
     * <p>Conveniência para localizar a definição de uma tabela específica sem
     * iterar manualmente sobre {@link #tabelas()}.</p>
     *
     * @param nome nome da tabela a localizar (case-insensitive)
     * @return {@link Optional} com o {@link TableSchema} correspondente,
     *         ou {@link Optional#empty()} se a tabela não existir no schema
     */
    public Optional<TableSchema> findTabela(String nome) {
        return tabelas.stream()
                .filter(t -> t.nome().equalsIgnoreCase(nome))
                .findFirst();
    }
}
