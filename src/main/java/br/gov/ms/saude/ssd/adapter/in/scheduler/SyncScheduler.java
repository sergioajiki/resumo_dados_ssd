package br.gov.ms.saude.ssd.adapter.in.scheduler;

import br.gov.ms.saude.ssd.application.usecase.ExecutarSyncUseCase;
import br.gov.ms.saude.ssd.domain.exception.SyncAlreadyRunningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada temporal — dispara a sincronização ETL em horário programado.
 *
 * <p>Implementa a porta de entrada agendada do sistema. Por padrão executa
 * sincronização incremental diariamente às 09h00 (após o reload do Qlik às 08h).
 * O cron pode ser sobrescrito pela propriedade {@code sync.schedule} no
 * {@code application.yml}.</p>
 *
 * <p>Em caso de {@link SyncAlreadyRunningException} (ex: execução manual em andamento),
 * o agendamento é ignorado sem erro — o log registra o motivo do skip.</p>
 *
 * <p>Princípio aplicado: SRP — este componente só faz scheduling.
 * Toda a lógica de negócio reside no {@link ExecutarSyncUseCase}.</p>
 *
 * @see ExecutarSyncUseCase
 * @see br.gov.ms.saude.ssd.application.service.SyncService
 */
@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final ExecutarSyncUseCase executarSyncUseCase;

    /**
     * Injeta o use case via construtor (facilita substituição e testes).
     *
     * @param executarSyncUseCase use case de execução e monitoramento do ETL
     */
    public SyncScheduler(ExecutarSyncUseCase executarSyncUseCase) {
        this.executarSyncUseCase = executarSyncUseCase;
    }

    /**
     * Executa a sincronização incremental diária às 09h00.
     *
     * <p>O horário é posterior ao reload diário do Qlik Sense (08h00),
     * garantindo que os dados mais recentes já estejam disponíveis na fonte.
     * O cron {@code "0 0 9 * * *"} pode ser sobrescrito pela propriedade
     * {@code sync.schedule} no {@code application.yml} ou via variável de ambiente.</p>
     *
     * <p>Em caso de falha, o erro é logado mas não relançado — o scheduler
     * continua funcionando para a próxima execução agendada.</p>
     */
    @Scheduled(cron = "${sync.schedule:0 0 9 * * *}")
    public void executarSyncDiaria() {
        log.info("Iniciando sync diária agendada.");
        try {
            var result = executarSyncUseCase.executarIncrementalSync();
            if (result.sucesso()) {
                log.info("Sync diária concluída com sucesso. Resumo: {}", result.resumo());
            } else {
                log.warn("Sync diária concluída com falhas. Resumo: {}", result.resumo());
            }
        } catch (SyncAlreadyRunningException e) {
            log.info("Sync diária ignorada: já há uma sincronização em andamento.");
        } catch (Exception e) {
            log.error("Erro inesperado na sync diária: {}", e.getMessage(), e);
        }
    }
}
