package br.gov.ms.saude.ssd.domain.port.out;

import br.gov.ms.saude.ssd.domain.model.SyncLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para persistência e consulta do histórico de sincronizações.
 *
 * <p>Permite que o serviço de sincronização registre cada execução e consulte
 * o watermark da última sync bem-sucedida, sem depender diretamente
 * do framework de persistência (JPA, JDBC, etc.).</p>
 *
 * @see br.gov.ms.saude.ssd.domain.model.SyncLog
 */
public interface SyncRepositoryPort {

    /**
     * Retorna a data/hora da última sincronização bem-sucedida para uma tabela.
     *
     * <p>Este valor é usado como watermark na extração incremental:
     * apenas registros com {@code DT_NEW} posterior a este valor serão extraídos.</p>
     *
     * @param tableName nome da tabela (ex: "DB_DIGSAUDE")
     * @return {@link Optional} com a data/hora da última sync, ou vazio se nunca sincronizado
     */
    Optional<LocalDateTime> getLastSyncTime(String tableName);

    /**
     * Persiste o log de uma execução de sincronização (completa ou parcial).
     *
     * <p>Deve ser chamado tanto em caso de sucesso quanto de falha,
     * para garantir rastreabilidade completa do histórico.</p>
     *
     * <p>Retorna o {@link SyncLog} persistido, com o {@code id} gerado pelo banco
     * preenchido. O chamador deve reaproveitar esse retorno (e não o {@code log}
     * original) nas transições de estado subsequentes ({@code concluido}/{@code falhou}),
     * para que a mesma linha seja atualizada em vez de uma nova ser inserida.</p>
     *
     * @param log registro da execução de sincronização
     * @return o log persistido, com {@code id} preenchido
     */
    SyncLog recordSync(SyncLog log);

    /**
     * Retorna o histórico de sincronizações de uma tabela, ordenado por data decrescente.
     *
     * @param tableName nome da tabela
     * @param limit     número máximo de registros a retornar
     * @return lista de logs de sincronização
     */
    List<SyncLog> getHistory(String tableName, int limit);
}
