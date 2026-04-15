package br.gov.ms.saude.ssd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
@ConfigurationProperties(prefix = "datasource.qlik")
public class QlikProperties {

    /**
     * Hostname do servidor Qlik Sense (sem protocolo).
     * Exemplo: {@code paineispublicos.saude.ms.gov.br}
     */
    private String host = "";

    /**
     * Identificador único do app Qlik Sense a ser consultado.
     * Exemplo: {@code 10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb}
     */
    private String appId = "";

    /**
     * Timeout de abertura da conexão WebSocket em milissegundos.
     * Aumentar em redes com alta latência.
     */
    private int connectionTimeoutMs = 10000;

    /**
     * Timeout de leitura de dados em milissegundos.
     * Aumentar para tabelas grandes ou conexões lentas.
     */
    private int readTimeoutMs = 30000;

    /**
     * Prefixo do virtual proxy anônimo do Qlik Sense.
     * Para painéis públicos sem autenticação use {@code /anon}.
     * Para acesso autenticado use {@code ""} (raiz) ou o nome do proxy configurado.
     */
    private String virtualProxy = "/anon";

    /**
     * Número máximo de linhas retornadas por página na Engine API.
     * O Qlik suporta até 10.000; recomendado 5.000 para margem de segurança.
     */
    private int pageSize = 5000;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public String getVirtualProxy() { return virtualProxy; }
    public void setVirtualProxy(String virtualProxy) { this.virtualProxy = virtualProxy; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
