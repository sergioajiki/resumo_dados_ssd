package br.gov.ms.saude.ssd.adapter.out.qlik.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serialização e desserialização do protocolo JSON-RPC 2.0 usado pela Qlik Engine API.
 *
 * <p>A Qlik Engine API utiliza JSON-RPC 2.0 sobre WebSocket. Cada mensagem segue
 * o formato:</p>
 * <pre>
 * Requisição:  {"jsonrpc":"2.0","id":1,"method":"OpenDoc","handle":-1,"params":["appId"]}
 * Resposta:    {"jsonrpc":"2.0","id":1,"result":{"qReturn":{"qType":"Doc","qHandle":1}}}
 * Erro:        {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"..."}}
 * </pre>
 *
 * <p>O campo {@code handle} identifica o objeto Qlik sobre o qual o método é chamado:
 * {@code -1} para chamadas globais (ex: {@code OpenDoc}) e o handle retornado
 * pelo servidor para chamadas sobre objetos criados.</p>
 *
 * <p>O {@code id} é auto-incrementado por instância, garantindo correlação entre
 * requisições e respostas em sessões com múltiplas chamadas em andamento.</p>
 *
 * @see QlikWebSocketClient
 */
public class QlikJsonRpcProtocol {

    private static final Logger log = LoggerFactory.getLogger(QlikJsonRpcProtocol.class);

    /** Handle global — usado para chamadas que não pertencem a um objeto específico. */
    public static final int HANDLE_GLOBAL = -1;

    private final ObjectMapper objectMapper;
    private final AtomicInteger requestId = new AtomicInteger(1);

