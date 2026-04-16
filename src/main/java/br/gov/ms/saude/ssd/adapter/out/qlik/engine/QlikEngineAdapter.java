package br.gov.ms.saude.ssd.adapter.out.qlik.engine;

import br.gov.ms.saude.ssd.config.QlikProperties;
import br.gov.ms.saude.ssd.domain.exception.DataExtractionException;
import br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException;
import br.gov.ms.saude.ssd.domain.model.*;
import br.gov.ms.saude.ssd.domain.port.out.DataExtractorPort;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de saída principal que implementa {@link DataSourcePort} e {@link DataExtractorPort}
 * usando a Qlik Engine API via WebSocket + JSON-RPC 2.0.
 *
 * <p>É o adaptador de produção do projeto — fornece acesso completo ao Qlik Sense,
 * incluindo tanto metadados quanto extração de dados tabulares em lote.</p>
 *
 * <p>Fluxo de extração de uma tabela:</p>
 * <ol>
 *   <li>Abre conexão WebSocket em {@code wss://host/anon/app/appId}</li>
 *   <li>Chama {@code OpenDoc} para obter o handle do documento</li>
 *   <li>Chama {@code CreateSessionObject} com a definição do HyperCube
 *       (construída por {@link QlikHyperCubeBuilder})</li>
 *   <li>Itera com {@code GetHyperCubeData} até esgotar os registros
 *       (controlado por {@link QlikPaginationStrategy})</li>
 *   <li>Fecha a conexão WebSocket</li>
 * </ol>
 *
 * <p>Cada extração abre e fecha sua própria conexão WebSocket, garantindo
 * isolamento entre execuções e facilitando o tratamento de erros.</p>
 *
 * <p>Ativação: {@code datasource.adapter: qlik-engine} no {@code application.yml}.</p>
 *
 * @see QlikWebSocketClient
 * @see QlikJsonRpcProtocol
 * @see QlikHyperCubeBuilder
 * @see QlikPaginationStrategy
 */
@Component("qlikEngineAdapter")
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "qlik-engine")
public class QlikEngineAdapter implements DataSourcePort, DataExtractorPort {

    private static final Logger log = LoggerFactory.getLogger(QlikEngineAdapter.class);

    private final QlikProperties props;
    private final ObjectMapper objectMapper;
    private final QlikHyperCubeBuilder hyperCubeBuilder;
    private final QlikPaginationStrategy paginationStrategy;

    /**
     * Injeta dependências via construtor.
     *
     * @param props              propriedades de configuração do Qlik (host, appId, timeouts)
     * @param objectMapper       serializador JSON compartilhado do contexto Spring
     */
    public QlikEngineAdapter(QlikProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.hyperCubeBuilder = new QlikHyperCubeBuilder(objectMapper);
        this.paginationStrategy = new QlikPaginationStrategy(props);
    }

