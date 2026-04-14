package br.gov.ms.saude.ssd.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de configuração da conexão com o servidor Qlik Sense.
 *
 * <p>Lidas do prefixo {@code datasource.qlik} no {@code application.yml}.
 * Validadas na inicialização do contexto Spring para evitar falhas silenciosas
 * em produção quando alguma propriedade obrigatória estiver ausente.</p>
 *
 * <p>Exemplo de configuração:</p>
 * <pre>
 * datasource:
 *   qlik:
 *     host: paineispublicos.saude.ms.gov.br
 *     app-id: 10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb
 *     page-size: 5000
 * </pre>
 *
 * @see br.gov.ms.saude.ssd.config.DataSourceConfig
 */
@Validated
@ConfigurationProperties(prefix = "datasource.qlik")
public class QlikProperties {

    /**
     * Hostname do servidor Qlik Sense (sem protocolo).
     * Exemplo: {@code paineispublicos.saude.ms.gov.br}
     */
    @NotBlank
    private String host = "";

    /**
     * Identificador único do app Qlik Sense a ser consultado.
     * Exemplo: {@code 10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb}
     */
    @NotBlank
    private String appId = "";

    /**
     * Timeout de abertura da conexão WebSocket em milissegundos.
     * Aumentar em redes com alta latência.
     */
    @Positive
    private int connectionTimeoutMs = 10000;

    /**
     * Timeout de leitura de dados em milissegundos.
     * Aumentar para tabelas grandes ou conexões lentas.
     */
    @Positive
    private int readTimeoutMs = 30000;

    /**
     * Número máximo de linhas retornadas por página na Engine API.
     * O Qlik suporta até 10.000; recomendado 5.000 para margem de segurança.
     */
    @Positive
    private int pageSize = 5000;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
