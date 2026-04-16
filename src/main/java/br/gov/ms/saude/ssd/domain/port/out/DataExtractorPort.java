package br.gov.ms.saude.ssd.domain.port.out;

import br.gov.ms.saude.ssd.domain.model.ExtractOptions;
import br.gov.ms.saude.ssd.domain.model.ExtractResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de saída para extração em lote de dados — usada exclusivamente pelo ETL pipeline.
 *
 * <p>Separada de {@link DataSourcePort} para respeitar o princípio da Segregação de
 * Interfaces (ISP — SOLID): nem toda fonte de dados precisa suportar extração em lote,
 * e nem todo consumidor de {@link DataSourcePort} precisa de extração.</p>
 *
 * <p>A extração é realizada de forma paginada para evitar sobrecarga na fonte
 * e no consumidor. O tamanho padrão de página é configurável via
 * {@code datasource.qlik.page-size} no {@code application.yml}.</p>
 *
 * <p>Implementações:</p>
 * <ul>
 *   <li>{@code QlikEngineAdapter} — extrai via WebSocket + JSON-RPC paginado</li>
 *   <li>{@code MockAdapter} — retorna dados simulados dos arquivos JSON de teste</li>
 * </ul>
 *
 * @see DataSourcePort
 * @see br.gov.ms.saude.ssd.domain.model.ExtractResult
 */
public interface DataExtractorPort {

    /**
     * Extrai todos os registros de uma tabela da fonte de dados de forma paginada.
     *
     * <p>A extração é incremental por página internamente — o chamador recebe
     * o resultado consolidado com todos os registros. Para tabelas grandes,
     * prefira {@link #extractSince} para extrações incrementais.</p>
     *
     * @param tableName nome da tabela a ser extraída (ex: "DB_DIGSAUDE")
     * @param fields    lista de campos a incluir na extração
     * @param options   opções de extração: tamanho de página, timeout, ordenação
     * @return {@link ExtractResult} com todos os registros extraídos e metadados
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se não for possível conectar à fonte
     * @throws br.gov.ms.saude.ssd.domain.exception.DataExtractionException
     *         se ocorrer erro durante a extração das páginas
     */
    ExtractResult extractTable(String tableName, List<String> fields, ExtractOptions options);

    /**
     * Extrai apenas os registros alterados ou criados após a data de referência (watermark).
     *
     * <p>Utilizado nas sincronizações incrementais diárias. O campo watermark
     * padrão é {@code DT_NEW}, que possui a maior cardinalidade temporal do dataset
     * (16.403 valores distintos), garantindo precisão na detecção de alterações.</p>
     *
     * @param tableName nome da tabela a ser extraída
     * @param fields    lista de campos a incluir na extração
     * @param watermark data/hora de referência — apenas registros com DT_NEW posterior
     *                  a este valor serão retornados
     * @return {@link ExtractResult} com os registros novos/alterados
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se não for possível conectar à fonte
     * @throws br.gov.ms.saude.ssd.domain.exception.DataExtractionException
     *         se ocorrer erro durante a extração
     */
    ExtractResult extractSince(String tableName, List<String> fields, LocalDateTime watermark);
}
