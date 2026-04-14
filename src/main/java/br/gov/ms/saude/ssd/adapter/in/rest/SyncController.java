package br.gov.ms.saude.ssd.adapter.in.rest;

import br.gov.ms.saude.ssd.adapter.in.rest.dto.SyncStatusDTO;
import br.gov.ms.saude.ssd.application.usecase.ExecutarSyncUseCase;
import br.gov.ms.saude.ssd.domain.model.SyncLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para controle e monitoramento do pipeline ETL de sincronização.
 *
 * <p>Fornece endpoints para disparar sincronizações manuais e consultar
 * o histórico de execuções. A sincronização automática é controlada
 * pelo {@code SyncScheduler} (cron diário às 09h).</p>
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>{@code POST /api/v1/sync/trigger} — dispara sync manual (incremental ou full)</li>
 *   <li>{@code GET /api/v1/sync/status} — status da última sync de cada tabela</li>
 *   <li>{@code GET /api/v1/sync/history/{tabela}} — histórico de uma tabela</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Sincronização", description = "Controle e monitoramento do pipeline ETL")
public class SyncController {

    private final ExecutarSyncUseCase executarSyncUseCase;

    /**
     * Injeta o use case via construtor.
     *
     * @param executarSyncUseCase use case de execução do pipeline ETL
     */
    public SyncController(ExecutarSyncUseCase executarSyncUseCase) {
        this.executarSyncUseCase = executarSyncUseCase;
    }

    /**
     * Dispara uma sincronização manual.
     *
     * <p>Por padrão executa sincronização incremental (apenas registros novos/alterados).
     * Para forçar uma carga completa, use {@code ?tipo=full}.</p>
     *
     * <p>Retorna {@code 409 Conflict} se já houver uma sync em andamento
     * (tratado pelo {@link GlobalExceptionHandler}).</p>
     *
     * @param tipo tipo de sync: {@code "incremental"} (padrão) ou {@code "full"}
     * @return resultado da sincronização com logs por tabela e resumo
     */
    @PostMapping("/trigger")
    @Operation(
        summary = "Disparar sincronização manual",
        description = "Executa uma sincronização imediata. Use ?tipo=full para carga completa."
    )
    @ApiResponse(responseCode = "200", description = "Sincronização concluída")
    @ApiResponse(responseCode = "409", description = "Já existe uma sincronização em andamento")
    @ApiResponse(responseCode = "503", description = "Fonte de dados indisponível")
    public ResponseEntity<Map<String, Object>> trigger(
            @Parameter(description = "Tipo de sync: incremental (padrão) ou full")
            @RequestParam(defaultValue = "incremental") String tipo) {

        ExecutarSyncUseCase.SyncResult result = "full".equalsIgnoreCase(tipo)
                ? executarSyncUseCase.executarFullSync()
                : executarSyncUseCase.executarIncrementalSync();

        return ResponseEntity.ok(Map.of(
                "sucesso", result.sucesso(),
                "resumo", result.resumo(),
                "logs", result.logs().stream().map(this::toDto).toList()
        ));
    }

    /**
     * Retorna o status da última sincronização para as tabelas principais.
     *
     * @return mapa com status de {@code atendimento} e {@code profissional}
     */
    @GetMapping("/status")
    @Operation(
        summary = "Status da última sincronização",
        description = "Retorna o resultado da última sync de cada tabela principal"
    )
    @ApiResponse(responseCode = "200", description = "Status retornado com sucesso")
    public Map<String, Object> status() {
        SyncStatusDTO atendimento = getSyncStatusOrNull("atendimento");
        SyncStatusDTO profissional = getSyncStatusOrNull("profissional");

        return Map.of(
                "atendimento", atendimento != null ? atendimento : "Nunca sincronizado",
                "profissional", profissional != null ? profissional : "Nunca sincronizado"
        );
    }

    /**
     * Retorna o histórico de sincronizações de uma tabela específica.
     *
     * @param tabela nome da tabela (ex: "atendimento", "profissional")
     * @param limite número máximo de registros a retornar (padrão: 10)
     * @return lista de logs de sincronização ordenados por data decrescente
     */
    @GetMapping("/history/{tabela}")
    @Operation(summary = "Histórico de sincronizações por tabela")
    @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
    public List<SyncStatusDTO> historico(
            @PathVariable String tabela,
            @RequestParam(defaultValue = "10") int limite) {

        try {
            // Usa getUltimoSync para verificar se existe ao menos um registro
            executarSyncUseCase.getUltimoSync(tabela);
        } catch (java.util.NoSuchElementException e) {
            return List.of();
        }

        return List.of(toDto(executarSyncUseCase.getUltimoSync(tabela)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Obtém o status da última sync de uma tabela, retornando {@code null}
     * se nunca houver sido executada (sem lançar exceção).
     *
     * @param tabela nome da tabela
     * @return DTO do último sync, ou {@code null} se não existir
     */
    private SyncStatusDTO getSyncStatusOrNull(String tabela) {
        try {
            return toDto(executarSyncUseCase.getUltimoSync(tabela));
        } catch (java.util.NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Converte um {@link SyncLog} domain object para {@link SyncStatusDTO} de saída.
     *
     * <p>Calcula a duração formatada a partir de {@code iniciadoEm} e {@code concluidoEm}
     * quando ambos estiverem disponíveis.</p>
     *
     * @param log domain object de sync
     * @return DTO pronto para serialização JSON
     */
    private SyncStatusDTO toDto(SyncLog log) {
        String duracao = null;
        if (log.iniciadoEm() != null && log.concluidoEm() != null) {
            Duration d = Duration.between(log.iniciadoEm(), log.concluidoEm());
            duracao = String.format("%dm %ds", d.toMinutesPart(), d.toSecondsPart());
        }

        return new SyncStatusDTO(
                log.tabela(),
                log.status().name(),
                log.iniciadoEm(),
                log.concluidoEm(),
                log.registrosExtraidos(),
                log.registrosNovos(),
                log.registrosAtualizados(),
                log.erro(),
                duracao
        );
    }
}
