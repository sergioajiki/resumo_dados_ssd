package br.gov.ms.saude.ssd.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado consolidado de uma execução do pipeline ETL (full ou incremental).
 *
 * <p>Record imutável que agrega os logs individuais de cada tabela sincronizada,
 * um indicador booleano de sucesso global, um resumo textual legível para
 * exibição em dashboards ou logs de auditoria, e a data/hora de execução.</p>
 *
 * <p>Instâncias são criadas pelos serviços de sincronização ao final de cada
 * execução e retornadas pelo {@code ExecutarSyncUseCase}. Use os factory methods
 * {@link #sucesso} e {@link #falha} para construções semânticamente corretas.</p>
 *
 * @param logs       lista de {@link SyncLog} — um por tabela processada na execução;
 *                   nunca {@code null}, pode ser vazia se nenhuma tabela foi processada
 * @param sucesso    {@code true} se todas as tabelas foram sincronizadas com sucesso;
 *                   {@code false} se ao menos uma falhou (status FAILED ou PARTIAL)
 * @param resumo     texto descritivo do resultado (ex: "3 tabelas sincronizadas, 0 erros");
 *                   destinado a exibição humana em dashboards e e-mails de notificação
 * @param executadoEm data e hora em que a execução do pipeline foi iniciada
 */
public record SyncResult(
        List<SyncLog> logs,
        boolean sucesso,
        String resumo,
        LocalDateTime executadoEm
) {

    /**
     * Cria um {@link SyncResult} representando uma execução bem-sucedida.
     *
     * <p>O campo {@link #executadoEm} é definido para o momento da chamada.
     * O campo {@link #sucesso} é definido como {@code true}.</p>
     *
     * @param logs   lista de {@link SyncLog} das tabelas processadas
     * @param resumo texto descritivo do resultado
     * @return novo {@link SyncResult} com {@code sucesso = true} e hora de execução atual
     */
    public static SyncResult sucesso(List<SyncLog> logs, String resumo) {
        return new SyncResult(logs, true, resumo, LocalDateTime.now());
    }

    /**
     * Cria um {@link SyncResult} representando uma execução com falha (total ou parcial).
     *
     * <p>O campo {@link #executadoEm} é definido para o momento da chamada.
     * O campo {@link #sucesso} é definido como {@code false}.</p>
     *
     * @param logs   lista de {@link SyncLog} das tabelas processadas (pode conter logs
     *               de tabelas que sucederam antes da falha)
     * @param resumo texto descritivo do motivo da falha
     * @return novo {@link SyncResult} com {@code sucesso = false} e hora de execução atual
     */
    public static SyncResult falha(List<SyncLog> logs, String resumo) {
        return new SyncResult(logs, false, resumo, LocalDateTime.now());
    }

    /**
     * Retorna a quantidade de tabelas processadas nesta execução.
     *
     * @return número de {@link SyncLog}s na lista {@link #logs}
     */
    public int totalTabelas() {
        return logs.size();
    }

    /**
     * Retorna a quantidade de tabelas que foram sincronizadas com sucesso.
     *
     * @return número de logs com status {@link SyncLog.SyncStatus#SUCCESS}
     */
    public long tabelasSucesso() {
        return logs.stream()
                .filter(l -> l.status() == SyncLog.SyncStatus.SUCCESS)
                .count();
    }

    /**
     * Retorna a quantidade de tabelas que falharam durante a sincronização.
     *
     * @return número de logs com status {@link SyncLog.SyncStatus#FAILED}
     */
    public long tabelasFalhas() {
        return logs.stream()
                .filter(l -> l.status() == SyncLog.SyncStatus.FAILED)
                .count();
    }
}