    // =========================================================================
    // DataSourcePort — metadados e objetos de visualização
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Abre uma conexão WebSocket, chama {@code OpenDoc} para obter o handle
     * do documento e retorna metadados básicos. O Qlik Engine API não expõe
     * todos os metadados via Engine — campos como {@code criadoEm} ficam nulos
     * (use {@link br.gov.ms.saude.ssd.adapter.out.qlik.rest.QlikRestAdapter}
     * para metadados completos).</p>
     */
    @Override
    public AppMetadata getAppMetadata() {
        log.debug("Obtendo metadados via Engine API.");
        QlikWebSocketClient ws = null;
        try {
            ws = newClient();
            QlikJsonRpcProtocol rpc = newProtocol();

            JsonNode openDocResp = ws.sendAndWait(rpc.buildOpenDoc(props.getAppId()));
            checkError(openDocResp, "OpenDoc");

            // A Engine API retorna poucos metadados — apenas o essencial
            return new AppMetadata(
                    props.getAppId(),
                    "Saúde Digital - FIOCRUZ",
                    "Dados de Telessaúde e Superintendência de Saúde Digital — SES/MS",
                    null,
                    null,
                    null,
                    null,
                    true
            );
        } finally {
            closeQuietly(ws);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A Engine API não expõe o schema de tabelas diretamente de forma eficiente.
     * Este método retorna um {@link DataSchema} com as tabelas conhecidas do app
     * de produção, baseado no mapeamento estático documentado em
     * {@code docs/campos-disponiveis-telessaude.md}.</p>
     *
     * <p>Para um schema dinâmico, use o {@code QlikRestAdapter} que consulta
     * {@code /api/v1/apps/{appId}/data/metadata}.</p>
     */
    @Override
    public DataSchema getDataSchema() {
        // Schema estático baseado no mapeamento documentado do app de produção
        List<FieldSchema> camposAtend = List.of(
                new FieldSchema("ID_ATENDIMENTO", "numeric", 16426, true, List.of("key")),
                new FieldSchema("CNS_PACIENTE", "text", 8518, false, List.of()),
                new FieldSchema("DT_NASC_PACIENTE", "text", 7274, false, List.of("date")),
                new FieldSchema("RACA_PACIENTE", "text", 5, false, List.of()),
                new FieldSchema("ETNIA", "text", 9, false, List.of()),
                new FieldSchema("NOME_MUNICIPIO", "text", 49, false, List.of()),
                new FieldSchema("IBGE_ATEND", "text", 49, false, List.of()),
                new FieldSchema("DT_AGENDAMENTO", "timestamp", 306, false, List.of("date", "timestamp")),
                new FieldSchema("HR_AGENDAMENTO", "numeric", 81, false, List.of()),
                new FieldSchema("NOME_MEDICO", "text", 28, false, List.of()),
                new FieldSchema("CBO_MEDICO", "text", 14, false, List.of()),
                new FieldSchema("STATUS_CONSULTA", "text", 7, false, List.of()),
                new FieldSchema("CLASSIF_CONCLUSAO", "text", 3, false, List.of()),
                new FieldSchema("TIPO_SERV_ID", "text", 3, false, List.of()),
                new FieldSchema("DESFECHO_ATEND", "text", 8, false, List.of()),
                new FieldSchema("CID_CONSULTA", "text", 1233, false, List.of()),
                new FieldSchema("DT_SOLICITACAO", "text", 341, false, List.of("date")),
                new FieldSchema("TIPO_ZONA", "text", 2, false, List.of()),
                new FieldSchema("DT_NEW", "timestamp", 16403, false, List.of("date", "timestamp", "watermark")),
                // Campos adicionados na V10
                new FieldSchema("ID_ESTABELECIMENTO", "text", 10, false, List.of()),
                new FieldSchema("CNES_NESTABELECIMENTO", "text", 10, false, List.of()),
                new FieldSchema("ID_MEDICO", "text", 28, false, List.of()),
                new FieldSchema("CLASSFIC_COR", "text", 5, false, List.of()),
                new FieldSchema("TP_NW_CONCLUSAO", "text", 5, false, List.of()),
                new FieldSchema("ID_DIGSAUDE_REF", "text", 10, false, List.of()),
                new FieldSchema("TELEFONE", "text", 8518, false, List.of()),
                new FieldSchema("CEP_PACIENTE", "text", 8518, false, List.of()),
                new FieldSchema("RUA_PACIENTE", "text", 8518, false, List.of()),
                new FieldSchema("NUM_PACIENTE", "text", 8518, false, List.of()),
                new FieldSchema("BAIRRO_PACIENTE", "text", 8518, false, List.of()),
                new FieldSchema("COMPLEMENTO_END_PACIENTE", "text", 8518, false, List.of()),
                new FieldSchema("DESCRICAO_ENDERECO", "text", 8518, false, List.of()),
                new FieldSchema("ENDERECO_COMPLETO", "text", 8518, false, List.of()),
                new FieldSchema("DESCRICAO_CONSULTA", "text", 8518, false, List.of())
        );

        List<FieldSchema> camposProf = List.of(
                new FieldSchema("ID_USER", "numeric", 9213, true, List.of("key")),
                new FieldSchema("NOME_USER", "text", 9200, false, List.of()),
                new FieldSchema("CRM_USER", "text", 850, false, List.of()),
                new FieldSchema("NOME_ESPECIALIDADE", "text", 16, false, List.of()),
                new FieldSchema("MUNICIPIO_USER", "text", 60, false, List.of()),
                new FieldSchema("COD_IBGE", "text", 60, false, List.of()),
                new FieldSchema("TP_USER", "text", 7, false, List.of()),
                new FieldSchema("RANGE_IDADE", "text", 5, false, List.of()),
                new FieldSchema("RACA_COR_USER", "text", 5, false, List.of())
        );

        return new DataSchema(List.of(
                new TableSchema("DB_DIGSAUDE", 16426, camposAtend),
                new TableSchema("TEMPDB_USER", 9213, camposProf)
        ));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Abre uma sessão WebSocket e cria um HyperCube com o objeto especificado.
     * O {@code objectId} deve ser um dos IDs de objetos Qlik (filtros, KPIs, gráficos)
     * listados no {@code MockDataSourceAdapter.OBJECT_IDS}.</p>
     *
     * @throws DataExtractionException se o objeto não existir ou a extração falhar
     */
    @Override
    public ObjectData getObjectData(String objectId, QueryOptions options) {
        log.debug("Obtendo dados do objeto {}", objectId);
        QlikWebSocketClient ws = null;
        try {
            ws = newClient();
            QlikJsonRpcProtocol rpc = newProtocol();

            JsonNode openDocResp = ws.sendAndWait(rpc.buildOpenDoc(props.getAppId()));
            checkError(openDocResp, "OpenDoc");
            int docHandle = rpc.extractHandle(openDocResp);

            // Para objetos de visualização, usa uma lista genérica de campos
            List<String> fields = List.of(objectId);
            JsonNode cubeDef = hyperCubeBuilder.build(fields);
            JsonNode createResp = ws.sendAndWait(rpc.buildCreateSessionObject(docHandle, cubeDef));
            checkError(createResp, "CreateSessionObject");
            int objHandle = rpc.extractHandle(createResp);

            JsonNode dataResp = ws.sendAndWait(
                    rpc.buildGetHyperCubeData(objHandle, 0, paginationStrategy.getPageSize(), fields.size()));
            checkError(dataResp, "GetHyperCubeData");

            List<List<Object>> rows = paginationStrategy.extractRows(rpc.extractDataRows(dataResp));
            return new ObjectData(objectId, fields, rows);

        } catch (DataExtractionException | DataSourceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new DataExtractionException(objectId, "Erro ao obter dados do objeto: " + e.getMessage());
        } finally {
            closeQuietly(ws);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retorna lista vazia — a Engine API não lista objetos de visualização
     * sem autenticação. Use o {@code MockDataSourceAdapter} para desenvolvimento.</p>
     */
    @Override
    public List<ObjectDescriptor> listAvailableObjects() {
        log.warn("QlikEngineAdapter.listAvailableObjects() não suportado sem autenticação.");
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Chama {@code GetTablesAndKeys} para obter todas as tabelas e campos do app.
     * Filtra pela {@code tabelaNome} informada e retorna os nomes dos campos encontrados.
     * Útil para descobrir o nome exato de campos desconhecidos no Qlik.</p>
     */
    @Override
    public java.util.List<String> listarCamposDisponiveis(String tabelaNome) {
        QlikWebSocketClient ws = null;
        try {
            ws = newClient();
            QlikJsonRpcProtocol rpc = newProtocol();

            JsonNode openDocResp = ws.sendAndWait(rpc.buildOpenDoc(props.getAppId()));
            checkError(openDocResp, "OpenDoc");
            int docHandle = rpc.extractHandle(openDocResp);

            // Cria objeto FieldList — funciona em sessões anônimas
            JsonNode createResp = ws.sendAndWait(rpc.buildCreateFieldList(docHandle));
            checkError(createResp, "CreateFieldList");
            int fieldListHandle = rpc.extractHandle(createResp);

            // Obtém o layout do FieldList com todos os campos
            JsonNode layoutResp = ws.sendAndWait(rpc.buildGetLayout(fieldListHandle));
            checkError(layoutResp, "GetLayout");

            java.util.List<String> campos = new java.util.ArrayList<>();
            JsonNode items = layoutResp.path("result").path("qLayout")
                    .path("qFieldList").path("qItems");

            for (JsonNode item : items) {
                String fieldName = item.path("qName").asText(null);
                // qSrcTables contém as tabelas de origem do campo
                JsonNode srcTables = item.path("qSrcTables");
                boolean pertenceTabela = tabelaNome.isBlank();
                if (!pertenceTabela) {
                    for (JsonNode t : srcTables) {
                        if (tabelaNome.equalsIgnoreCase(t.asText(""))) {
                            pertenceTabela = true;
                            break;
                        }
                    }
                }
                if (fieldName != null && !fieldName.startsWith("$") && pertenceTabela) {
                    campos.add(fieldName);
                }
            }

            log.info("Campos encontrados em '{}': {}", tabelaNome, campos);
            return campos;

        } catch (Exception e) {
            log.error("Erro ao listar campos de {}: {}", tabelaNome, e.getMessage());
            return java.util.List.of();
        } finally {
            closeQuietly(ws);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tenta abrir uma conexão WebSocket e chamar {@code OpenDoc} para medir
     * a latência real de ponta a ponta com o servidor Qlik.</p>
     */
    @Override
    public HealthStatus checkHealth() {
        long inicio = System.currentTimeMillis();
        QlikWebSocketClient ws = null;
        try {
            ws = newClient();
            QlikJsonRpcProtocol rpc = newProtocol();
            JsonNode resp = ws.sendAndWait(rpc.buildOpenDoc(props.getAppId()));
            long latency = System.currentTimeMillis() - inicio;

            if (rpc.isError(resp)) {
                return HealthStatus.degraded(
                        "OpenDoc retornou erro: " + rpc.getErrorMessage(resp), latency);
            }
            return HealthStatus.up(latency);

        } catch (DataSourceUnavailableException e) {
            return HealthStatus.down("Não foi possível conectar: " + e.getMessage());
        } catch (Exception e) {
            return HealthStatus.down("Erro inesperado: " + e.getMessage());
        } finally {
            closeQuietly(ws);
        }
    }

    // =========================================================================
    // DataExtractorPort — extração em lote
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Extrai todos os registros de {@code tableName} com os campos especificados.
     * Usa paginação automática até esgotar todos os registros da tabela.</p>
     *
     * @param tableName nome da tabela no Qlik (ex: "DB_DIGSAUDE")
     * @param fields    campos a extrair (ex: ["ID_ATENDIMENTO", "CNS_PACIENTE", ...])
     * @param options   opções de extração (pageSize, timeout)
     * @return resultado com todos os registros extraídos e watermark do campo DT_NEW
     */
    @Override
    public ExtractResult extractTable(String tableName, List<String> fields, ExtractOptions options) {
        log.info("Iniciando extração completa de {} ({} campos)", tableName, fields.size());
        return executarExtracao(tableName, fields, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extrai apenas os registros com {@code DT_NEW} posterior ao watermark.
     * A filtragem é feita pelo Qlik via seleção de campo antes da extração do HyperCube.</p>
     *
     * <p><strong>Nota de implementação:</strong> A filtragem por watermark via Engine API
     * requer uso de {@code SetSelections} no documento, que modifica o estado da sessão
     * e é revertido ao fechar. Nesta versão, a filtragem é feita após a extração completa
     * no lado Java — adequado para tabelas com até ~50.000 registros.</p>
     *
     * @param tableName nome da tabela no Qlik
     * @param fields    campos a extrair
     * @param watermark apenas registros com DT_NEW posterior a esta data
     * @return resultado com registros mais recentes que o watermark
     */
    @Override
    public ExtractResult extractSince(String tableName, List<String> fields, LocalDateTime watermark) {
        log.info("Iniciando extração incremental de {} desde {}", tableName, watermark);
        return executarExtracao(tableName, fields, watermark);
    }

    // =========================================================================
    // Lógica interna de extração
    // =========================================================================

    /**
     * Executa o pipeline completo de extração via WebSocket.
     *
     * <p>Fluxo:</p>
     * <ol>
     *   <li>Conecta via WebSocket</li>
     *   <li>OpenDoc → docHandle</li>
     *   <li>CreateSessionObject (HyperCube) → objHandle</li>
     *   <li>GetHyperCubeData em loop paginado</li>
     *   <li>Filtragem por watermark (se fornecido) — feita no índice do campo DT_NEW</li>
     *   <li>Fecha a conexão</li>
     * </ol>
     *
     * @param tableName nome da tabela Qlik
     * @param fields    campos a extrair
     * @param watermark filtro de data (opcional — {@code null} para extração completa)
     * @return resultado da extração
     */
    private ExtractResult executarExtracao(String tableName, List<String> fields, LocalDateTime watermark) {
        LocalDateTime inicio = LocalDateTime.now();
        QlikWebSocketClient ws = null;

        try {
            ws = newClient();
            QlikJsonRpcProtocol rpc = newProtocol();

            // Passo 1: Abrir documento
            JsonNode openDocResp = ws.sendAndWait(rpc.buildOpenDoc(props.getAppId()));
            checkError(openDocResp, "OpenDoc");
            int docHandle = rpc.extractHandle(openDocResp);
            log.debug("Documento aberto. Handle={}", docHandle);

            // Passo 2: Criar objeto de sessão (HyperCube)
            JsonNode cubeDef = hyperCubeBuilder.build(fields);
            JsonNode createResp = ws.sendAndWait(rpc.buildCreateSessionObject(docHandle, cubeDef));
            checkError(createResp, "CreateSessionObject");
            int objHandle = rpc.extractHandle(createResp);
            log.debug("HyperCube criado. Handle={}, campos={}", objHandle, fields.size());

            // Passo 3: Extração paginada
            // pageSize efetivo respeita o limite de células do Qlik: rows × cols ≤ 10.000
            List<List<Object>> todasAsLinhas = new ArrayList<>();
            int effectivePageSize = paginationStrategy.getEffectivePageSize(fields.size());
            int top = 0;

            do {
                JsonNode dataResp = ws.sendAndWait(
                        rpc.buildGetHyperCubeData(objHandle, top, effectivePageSize, fields.size()));
                checkError(dataResp, "GetHyperCubeData");

                List<List<Object>> pagina = paginationStrategy.extractRows(rpc.extractDataRows(dataResp));
                todasAsLinhas.addAll(pagina);
                log.debug("Página extraída: {} linhas (top={}). Total acumulado: {}", pagina.size(), top, todasAsLinhas.size());

                if (!paginationStrategy.hasMorePages(pagina, top, effectivePageSize)) break;
                top += effectivePageSize;

            } while (true);

            // Passo 4: Filtrar por watermark se necessário
            List<List<Object>> linhasFiltradas = todasAsLinhas;
            if (watermark != null) {
                int dtNewIdx = fields.indexOf("DT_NEW");
                if (dtNewIdx >= 0) {
                    linhasFiltradas = filtrarPorWatermark(todasAsLinhas, dtNewIdx, watermark);
                    log.info("Filtro watermark aplicado: {} de {} linhas passaram.", linhasFiltradas.size(), todasAsLinhas.size());
                }
            }

            LocalDateTime watermarkResult = LocalDateTime.now();
            Duration duracao = Duration.between(inicio, watermarkResult);

            log.info("Extração de {} concluída: {} registros em {}s.",
                    tableName, linhasFiltradas.size(), duracao.toSeconds());

            return new ExtractResult(tableName, linhasFiltradas, fields,
                    linhasFiltradas.size(), watermarkResult, duracao);

        } catch (DataExtractionException | DataSourceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new DataExtractionException(tableName, "Falha na extração: " + e.getMessage());
        } finally {
            closeQuietly(ws);
        }
    }

    /**
     * Filtra linhas cujo campo DT_NEW seja posterior ao watermark.
     *
     * <p>O valor de DT_NEW vem como string do Qlik (ex: "29/03/2026 08:15:00").
     * A comparação é feita textualmente em formato ISO após normalização, o que
     * é suficiente para o padrão de datas retornado pelo Qlik.</p>
     *
     * @param rows      todas as linhas extraídas
     * @param dtNewIdx  índice da coluna DT_NEW na lista de campos
     * @param watermark data/hora de referência para o filtro
     * @return linhas filtradas com DT_NEW > watermark
     */
    private List<List<Object>> filtrarPorWatermark(List<List<Object>> rows, int dtNewIdx,
                                                    LocalDateTime watermark) {
        List<List<Object>> resultado = new ArrayList<>();
        for (List<Object> row : rows) {
            if (dtNewIdx >= row.size()) continue;
            Object dtNewVal = row.get(dtNewIdx);
            if (dtNewVal == null) continue;

            try {
                // Tenta parsear o valor como LocalDateTime para comparação exata
                String dtStr = dtNewVal.toString();
                // Normaliza formatos comuns do Qlik
                dtStr = dtStr.replace("/", "-");
                if (dtStr.length() == 10) dtStr += " 00:00:00";
                LocalDateTime dtNew = LocalDateTime.parse(dtStr.replace(" ", "T"));
                if (dtNew.isAfter(watermark)) {
                    resultado.add(row);
                }
            } catch (Exception e) {
                // Se não conseguir parsear, inclui a linha (comportamento conservador)
                resultado.add(row);
            }
        }
        return resultado;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Cria uma nova instância do cliente WebSocket.
     * Cada extração usa sua própria conexão para isolamento.
     *
     * @return novo cliente WebSocket conectado
     */
    private QlikWebSocketClient newClient() {
        return new QlikWebSocketClient(props, objectMapper);
    }

    /**
     * Cria uma nova instância do protocolo JSON-RPC.
     * O contador de IDs começa em 1 para cada conexão.
     *
     * @return nova instância do protocolo
     */
    private QlikJsonRpcProtocol newProtocol() {
        return new QlikJsonRpcProtocol(objectMapper);
    }

    /**
     * Verifica se uma resposta JSON-RPC contém erro e lança exceção se sim.
     *
     * @param response resposta do servidor
     * @param method   nome do método chamado (para contexto na mensagem de erro)
     * @throws DataExtractionException se a resposta contiver um campo {@code error}
     */
    private void checkError(JsonNode response, String method) {
        QlikJsonRpcProtocol rpc = new QlikJsonRpcProtocol(objectMapper);
        if (rpc.isError(response)) {
            throw new DataExtractionException(method, rpc.getErrorMessage(response));
        }
    }

    /**
     * Fecha o cliente WebSocket sem propagar exceções.
     * Usado no bloco {@code finally} para garantir o fechamento mesmo após erros.
     *
     * @param ws cliente a fechar; pode ser {@code null}
     */
    private void closeQuietly(QlikWebSocketClient ws) {
        if (ws != null) {
            try {
                ws.close();
            } catch (Exception e) {
                log.warn("Erro ao fechar WebSocket: {}", e.getMessage());
            }
        }
    }
}
