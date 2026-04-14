package br.gov.ms.saude.ssd.domain.model;

import java.time.LocalDateTime;

/**
 * Registro imutável de uma execução de sincronização ETL para uma tabela específica.
 *
 * <p>Captura o ciclo de vida completo de uma sync: início, conclusão, contadores
 * de registros processados, status final e mensagem de erro em caso de falha.</p>
 *
 * <p>Persistido via {@code SyncRepositoryPort#recordSync(SyncLog)} e consultado
 * por {@code ExecutarSyncUseCase#getUltimoSync(String)}. Por ser um record imutável,
 * as transições de estado (ex: RUNNING → SUCCESS) são representadas pela criação
 * de novas instâncias via {@link #concluido} e {@link #falhou}.</p>
 *
 * @param id                   identificador único do log persistido; {@code null} antes da persistência
 * @param tabela               nome da tabela sincronizada (ex: "DB_DIGSAUDE")
 * @param iniciadoEm           data/hora de início da sincronização
 * @param concluidoEm          data/hora de conclusão (sucesso ou falha);
 *                             {@code null} enquanto a sync estiver em andamento
 * @param registrosExtraidos   total de registros lidos da fonte de dados
 * @param registrosNovos       total de registros inseridos no destino como novos
 * @param registrosAtualizados total de registros já existentes no destino que foram atualizados
 * @param status               status atual da sincronização conforme {@link SyncStatus}
 * @param erro                 mensagem de erro em caso de falha; {@code null} em execuções bem-sucedidas
 */
public record SyncLog(
        Long id,
        String tabela,
        LocalDateTime iniciadoEm,
        LocalDateTime concluidoEm,
        int registrosExtraidos,
        int registrosNovos,
        int registrosAtualizados,
        SyncStatus status,
        String erro
) {

    /**
     * Possíveis estados de uma execução de sincronização.
     */
    public enum SyncStatus {

        /** Sincronização concluída com sucesso e todos os registros processados. */
        SUCCESS,

        /** Sincronização encerrada com erro — nenhum dado pode ter sido persistido. */
        FAILED,

        /** Sincronização concluída parcialmente — alguns registros foram processados,
         *  mas ocorreram erros em parte do processamento. */
        PARTIAL,

        /** Sincronização em andamento — ainda não foi concluída. */
        RUNNING
    }

    /**
     * Cria um novo {@link SyncLog} no estado inicial {@link SyncStatus#RUNNING},
     * com {@code iniciadoEm} definido para o momento atual e demais contadores zerados.
     *
     * <p>Deve ser chamado imediatamente antes de iniciar o processo de extração,
     * para que o registro seja persistido e possa ser consultado durante a execução.</p>
     *
     * @param tabela nome da tabela que será sincronizada
     * @return novo {@link SyncLog} com status {@code RUNNING} e hora de início atual
     */
    public static SyncLog iniciando(String tabela) {
        return new SyncLog(null, tabela, LocalDateTime.now(), null, 0, 0, 0, SyncStatus.RUNNING, null);
    }

    /**
     * Retorna uma nova instância deste log com status {@link SyncStatus#SUCCESS}
     * e os contadores de registros atualizados.
     *
     * <p>O campo {@code concluidoEm} é definido para o momento da chamada.
     * O {@code id} e demais metadados são preservados da instância original.</p>
     *
     * @param extraidos   total de registros extraídos da fonte de dados
     * @param novos       total de registros inseridos no destino
     * @param atualizados total de registros atualizados no destino
     * @return nova instância com status {@code SUCCESS} e contadores preenchidos
     */
    public SyncLog concluido(int extraidos, int novos, int atualizados) {
        return new SyncLog(id, tabela, iniciadoEm, LocalDateTime.now(),
                extraidos, novos, atualizados, SyncStatus.SUCCESS, null);
    }

    /**
     * Retorna uma nova instância deste log com status {@link SyncStatus#FAILED}
     * e a mensagem de erro registrada.
     *
     * <p>O campo {@code concluidoEm} é definido para o momento da chamada.
     * Os contadores de registros são preservados com os valores que tinham
     * no momento da falha.</p>
     *
     * @param erro mensagem descritiva do erro que causou a falha
     * @return nova instância com status {@code FAILED} e mensagem de erro preenchida
     */
    public SyncLog falhou(String erro) {
        return new SyncLog(id, tabela, iniciadoEm, LocalDateTime.now(),
                registrosExtraidos, registrosNovos, registrosAtualizados, SyncStatus.FAILED, erro);
    }
}
