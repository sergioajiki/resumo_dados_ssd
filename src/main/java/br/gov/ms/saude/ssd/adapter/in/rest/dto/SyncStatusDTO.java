package br.gov.ms.saude.ssd.adapter.in.rest.dto;

import java.time.LocalDateTime;

/**
 * DTO imutável de saída que representa o status de uma execução de sincronização ETL.
 *
 * <p>Exposto pela API REST ({@code /api/v1/sync/status}) para que clientes externos
 * acompanhem o andamento ou resultado das sincronizações de cada tabela.
 * Produzido a partir de {@code SyncLog} pelo transformador de sincronização.</p>
 *
 * <p>O campo {@link #status} reflete o ciclo de vida da sincronização:
 * {@code RUNNING} enquanto em andamento, {@code SUCCESS}, {@code FAILED}
 * ou {@code PARTIAL} ao final.</p>
 *
 * @param tabela               nome da tabela que foi (ou está sendo) sincronizada
 * @param status               status da execução: {@code SUCCESS}, {@code FAILED},
 *                             {@code PARTIAL} ou {@code RUNNING}
 * @param iniciadoEm           data e hora de início da sincronização
 * @param concluidoEm          data e hora de conclusão; {@code null} se ainda em andamento
 * @param registrosExtraidos   total de registros lidos da fonte de dados durante a execução
 * @param registrosNovos       total de registros inseridos no repositório local como novos
 * @param registrosAtualizados total de registros já existentes que foram atualizados
 * @param erro                 mensagem de erro em caso de falha; {@code null} em execuções bem-sucedidas
 * @param duracaoFormatada     duração total formatada para exibição (ex: {@code "1m 23s"});
 *                             {@code null} se a sincronização ainda estiver em andamento
 */
public record SyncStatusDTO(
        String tabela,
        String status,
        LocalDateTime iniciadoEm,
        LocalDateTime concluidoEm,
        Integer registrosExtraidos,
        Integer registrosNovos,
        Integer registrosAtualizados,
        String erro,
        String duracaoFormatada
) {
}
