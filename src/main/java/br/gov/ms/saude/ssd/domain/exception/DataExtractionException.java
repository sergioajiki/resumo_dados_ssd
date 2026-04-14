package br.gov.ms.saude.ssd.domain.exception;

/**
 * Lançada quando ocorre erro durante a extração de dados de uma tabela específica.
 *
 * <p>Diferente de {@link DataSourceUnavailableException}, esta exceção indica que
 * a conexão com a fonte pôde ser estabelecida, mas falhou durante o processo de
 * leitura dos dados — por exemplo, ao paginar os resultados ou ao serializar
 * a resposta do servidor.</p>
 *
 * <p>Exemplos de situações que devem lançar esta exceção:</p>
 * <ul>
 *   <li>Erro de protocolo JSON-RPC durante a paginação via WebSocket</li>
 *   <li>Tabela solicitada não existe ou não está acessível na fonte</li>
 *   <li>Resposta da fonte com estrutura inesperada (campo faltando, tipo incorreto)</li>
 *   <li>Timeout durante a leitura de uma página intermediária da extração</li>
 *   <li>Sessão expirada durante a extração de tabelas grandes</li>
 * </ul>
 *
 * @see DataSourceUnavailableException
 */
public class DataExtractionException extends RuntimeException {

    /** Nome da tabela na qual o erro de extração ocorreu. */
    private final String tableName;

    /**
     * Cria a exceção identificando a tabela afetada e descrevendo o erro.
     *
     * @param tableName nome da tabela na qual a extração falhou (ex: "DB_DIGSAUDE")
     * @param message   descrição do erro ocorrido durante a extração
     */
    public DataExtractionException(String tableName, String message) {
        super(String.format("Erro ao extrair tabela '%s': %s", tableName, message));
        this.tableName = tableName;
    }

    /**
     * Cria a exceção identificando a tabela afetada, descrevendo o erro e preservando a causa raiz.
     *
     * <p>Preferir este construtor quando a falha é consequência de outra exceção,
     * para manter o stack trace original e facilitar o diagnóstico em logs.</p>
     *
     * @param tableName nome da tabela na qual a extração falhou (ex: "DB_DIGSAUDE")
     * @param message   descrição do erro ocorrido durante a extração
     * @param cause     exceção original que causou a falha de extração
     */
    public DataExtractionException(String tableName, String message, Throwable cause) {
        super(String.format("Erro ao extrair tabela '%s': %s", tableName, message), cause);
        this.tableName = tableName;
    }

    /**
     * Retorna o nome da tabela na qual o erro de extração ocorreu.
     *
     * <p>Útil para correlacionar a exceção com o {@code SyncLog} correspondente
     * e registrar a falha na tabela correta via {@code SyncRepositoryPort}.</p>
     *
     * @return nome da tabela afetada
     */
    public String getTableName() {
        return tableName;
    }
}
