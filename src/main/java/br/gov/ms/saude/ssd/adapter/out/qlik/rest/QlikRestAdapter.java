package br.gov.ms.saude.ssd.adapter.out.qlik.rest;

import br.gov.ms.saude.ssd.config.QlikProperties;
import br.gov.ms.saude.ssd.domain.exception.DataExtractionException;
import br.gov.ms.saude.ssd.domain.model.*;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de saída que implementa {@link DataSourcePort} usando a REST API pública do Qlik Sense.
 *
 * <p>A REST API do Qlik Sense Enterprise expõe endpoints HTTP públicos para metadados.
 * Este adaptador é adequado para operações de consulta de schema e metadados do app,
 * mas <strong>não suporta extração de dados</strong> — para isso, use o
 * {@code QlikEngineAdapter} (WebSocket + JSON-RPC).</p>
 *
 * <p>Ativação: {@code datasource.adapter: qlik-rest} no {@code application.yml}.
 * Útil para ambientes onde apenas metadados são necessários (ex: diagnóstico de configuração).</p>
 *
 * <p>Endpoints utilizados (públicos, sem autenticação no virtual proxy anônimo):</p>
 * <ul>
 *   <li>{@code GET /api/v1/apps/{appId}} — metadados do app</li>
 *   <li>{@code GET /api/v1/apps/{appId}/data/metadata} — schema de campos</li>
 * </ul>
 *
 * @see QlikRestClient
 * @see br.gov.ms.saude.ssd.adapter.out.qlik.engine.QlikEngineAdapter
 */
@Component("qlikRestAdapter")
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "qlik-rest")
public class QlikRestAdapter implements DataSourcePort {

    private static final Logger log = LoggerFactory.getLogger(QlikRestAdapter.class);

    private final QlikRestClient client;
    private final QlikProperties props;

    /**
     * Injeta o cliente HTTP e as propriedades de configuração via construtor.
     *
     * @param client cliente HTTP para os endpoints REST do Qlik
     * @param props  propriedades de configuração (host, appId, timeouts)
     */
    public QlikRestAdapter(QlikRestClient client, QlikProperties props) {
        this.client = client;
        this.props = props;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Chama {@code GET /api/v1/apps/{appId}} e mapeia os campos do JSON para
     * o domain object {@link AppMetadata}. O campo {@code attributes} do JSON
     * Qlik contém os metadados relevantes.</p>
     */
    @Override
    public AppMetadata getAppMetadata() {
        log.debug("Obtendo metadados do app {}", props.getAppId());
        JsonNode json = client.getAppMetadata(props.getAppId());
        JsonNode attrs = json.path("attributes");

        return new AppMetadata(
                props.getAppId(),
                attrs.path("name").asText(""),
                attrs.path("description").asText(null),
                attrs.path("owner").path("name").asText(null),
                parseDateTime(attrs.path("createdDate").asText(null)),
                parseDateTime(attrs.path("publishTime").asText(null)),
                parseDateTime(attrs.path("lastReloadTime").asText(null)),
                attrs.path("published").asBoolean(false)
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Chama {@code GET /api/v1/apps/{appId}/data/metadata} e converte a resposta
     * em um {@link DataSchema}. O JSON retornado pelo Qlik contém as tabelas no campo
     * {@code tables} e os campos de cada tabela no sub-array {@code fields}.</p>
     */
    @Override
    public DataSchema getDataSchema() {
        log.debug("Obtendo schema do app {}", props.getAppId());
        JsonNode json = client.getDataMetadata(props.getAppId());
        List<TableSchema> tabelas = new ArrayList<>();

        JsonNode tables = json.path("tables");
        if (tables.isArray()) {
            for (JsonNode table : tables) {
                String nome = table.path("name").asText();
                long totalRegistros = table.path("noOfRows").asLong(0);

                List<FieldSchema> campos = new ArrayList<>();
                JsonNode fields = table.path("fields");
                if (fields.isArray()) {
                    for (JsonNode field : fields) {
                        campos.add(new FieldSchema(
                                field.path("name").asText(),
                                field.path("type").asText("text"),
                                (int) field.path("cardinalValues").asLong(0),
                                field.path("keyType").asText("NOT_KEY").equals("PRIMARY_KEY"),
                                List.of()
                        ));
                    }
                }

                tabelas.add(new TableSchema(nome, totalRegistros, campos));
            }
        }

        return new DataSchema(tabelas);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A REST API não suporta consulta de objetos de visualização.
     * Este método lança {@link DataExtractionException} para indicar que a operação
     * não está disponível neste adaptador — use {@code QlikEngineAdapter} para isso.</p>
     *
     * @throws DataExtractionException sempre — REST API não suporta esta operação
     */
    @Override
    public ObjectData getObjectData(String objectId, QueryOptions options) {
        throw new DataExtractionException(objectId,
                "QlikRestAdapter não suporta getObjectData. Use QlikEngineAdapter.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>A REST API não lista objetos de visualização. Retorna lista vazia.</p>
     */
    @Override
    public List<ObjectDescriptor> listAvailableObjects() {
        log.warn("QlikRestAdapter.listAvailableObjects() retorna lista vazia — use QlikEngineAdapter.");
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verifica a saúde medindo o tempo de resposta de {@link #getAppMetadata()}.
     * Um erro de rede resulta em status {@link HealthStatus.HealthStatusEnum#DOWN}.</p>
     */
    @Override
    public HealthStatus checkHealth() {
        long inicio = System.currentTimeMillis();
        try {
            getAppMetadata();
            long latency = System.currentTimeMillis() - inicio;
            return HealthStatus.up(latency);
        } catch (Exception e) {
            return HealthStatus.down("Falha ao conectar ao Qlik REST API: " + e.getMessage());
        }
    }

    /**
     * Converte uma string ISO de data/hora para {@link LocalDateTime}.
     * Retorna {@code null} se a string for nula ou inválida.
     *
     * @param text string no formato ISO-8601 (ex: "2026-03-29T08:15:00Z")
     * @return {@link LocalDateTime} correspondente, ou {@code null} se inválida
     */
    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            // Remove o sufixo de timezone para compatibilidade com LocalDateTime
            return LocalDateTime.parse(text.replace("Z", "").replaceAll("\\+.*", ""));
        } catch (Exception e) {
            log.debug("Não foi possível parsear datetime '{}': {}", text, e.getMessage());
            return null;
        }
    }
}
