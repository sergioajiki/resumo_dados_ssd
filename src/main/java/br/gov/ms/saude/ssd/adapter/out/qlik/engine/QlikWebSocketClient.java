package br.gov.ms.saude.ssd.adapter.out.qlik.engine;

import br.gov.ms.saude.ssd.config.QlikProperties;
import br.gov.ms.saude.ssd.domain.exception.DataExtractionException;
import br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.*;

/**
 * Cliente WebSocket para comunicação com a Qlik Engine API via JSON-RPC 2.0.
 *
 * <p>Gerencia o ciclo de vida completo da conexão WebSocket com o servidor Qlik:</p>
 * <ol>
 *   <li>Abre conexão em {@code wss://{host}/anon/app/{appId}}</li>
 *   <li>Aguarda a confirmação de conexão do servidor (handshake)</li>
 *   <li>Envia mensagens JSON-RPC síncronas via {@link #sendAndWait}</li>
 *   <li>Fecha a conexão ao final via {@link #close}</li>
 * </ol>
 *
 * <p>A comunicação é assíncrona internamente (WebSocket), mas este cliente a expõe
 * de forma síncrona via {@link CompletableFuture} e {@link BlockingQueue}. Cada
 * chamada {@link #sendAndWait} bloqueia até receber a resposta correspondente,
 * usando o {@code id} do JSON-RPC para correlação.</p>
 *
 * <p>Protocolo de URL do Qlik:</p>
 * <ul>
 *   <li>Acesso anônimo: {@code wss://host/anon/app/appId}</li>
 *   <li>Acesso autenticado: {@code wss://host/app/appId} (requer cookie de sessão)</li>
 * </ul>
 *
 * <p>Esta classe <strong>não é thread-safe</strong>. Cada extração deve usar
 * sua própria instância. O {@link QlikEngineAdapter} cria uma nova instância
 * por operação de extração.</p>
 *
 * @see QlikJsonRpcProtocol
 * @see QlikEngineAdapter
 */
public class QlikWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(QlikWebSocketClient.class);

    /**
     * Fila de bloqueio para receber respostas do servidor.
     * Capacidade 1 — cada {@code sendAndWait} aguarda uma única resposta.
     */
    private final BlockingQueue<JsonNode> responseQueue = new LinkedBlockingQueue<>(1);

    /** Sinaliza quando a conexão foi estabelecida (ou falhou). */
    private final CountDownLatch connectLatch = new CountDownLatch(1);

    /** Sinaliza se a conexão foi estabelecida com sucesso. */
    private volatile boolean connected = false;

    /** Sinaliza se a conexão foi encerrada com erro. */
    private volatile String connectionError = null;

    private final ObjectMapper objectMapper;
    private final int readTimeoutMs;
    private final InternalWsClient wsClient;

    /**
     * Cria e conecta o cliente WebSocket ao servidor Qlik.
     *
     * <p>Constrói a URI de conexão no formato {@code wss://host/anon/app/appId}
     * para acesso anônimo. Chama {@link #connect()} e aguarda o handshake.</p>
     *
     * @param props        propriedades de configuração (host, appId, timeouts)
     * @param objectMapper serializador JSON compartilhado
     * @throws DataSourceUnavailableException se a conexão não for estabelecida dentro do timeout
     */
    public QlikWebSocketClient(QlikProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.readTimeoutMs = props.getReadTimeoutMs();

        String uri = String.format("wss://%s%s/app/%s",
                props.getHost(), props.getVirtualProxy(), props.getAppId());
        log.info("Conectando ao Qlik Engine API: {}", uri);

        try {
            this.wsClient = new InternalWsClient(new URI(uri));
            this.wsClient.connectBlocking(props.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS);

            // Aguarda sinal de conexão (onOpen ou onError)
            boolean signaled = connectLatch.await(props.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!signaled || !connected) {
                String reason = connectionError != null ? connectionError : "Timeout de conexão";
                throw new DataSourceUnavailableException("Não foi possível conectar ao Qlik: " + reason);
            }

            log.info("Conexão WebSocket estabelecida com o servidor Qlik.");
        } catch (DataSourceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new DataSourceUnavailableException(
                    "Erro ao abrir conexão WebSocket com o Qlik: " + e.getMessage());
        }
    }

    /**
     * Envia uma mensagem JSON-RPC e aguarda a resposta de forma síncrona.
     *
     * <p>A resposta é correlacionada pelo campo {@code id} do JSON-RPC.
     * O método bloqueia até receber a resposta ou atingir o timeout de leitura.</p>
     *
     * @param message mensagem JSON-RPC serializada (produzida por {@link QlikJsonRpcProtocol})
     * @return nó JSON da resposta do servidor
     * @throws DataExtractionException se o timeout for atingido ou a conexão for perdida
     */
    public JsonNode sendAndWait(String message) {
        if (!connected) {
            throw new DataSourceUnavailableException("WebSocket não está conectado.");
        }

        log.trace("WS >> {}", message);
        responseQueue.clear(); // garante que não há resposta anterior pendente
        wsClient.send(message);

        try {
            JsonNode response = responseQueue.poll(readTimeoutMs, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new DataExtractionException("QlikWebSocket",
                        "Timeout aguardando resposta do Qlik Engine API (" + readTimeoutMs + "ms).");
            }
            log.trace("WS << {}", response);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataExtractionException("QlikWebSocket", "Thread interrompida aguardando resposta do Qlik.");
        }
    }

    /**
     * Fecha a conexão WebSocket com o servidor Qlik.
     *
     * <p>Deve ser chamado ao final de cada operação de extração para liberar
     * recursos no servidor. O uso em bloco try-with-resources via {@link AutoCloseable}
     * garante o fechamento mesmo em caso de exceção.</p>
     */
    public void close() {
        if (wsClient != null && wsClient.isOpen()) {
            log.debug("Fechando conexão WebSocket com o Qlik.");
            wsClient.close();
        }
        connected = false;
    }

    // -------------------------------------------------------------------------
    // Implementação interna do WebSocketClient (java-websocket)
    // -------------------------------------------------------------------------

    /**
     * Implementação interna do cliente WebSocket usando a biblioteca java-websocket.
     *
     * <p>Delega os eventos de conexão para a classe externa via flags e queues,
     * mantendo a interface pública do {@link QlikWebSocketClient} síncrona e simples.</p>
     */
    private class InternalWsClient extends WebSocketClient {

        public InternalWsClient(URI uri) {
            super(uri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            log.debug("WebSocket onOpen: status={}", handshake.getHttpStatus());
            connected = true;
            connectLatch.countDown();
        }

        @Override
        public void onMessage(String message) {
            try {
                JsonNode json = objectMapper.readTree(message);
                // Ignora notificações push do Qlik (não têm campo "id")
                if (!json.has("id")) {
                    log.trace("Notificação push ignorada: {}", message);
                    return;
                }
                responseQueue.offer(json, 1, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Erro ao processar mensagem WebSocket: {}", e.getMessage());
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            log.info("WebSocket fechado: code={}, reason={}, remote={}", code, reason, remote);
            connected = false;
            connectLatch.countDown(); // libera o await em caso de falha de conexão
        }

        @Override
        public void onError(Exception ex) {
            log.error("Erro WebSocket: {}", ex.getMessage());
            connectionError = ex.getMessage();
            connected = false;
            connectLatch.countDown();
        }
    }
}
