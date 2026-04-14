package br.gov.ms.saude.ssd.domain.model;

import java.time.LocalDateTime;

/**
 * Representa o status de saúde e disponibilidade da conexão com a fonte de dados.
 *
 * <p>Record imutável retornado por {@code DataSourcePort#checkHealth()} e exposto
 * via {@code ConsultarSchemaUseCase#getHealth()}. Contém o status operacional,
 * a latência medida e uma mensagem descritiva.</p>
 *
 * <p>Use os factory methods ({@link #up}, {@link #down}, {@link #degraded}) para
 * criar instâncias semanticamente corretas, evitando construção direta com
 * combinações inválidas de parâmetros.</p>
 *
 * @param status     status operacional da fonte conforme {@link HealthStatusEnum}
 * @param latencyMs  latência da última verificação em milissegundos;
 *                   zero ou negativo indica que a medição não foi possível (ex: timeout)
 * @param message    mensagem descritiva do resultado da verificação;
 *                   em caso de falha, deve descrever o motivo do problema
 * @param checkedAt  data/hora em que a verificação foi realizada
 */
public record HealthStatus(
        HealthStatusEnum status,
        long latencyMs,
        String message,
        LocalDateTime checkedAt
) {

    /**
     * Enumeração dos possíveis estados operacionais da fonte de dados.
     */
    public enum HealthStatusEnum {

        /** Fonte disponível e respondendo dentro dos parâmetros normais. */
        UP,

        /** Fonte indisponível ou inacessível — extração deve ser abortada. */
        DOWN,

        /** Fonte disponível, mas com degradação de desempenho ou funcionalidade parcial. */
        DEGRADED
    }

    /**
     * Cria um status {@link HealthStatusEnum#UP} indicando que a fonte está
     * disponível e respondendo normalmente.
     *
     * @param latencyMs latência medida em milissegundos durante a verificação
     * @return {@link HealthStatus} com status UP e mensagem padrão de sucesso
     */
    public static HealthStatus up(long latencyMs) {
        return new HealthStatus(HealthStatusEnum.UP, latencyMs, "Fonte de dados disponível.", LocalDateTime.now());
    }

    /**
     * Cria um status {@link HealthStatusEnum#DOWN} indicando que a fonte está
     * indisponível. A latência é registrada como {@code -1} por não ser aplicável.
     *
     * @param reason descrição do motivo da indisponibilidade (ex: mensagem de exceção)
     * @return {@link HealthStatus} com status DOWN e o motivo informado na mensagem
     */
    public static HealthStatus down(String reason) {
        return new HealthStatus(HealthStatusEnum.DOWN, -1L, reason, LocalDateTime.now());
    }

    /**
     * Cria um status {@link HealthStatusEnum#DEGRADED} indicando que a fonte está
     * operacional, porém com desempenho ou disponibilidade reduzidos.
     *
     * @param reason    descrição da degradação observada
     * @param latencyMs latência medida em milissegundos, possivelmente acima do normal
     * @return {@link HealthStatus} com status DEGRADED, motivo e latência informados
     */
    public static HealthStatus degraded(String reason, long latencyMs) {
        return new HealthStatus(HealthStatusEnum.DEGRADED, latencyMs, reason, LocalDateTime.now());
    }

    /**
     * Indica se a fonte de dados está disponível para consulta.
     *
     * <p>Considera disponível qualquer status diferente de {@link HealthStatusEnum#DOWN}:
     * tanto {@code UP} quanto {@code DEGRADED} permitem tentativa de extração,
     * embora com risco de lentidão no caso degradado.</p>
     *
     * @return {@code true} se o status for {@link HealthStatusEnum#UP} ou
     *         {@link HealthStatusEnum#DEGRADED}; {@code false} se for {@link HealthStatusEnum#DOWN}
     */
    public boolean isAvailable() {
        return status == HealthStatusEnum.UP || status == HealthStatusEnum.DEGRADED;
    }
}
