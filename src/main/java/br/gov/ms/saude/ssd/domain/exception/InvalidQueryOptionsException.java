package br.gov.ms.saude.ssd.domain.exception;

/**
 * Lançada quando as opções de consulta fornecidas são inválidas e não podem
 * ser processadas pela fonte de dados ou pelo use case.
 *
 * <p>Deve ser lançada na camada de aplicação (use case) após validar os
 * parâmetros de {@code QueryOptions} ou de paginação, antes de delegar
 * à porta de saída — evitando que dados inválidos cheguem à camada de adaptadores.</p>
 *
 * <p>Exemplos de situações que devem lançar esta exceção:</p>
 * <ul>
 *   <li>Número de página negativo ({@code page < 0})</li>
 *   <li>Tamanho de página inválido ({@code pageSize <= 0} ou acima do máximo permitido)</li>
 *   <li>Direção de ordenação diferente de {@code "ASC"} ou {@code "DESC"}</li>
 *   <li>Campo de ordenação ({@code sortBy}) referenciando coluna inexistente</li>
 * </ul>
 */
public class InvalidQueryOptionsException extends RuntimeException {

    /** Nome do campo da opção de consulta que contém o valor inválido. */
    private final String field;

    /**
     * Cria a exceção identificando o campo inválido e descrevendo o motivo da rejeição.
     *
     * @param field  nome do campo que possui o valor inválido (ex: {@code "page"}, {@code "pageSize"})
     * @param reason explicação sobre por que o valor é inválido (ex: "deve ser maior que zero")
     */
    public InvalidQueryOptionsException(String field, String reason) {
        super(String.format("Opção de consulta inválida — campo '%s': %s", field, reason));
        this.field = field;
    }

    /**
     * Retorna o nome do campo que contém o valor inválido.
     *
     * <p>Útil para construir respostas de erro estruturadas na camada de apresentação,
     * indicando precisamente qual parâmetro deve ser corrigido pelo cliente.</p>
     *
     * @return nome do campo inválido
     */
    public String getField() {
        return field;
    }
}
