package br.gov.ms.saude.ssd.adapter.out.qlik.rest;

import br.gov.ms.saude.ssd.config.QlikProperties;
import br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cliente HTTP para os endpoints REST públicos do Qlik Sense Enterprise.
 *
 * <p>O Qlik Sense Enterprise expõe uma REST API para metadados do app.
 * Estes endpoints não requerem autenticação quando o servidor está em modo
 * de acesso anônimo via virtual proxy.</p>
 *
 * <p>Endpoints utilizados:</p>
 * <ul>
 *   <li>{@code GET /api/v1/apps/{appId}} — metadados do app</li>
 *   <li>{@code GET /api/v1/apps/{appId}/data/metadata} — schema das tabelas e campos</li>
 * </ul>
 *
 * <p>O {@link RestTemplate} é configurado com timeouts de conexão e leitura
 * definidos em {@link QlikProperties}. Em caso de timeout ou erro de rede,
 * lança {@link DataSourceUnavailableException} para tratamento unificado
 * pelo {@link br.gov.ms.saude.ssd.adapter.in.rest.GlobalExceptionHandler}.</p>
 *
 * @see QlikRestAdapter
 * @see QlikProperties
 */
@Component
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "qlik-rest")
public class QlikRestClient {

    private static final Logger log = LoggerFactory.getLogger(QlikRestClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    /**
     * Constrói o cliente HTTP com timeouts configurados via {@link QlikProperties}.
     *
     * @param props          propriedades de configuração do Qlik (host, timeouts)
     * @param templateBuilder builder do Spring para criação do RestTemplate com timeouts
     * @param objectMapper   serializer JSON compartilhado do contexto Spring
     */
    public QlikRestClient(QlikProperties props,
                          RestTemplateBuilder templateBuilder,
                          ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.baseUrl = "https://" + props.getHost();
        this.restTemplate = templateBuilder
                .connectTimeout(Duration.ofMillis(props.getConnectionTimeoutMs()))
                .readTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .build();
    }

    /**
     * Obtém os metadados descritivos do app Qlik Sense.
     *
     * <p>Chama {@code GET /api/v1/apps/{appId}} e retorna o JSON bruto.
     * O mapeamento para domain objects é feito pelo {@link QlikRestAdapter}.</p>
     *
     * @param appId identificador do app Qlik Sense
     * @return nó JSON com os metadados do app
     * @throws DataSourceUnavailableException se o servidor estiver inacessível ou retornar erro
     */
    public JsonNode getAppMetadata(String appId) {
        String url = baseUrl + "/api/v1/apps/" + appId;
        return get(url);
    }

    /**
     * Obtém o schema de dados (tabelas e campos) do app Qlik Sense.
     *
     * <p>Chama {@code GET /api/v1/apps/{appId}/data/metadata}.
     * O schema inclui nomes de campos, tipos e cardinalidades.</p>
     *
     * @param appId identificador do app Qlik Sense
     * @return nó JSON com o schema completo
     * @throws DataSourceUnavailableException se o servidor estiver inacessível ou retornar erro
     */
    public JsonNode getDataMetadata(String appId) {
        String url = baseUrl + "/api/v1/apps/" + appId + "/data/metadata";
        return get(url);
    }

    /**
     * Realiza uma requisição GET e retorna o corpo como {@link JsonNode}.
     *
     * <p>Em caso de erro de rede ({@link ResourceAccessException}) ou erro HTTP,
     * converte para {@link DataSourceUnavailableException} com a mensagem original.</p>
     *
     * @param url URL completa do endpoint
     * @return JSON de resposta
     * @throws DataSourceUnavailableException em caso de erro de rede ou HTTP
     */
    private JsonNode get(String url) {
        log.debug("GET {}", url);
        try {
            String body = restTemplate.getForObject(url, String.class);
            if (body == null) {
                throw new DataSourceUnavailableException("Resposta vazia do servidor Qlik: " + url);
            }
            return objectMapper.readTree(body);
        } catch (ResourceAccessException e) {
            throw new DataSourceUnavailableException(
                    "Não foi possível conectar ao servidor Qlik: " + e.getMessage());
        } catch (RestClientException e) {
            throw new DataSourceUnavailableException(
                    "Erro na requisição REST ao Qlik: " + e.getMessage());
        } catch (Exception e) {
            throw new DataSourceUnavailableException(
                    "Erro inesperado na comunicação com o Qlik: " + e.getMessage());
        }
    }
}
