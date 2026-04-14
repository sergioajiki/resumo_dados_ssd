package br.gov.ms.saude.ssd.domain.exception;

/**
 * Lançada quando uma tentativa de iniciar uma sincronização é feita enquanto
 * outra sincronização para a mesma tabela já está em andamento.
 *
 * <p>Protege contra execuções concorrentes do pipeline ETL que poderiam causar
 * duplicação de dados, race conditions na atualização do watermark ou
 * sobrecarga desnecessária na fonte de dados.</p>
 *
 * <p>Deve ser verificada pelo serviço de sincronização antes de criar um novo
 * {@code SyncLog} com status {@code RUNNING}, consultando
 * {@code SyncRepositoryPort} para detectar execuções ativas.</p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>{@code
 * if (syncRepository.hasRunningSync(tableName)) {
 *     throw new SyncAlreadyRunningException(tableName);
 * }
 * }</pre>
 */
public class SyncAlreadyRunningException extends RuntimeException {

    /**
     * Cria a exceção indicando o nome da tabela com sincronização em andamento.
     *
     * @param tableName nome da tabela que já possui uma sincronização ativa
     */
    public SyncAlreadyRunningException(String tableName) {
        super(String.format(
                "Já existe uma sincronização em andamento para a tabela '%s'. "
                + "Aguarde a conclusão antes de iniciar uma nova execução.",
                tableName));
    }
}
