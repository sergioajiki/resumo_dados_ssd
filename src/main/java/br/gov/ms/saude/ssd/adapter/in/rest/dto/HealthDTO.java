package br.gov.ms.saude.ssd.adapter.in.rest.dto;

import java.time.LocalDateTime;

/**
 * DTO imutável de saída que representa o estado de saúde da conexão com a fonte de dados.
 *
 * <p>Exposto pelo endpoint REST {@code /api/v1/health} e produzido a partir de
 * {@code HealthStatus} pelo transformador correspondente. Permite que clientes
 * externos verifiquem a disponibilidade do adaptador ativo sem acesso direto
 * ao domínio da aplicação.</p>
 *
 * <p>O campo {@link #adaptadorAtivo} identifica qual implementação de
 * {@code DataSourcePort} está em uso no momento, facilitando o diagnóstico
 * em ambientes com múltiplos profiles.</p>
 *
 * @param status         status operacional da fonte: {@code UP}, {@code DOWN} ou {@code DEGRADED}
 * @param latencyMs      latência medida em milissegundos na última verificação;
 *                       {@code -1} quando a medição não foi possível (ex: timeout)
 * @param message        mensagem descritiva do resultado da verificação;
 *                       em caso de falha, descreve o motivo do problema
 * @param checkedAt      data e hora em que a verificação foi realizada
 * @param adaptadorAtivo nome do adaptador de fonte de dados em uso (ex: {@code "qlik-engine"},
 *                       {@code "mock"})
 */
public record HealthDTO(
        String status,
        Long latencyMs,
        String message,
        LocalDateTime checkedAt,
        String adaptadorAtivo
) {
}
