package br.gov.ms.saude.ssd.domain.model;

import java.util.Map;

/**
 * Opções de consulta utilizadas para filtrar, paginar e ordenar dados
 * retornados por {@code DataSourcePort#getObjectData(String, QueryOptions)}.
 *
 * <p>Record imutável que concentra todos os parâmetros de consulta em um único
 * objeto de valor. Utiliza factory methods para os casos de uso mais comuns,
 * evitando a criação de instâncias com valores inválidos.</p>
 *
 * @param filters       mapa de filtros aplicados à consulta; as chaves são nomes
 *                      de campos e os valores são os critérios de filtro desejados.
 *                      Pode ser {@code null} ou vazio para nenhum filtro.
 * @param page          número da página a ser retornada (base zero)
 * @param pageSize      quantidade máxima de registros por página;
 *                      valores inválidos devem ser rejeitados por validação no use case
 * @param sortBy        nome do campo utilizado para ordenação dos resultados;
 *                      pode ser {@code null} para sem ordenação definida
 * @param sortDirection direção da ordenação: {@code "ASC"} ou {@code "DESC"};
 *                      ignorado se {@link #sortBy()} for {@code null}
 */
public record QueryOptions(
        Map<String, Object> filters,
        int page,
        int pageSize,
        String sortBy,
        String sortDirection
) {

    /**
     * Cria uma instância com configurações padrão: sem filtros, primeira página,
     * 100 registros por página e sem ordenação definida.
     *
     * <p>Conveniente para consultas exploratórias ou quando nenhuma customização
     * é necessária.</p>
     *
     * @return {@link QueryOptions} com valores padrão
     */
    public static QueryOptions defaultOptions() {
        return new QueryOptions(Map.of(), 0, 100, null, null);
    }

    /**
     * Cria uma instância de paginação simples, sem filtros e sem ordenação.
     *
     * <p>Útil quando se deseja apenas controlar a página e o tamanho da página,
     * mantendo os demais parâmetros neutros.</p>
     *
     * @param page     número da página (base zero)
     * @param pageSize quantidade de registros por página
     * @return {@link QueryOptions} com paginação definida e demais parâmetros padrão
     */
    public static QueryOptions of(int page, int pageSize) {
        return new QueryOptions(Map.of(), page, pageSize, null, null);
    }
}
