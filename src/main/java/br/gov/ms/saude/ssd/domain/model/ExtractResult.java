package br.gov.ms.saude.ssd.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado consolidado de uma extração de dados realizada pelo pipeline ETL.
 *
 * <p>Record imutável retornado por {@code DataExtractorPort#extractTable} e
 * {@code DataExtractorPort#extractSince}. Encapsula tanto os dados extraídos
 * quanto os metadados da execução (watermark, duração e total de registros).</p>
 *
 * <p>A estrutura linha/coluna segue a mesma convenção de {@link ObjectData}:
 * {@link #headers()} define as colunas e cada elemento de {@link #rows()}
 * é uma linha com valores na ordem correspondente.</p>
 *
 * @param tableName       nome da tabela de origem da extração (ex: "DB_DIGSAUDE")
 * @param rows            linhas extraídas; cada linha é uma lista de valores {@link Object}
 *                        cujos tipos dependem da fonte de dados
 * @param headers         nomes das colunas, na ordem em que os valores aparecem em cada linha
 * @param totalExtraidos  total de registros efetivamente extraídos e incluídos em {@link #rows()};
 *                        pode diferir do total da tabela em extrações incrementais
 * @param watermark       data/hora de referência dos dados extraídos — usada como ponto de
 *                        corte para a próxima extração incremental (persisted via {@code SyncLog})
 * @param duracao         duração total do processo de extração, incluindo todas as páginas
 */
public record ExtractResult(
        String tableName,
        List<List<Object>> rows,
        List<String> headers,
        int totalExtraidos,
        LocalDateTime watermark,
        Duration duracao
) {

    /**
     * Indica se a extração não retornou nenhum registro.
     *
     * <p>Útil para decidir se a etapa de carga (load) deve ser pulada
     * quando não há dados novos em uma extração incremental.</p>
     *
     * @return {@code true} se {@link #rows()} estiver vazia; {@code false} caso contrário
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
