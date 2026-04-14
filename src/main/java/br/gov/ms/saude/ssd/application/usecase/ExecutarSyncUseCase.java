package br.gov.ms.saude.ssd.application.usecase;

import br.gov.ms.saude.ssd.domain.model.SyncLog;

import java.util.List;

/**
 * Porta de entrada (use case) para execução e monitoramento do pipeline ETL de sincronização.
 *
 * <p>Define o contrato para as operações de sincronização de dados entre a fonte
 * (Qlik Sense) e o repositório local (H2). Suporta sincronização completa (full)
 * e incremental, além de consulta ao histórico.</p>
 *
 * <p>Implementado pelo serviço de sincronização na camada de aplicação e invocado
 * tanto pelo scheduler ({@code @Scheduled}) quanto pelos endpoints administrativos
 * da API REST.</p>
 *
 * @see SyncLog
 * @see SyncResult
 */
public interface ExecutarSyncUseCase {

    /**
     * Executa uma sincronização completa (full sync): extrai todos os registros
     * da fonte de dados e os persiste no repositório local, substituindo dados anteriores.
     *
     * <p>Deve ser utilizado na inicialização do sistema ou quando for necessário
     * reconstruir o repositório local do zero. Pode ser demorado para datasets grandes.</p>
     *
     * @return {@link SyncResult} com os logs de cada tabela sincronizada,
     *         indicador global de sucesso e um resumo textual da execução
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se a fonte de dados estiver inacessível no início da execução
     * @throws br.gov.ms.saude.ssd.domain.exception.SyncAlreadyRunningException
     *         se já houver uma sincronização em andamento
     */
    SyncResult executarFullSync();

    /**
     * Executa uma sincronização incremental: extrai apenas os registros novos ou
     * alterados desde a última sync bem-sucedida (baseada no watermark {@code DT_NEW}).
     *
     * <p>Método preferido para execuções periódicas agendadas, pois minimiza
     * o volume de dados transferido e o tempo de execução.</p>
     *
     * @return {@link SyncResult} com os logs de cada tabela sincronizada,
     *         indicador global de sucesso e um resumo textual da execução
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se a fonte de dados estiver inacessível no início da execução
     * @throws br.gov.ms.saude.ssd.domain.exception.SyncAlreadyRunningException
     *         se já houver uma sincronização em andamento
     */
    SyncResult executarIncrementalSync();

    /**
     * Retorna o registro da última sincronização executada para uma tabela específica,
     * independentemente do status (SUCCESS, FAILED, PARTIAL ou RUNNING).
     *
     * @param tabela nome da tabela a consultar (ex: "DB_DIGSAUDE")
     * @return {@link SyncLog} do último registro encontrado para a tabela
     * @throws java.util.NoSuchElementException se nenhuma sync tiver sido executada para a tabela
     */
    SyncLog getUltimoSync(String tabela);

    /**
     * Resultado consolidado de uma execução do pipeline ETL (full ou incremental).
     *
     * <p>Record imutável que agrega os logs individuais de cada tabela sincronizada,
     * um indicador booleano de sucesso global e um resumo textual legível para
     * exibição em dashboards ou logs de auditoria.</p>
     *
     * @param logs    lista de {@link SyncLog} — um por tabela processada na execução
     * @param sucesso {@code true} se todas as tabelas foram sincronizadas com sucesso;
     *                {@code false} se ao menos uma falhou (status FAILED ou PARTIAL)
     * @param resumo  texto descritivo do resultado (ex: "3 tabelas sincronizadas, 0 erros")
     */
    record SyncResult(
            List<SyncLog> logs,
            boolean sucesso,
            String resumo
    ) {
    }
}
