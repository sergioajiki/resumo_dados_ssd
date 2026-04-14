package br.gov.ms.saude.ssd.domain.model;

/**
 * Opções de configuração para o processo de extração de dados em lote.
 *
 * <p>Record imutável utilizado por {@code DataExtractorPort#extractTable} e
 * {@code DataExtractorPort#extractSince} para controlar o comportamento
 * da extração: tamanho das páginas, timeout e tratamento de valores nulos.</p>
 *
 * <p>Use {@link #defaults()} para o comportamento padrão recomendado para
 * o dataset do Núcleo de Telessaúde MS (páginas de 5.000 registros).</p>
 *
 * @param pageSize       número de registros por página durante a extração paginada;
 *                       valores muito altos podem causar timeout ou esgotamento de memória
 * @param timeoutSeconds tempo máximo em segundos para aguardar resposta da fonte
 *                       em cada requisição de página; após este limite, uma exceção é lançada
 * @param includeNulls   quando {@code true}, linhas com campos nulos são incluídas
 *                       no resultado; quando {@code false}, linhas com qualquer campo
 *                       nulo são descartadas durante a extração
 */
public record ExtractOptions(
        int pageSize,
        int timeoutSeconds,
        boolean includeNulls
) {

    /**
     * Cria uma instância com as configurações padrão otimizadas para o dataset
     * do Núcleo de Telessaúde MS: páginas de 5.000 registros, timeout de 30 segundos
     * e inclusão de valores nulos.
     *
     * @return {@link ExtractOptions} com {@code pageSize=5000}, {@code timeoutSeconds=30}
     *         e {@code includeNulls=true}
     */
    public static ExtractOptions defaults() {
        return new ExtractOptions(5000, 30, true);
    }
}