    /**
     * Cria uma nova instância do protocolo com o ObjectMapper fornecido.
     *
     * @param objectMapper serializador JSON compartilhado
     */
    public QlikJsonRpcProtocol(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Constrói uma mensagem JSON-RPC para abrir o documento (app) Qlik.
     *
     * <p>O método {@code OpenDoc} é sempre a primeira chamada após conectar.
     * Retorna o handle do documento, necessário para todas as chamadas subsequentes.</p>
     *
     * @param appId identificador do app Qlik a abrir
     * @return JSON serializado da mensagem RPC
     */
    public String buildOpenDoc(String appId) {
        return buildRequest(HANDLE_GLOBAL, "OpenDoc", appId);
    }

    /**
     * Constrói uma mensagem JSON-RPC para criar um objeto de sessão (HyperCube).
     *
     * <p>O HyperCube é o mecanismo de consulta tabular do Qlik. A definição
     * passada em {@code hyperCubeDef} especifica os campos a retornar e a
     * ordenação dos dados.</p>
     *
     * @param docHandle    handle do documento retornado por {@code OpenDoc}
     * @param hyperCubeDef definição do HyperCube construída por {@link QlikHyperCubeBuilder}
     * @return JSON serializado da mensagem RPC
     */
    public String buildCreateSessionObject(int docHandle, JsonNode hyperCubeDef) {
        return buildRequest(docHandle, "CreateSessionObject", hyperCubeDef);
    }

    /**
     * Constrói uma mensagem JSON-RPC para obter uma página de dados do HyperCube.
     *
     * <p>O Qlik limita o número de linhas por chamada. A paginação é feita
     * avançando o {@code top} (linha inicial) de {@code height} em {@code height}
     * até consumir todos os registros.</p>
     *
     * @param objectHandle handle do objeto de sessão (HyperCube)
     * @param top          linha inicial (base 0) desta página
     * @param height       número de linhas a retornar nesta página
     * @param width        número de colunas (deve corresponder ao total de dimensões + medidas)
     * @return JSON serializado da mensagem RPC
     */
    public String buildGetHyperCubeData(int objectHandle, int top, int height, int width) {
        ObjectNode pageRange = objectMapper.createObjectNode();
        pageRange.put("qLeft", 0);
        pageRange.put("qTop", top);
        pageRange.put("qWidth", width);
        pageRange.put("qHeight", height);

        ArrayNode pages = objectMapper.createArrayNode();
        pages.add(pageRange);

        return buildRequest(objectHandle, "GetHyperCubeData", "/qHyperCubeDef", pages);
    }

    /**
     * Constrói uma mensagem JSON-RPC para criar um objeto de listagem de campos (FieldList).
     *
     * <p>O FieldList é um objeto de sessão que lista todos os campos disponíveis
     * no modelo de dados do app, incluindo a tabela de origem de cada campo.
     * Funciona em sessões anônimas, ao contrário de {@code GetTablesAndKeys}.</p>
     *
     * @param docHandle handle do documento retornado por {@code OpenDoc}
     * @return JSON serializado da mensagem RPC
     */
    public String buildCreateFieldList(int docHandle) {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode qInfo = objectMapper.createObjectNode();
        qInfo.put("qType", "FieldList");
        root.set("qInfo", qInfo);

        ObjectNode qFieldListDef = objectMapper.createObjectNode();
        qFieldListDef.put("qShowSystem", false);
        qFieldListDef.put("qShowHidden", false);
        qFieldListDef.put("qShowSemantic", false);
        qFieldListDef.put("qShowSrcTables", true);
        qFieldListDef.put("qShowDefinitionOnly", false);
        qFieldListDef.put("qShowDerivedFields", false);
        root.set("qFieldListDef", qFieldListDef);

        return buildRequest(docHandle, "CreateSessionObject", root);
    }

    /**
     * Constrói uma mensagem JSON-RPC para obter o layout de um objeto de sessão.
     *
     * <p>Usado após {@link #buildCreateFieldList} para recuperar a lista de campos
     * do FieldList criado.</p>
     *
     * @param objectHandle handle do objeto criado por {@code CreateSessionObject}
     * @return JSON serializado da mensagem RPC
     */
    public String buildGetLayout(int objectHandle) {
        return buildRequest(objectHandle, "GetLayout");
    }

    /**
     * Constrói uma mensagem JSON-RPC para destruir um objeto de sessão.
     *
     * <p>Deve ser chamado ao final da extração para liberar recursos no servidor Qlik.
     * Não é obrigatório — a sessão WebSocket cleanup também destrói os objetos —
     * mas é boa prática para sessões longas.</p>
     *
     * @param docHandle    handle do documento
     * @param objectHandle handle do objeto a destruir
     * @return JSON serializado da mensagem RPC
     */
    public String buildDestroySessionObject(int docHandle, int objectHandle) {
        return buildRequest(docHandle, "DestroySessionObject", String.valueOf(objectHandle));
    }

    /**
     * Verifica se uma resposta JSON-RPC contém um erro.
     *
     * @param response nó JSON da resposta recebida do servidor
     * @return {@code true} se o campo {@code error} estiver presente e não nulo
     */
    public boolean isError(JsonNode response) {
        return response.has("error") && !response.get("error").isNull();
    }

    /**
     * Extrai a mensagem de erro de uma resposta JSON-RPC com falha.
     *
     * @param response nó JSON da resposta com erro
     * @return mensagem de erro, ou "Erro desconhecido" se não encontrada
     */
    public String getErrorMessage(JsonNode response) {
        JsonNode error = response.path("error");
        String message = error.path("message").asText(null);
        int code = error.path("code").asInt(0);
        return String.format("[%d] %s", code, message != null ? message : "Erro desconhecido");
    }

    /**
     * Extrai o handle de um objeto retornado por uma resposta RPC bem-sucedida.
     *
     * <p>Após {@code OpenDoc} e {@code CreateSessionObject}, o servidor retorna
     * o handle do objeto criado em {@code result.qReturn.qHandle}.</p>
     *
     * @param response nó JSON da resposta
     * @return handle do objeto, ou {@code -1} se não encontrado
     */
    public int extractHandle(JsonNode response) {
        return response.path("result").path("qReturn").path("qHandle").asInt(-1);
    }

    /**
     * Extrai as linhas de dados de uma resposta {@code GetHyperCubeData}.
     *
     * <p>Os dados ficam em {@code result.qDataPages[0].qMatrix}, onde cada elemento
     * de {@code qMatrix} é um array de células, cada uma com campos {@code qText}
     * (valor formatado) e {@code qNum} (valor numérico quando aplicável).</p>
     *
     * @param response nó JSON da resposta de GetHyperCubeData
     * @return nó array com as linhas de dados, ou array vazio se ausente
     */
    public JsonNode extractDataRows(JsonNode response) {
        JsonNode matrix = response.path("result")
                .path("qDataPages")
                .path(0)
                .path("qMatrix");
        return matrix.isMissingNode() ? objectMapper.createArrayNode() : matrix;
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    /**
     * Constrói o envelope JSON-RPC 2.0 com os parâmetros fornecidos.
     *
     * @param handle  handle do objeto Qlik alvo
     * @param method  nome do método RPC (ex: "OpenDoc", "GetHyperCubeData")
     * @param params  parâmetros do método (qualquer tipo serializável pelo Jackson)
     * @return JSON serializado
     */
    private String buildRequest(int handle, String method, Object... params) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("id", requestId.getAndIncrement());
            request.put("method", method);
            request.put("handle", handle);

            ArrayNode paramsArray = objectMapper.createArrayNode();
            for (Object param : params) {
                if (param instanceof JsonNode) {
                    paramsArray.add((JsonNode) param);
                } else if (param instanceof String s) {
                    paramsArray.add(s);
                } else if (param instanceof Integer i) {
                    paramsArray.add(i);
                } else {
                    paramsArray.add(objectMapper.valueToTree(param));
                }
            }
            request.set("params", paramsArray);

            String json = objectMapper.writeValueAsString(request);
            log.trace("RPC >> {}", json);
            return json;

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao serializar mensagem JSON-RPC: " + e.getMessage(), e);
        }
    }
}
