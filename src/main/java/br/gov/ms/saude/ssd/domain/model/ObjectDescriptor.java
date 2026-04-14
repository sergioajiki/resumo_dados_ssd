package br.gov.ms.saude.ssd.domain.model;

/**
 * Descreve um objeto disponível na fonte de dados (gráfico, KPI, filtro ou tabela).
 *
 * <p>Record imutável usado na descoberta de objetos via
 * {@code DataSourcePort#listAvailableObjects()}. Cada objeto representa
 * um elemento visual/analítico do app Qlik Sense que pode ser consultado
 * para extração de dados.</p>
 *
 * @param id        identificador único do objeto na fonte de dados
 * @param tipo      categoria do objeto conforme {@link ObjectType}
 * @param titulo    título de exibição do objeto na interface da fonte de dados
 * @param descricao descrição textual do propósito ou conteúdo do objeto;
 *                  pode ser {@code null} se a fonte não fornecer descrição
 */
public record ObjectDescriptor(
        String id,
        ObjectType tipo,
        String titulo,
        String descricao
) {

    /**
     * Categorias possíveis de objetos disponíveis na fonte de dados.
     *
     * <p>Utilizado por {@link ObjectDescriptor} para classificar o tipo
     * de cada objeto retornado pela fonte.</p>
     */
    public enum ObjectType {

        /** Gráfico de qualquer tipo (barras, linhas, pizza, etc.). */
        CHART,

        /** Indicador de desempenho chave (Key Performance Indicator). */
        KPI,

        /** Painel de filtro/seleção de dimensões. */
        FILTER,

        /** Tabela de dados estruturada (linhas e colunas). */
        TABLE,

        /** Tipo de objeto não reconhecido ou não mapeado. */
        UNKNOWN
    }
}
