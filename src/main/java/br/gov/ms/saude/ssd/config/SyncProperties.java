package br.gov.ms.saude.ssd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração do pipeline ETL de sincronização.
 *
 * <p>Lidas do prefixo {@code sync} no {@code application.yml}.
 * Controlam o agendamento, a estratégia de carga e o campo watermark
 * utilizado nas sincronizações incrementais.</p>
 *
 * <p>Exemplo de configuração:</p>
 * <pre>
 * sync:
 *   schedule: "0 0 9 * * *"   # Todo dia às 09h (após reload do Qlik às 08h)
 *   strategy: incremental      # "full" na primeira carga, "incremental" depois
 *   watermark-field: DT_NEW
 * </pre>
 */
@ConfigurationProperties(prefix = "sync")
public class SyncProperties {

    /**
     * Expressão cron que define o agendamento da sincronização automática.
     * Padrão: todo dia às 09h (após o reload diário do Qlik às 08h).
     */
    private String schedule = "0 0 9 * * *";

    /**
     * Estratégia de carga: {@code "full"} recarrega todos os registros a cada sync;
     * {@code "incremental"} carrega apenas registros novos/alterados desde a última sync.
     * Use {@code "full"} apenas na primeira carga ou quando o schema mudar.
     */
    private String strategy = "incremental";

    /**
     * Campo da tabela principal usado como watermark para a extração incremental.
     * {@code DT_NEW} possui a maior cardinalidade temporal do dataset (16.403 valores),
     * tornando-o o campo mais preciso para detectar alterações.
     */
    private String watermarkField = "DT_NEW";

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getWatermarkField() { return watermarkField; }
    public void setWatermarkField(String watermarkField) { this.watermarkField = watermarkField; }

    /**
     * Indica se a estratégia configurada é incremental.
     *
     * @return {@code true} se {@link #strategy} for {@code "incremental"}
     */
    public boolean isIncremental() {
        return "incremental".equalsIgnoreCase(strategy);
    }
}
