package br.gov.ms.saude.ssd.domain.exception;

/**
 * Lançada quando não é possível estabelecer conexão com a fonte de dados.
 *
 * <p>Indica uma falha de infraestrutura ou configuração que impede qualquer
 * operação de leitura ou verificação de saúde. Deve ser lançada pelos adaptadores
 * (ex: {@code QlikEngineAdapter}, {@code QlikRestAdapter}) antes mesmo de
 * tentar qualquer consulta, quando a conectividade básica não puder ser estabelecida.</p>
 *
 * <p>Exemplos de situações que devem lançar esta exceção:</p>
 * <ul>
 *   <li>Timeout na abertura do WebSocket com o Qlik Engine</li>
 *   <li>Falha de autenticação ao tentar conectar à fonte</li>
 *   <li>Host ou porta da fonte de dados inacessíveis na rede</li>
 *   <li>Certificado SSL inválido ou expirado na conexão</li>
 * </ul>
 *
 * @see DataExtractionException
 */
public class DataSourceUnavailableException extends RuntimeException {

    /**
     * Cria a exceção com uma mensagem descritiva do problema de conectividade.
     *
     * @param message descrição do motivo pelo qual a fonte está inacessível
     */
    public DataSourceUnavailableException(String message) {
        super(message);
    }

    /**
     * Cria a exceção com uma mensagem descritiva e a causa raiz.
     *
     * <p>Preferir este construtor quando a indisponibilidade é consequência de
     * outra exceção (ex: {@code java.net.ConnectException}, {@code SSLHandshakeException}),
     * para preservar o stack trace original e facilitar o diagnóstico.</p>
     *
     * @param message descrição do motivo pelo qual a fonte está inacessível
     * @param cause   exceção original que causou a indisponibilidade
     */
    public DataSourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
