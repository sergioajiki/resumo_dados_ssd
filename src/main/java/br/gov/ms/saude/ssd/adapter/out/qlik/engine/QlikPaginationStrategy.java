package br.gov.ms.saude.ssd.adapter.out.qlik.engine;

import br.gov.ms.saude.ssd.config.QlikProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controla a paginação na extração de dados via Qlik Engine API.
 *
 * <p>O Qlik Engine API limita o número de linhas retornadas por chamada
 * {@code GetHyperCubeData} (configurável, padrão: 5.000 linhas).
 * Para tabelas grandes como {@code DB_DIGSAUDE} (16.426 registros),
 * múltiplas chamadas são necessárias até consumir todos os dados.</p>
 *
 * <p>Algoritmo de paginação:</p>
 * <ol>
 *   <li>Solicita página começando em {@code top=0}</li>
 *   <li>Avança {@code top += pageSize} a cada iteração</li>
 *   <li>Para quando a página retornada tiver menos linhas que {@code pageSize}
 *       (indica que chegou ao fim dos dados)</li>
 * </ol>
 *
 * <p>Cada linha do Qlik retorna um array de células onde cada célula tem:</p>
 * <ul>
 *   <li>{@code qText} — valor formatado como string (sempre presente)</li>
 *   <li>{@code qNum} — valor numérico (presente apenas para campos numéricos)</li>
 *   <li>{@code qIsNull} — {@code true} se o valor for nulo na fonte</li>
 * </ul>
 *
 * @see QlikJsonRpcProtocol#buildGetHyperCubeData
 * @see QlikWebSocketClient
 */
public class QlikPaginationStrategy {

    private static final Logger log = LoggerFactory.getLogger(QlikPaginationStrategy.class);

    private final int pageSize;

    /**
     * Inicializa a estratégia de paginação com o tamanho de página configurado.
     *
     * @param props propriedades de configuração (pageSize é lido de {@code datasource.qlik.page-size})
     */
    public QlikPaginationStrategy(QlikProperties props) {
        this.pageSize = props.getPageSize();
        log.debug("Estratégia de paginação inicializada: {} linhas/página", pageSize);
    }

    /**
     * Retorna o tamanho de página configurado.
     *
     * <p>Usado pelo {@code QlikWebSocketClient} para determinar os parâmetros
     * {@code height} de cada chamada {@code GetHyperCubeData}.</p>
     *
     * @return número de linhas por página
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Verifica se há mais páginas a buscar com base no resultado da página atual.
     *
     * <p>Retorna {@code false} quando a página retornada tiver menos linhas que
     * {@code pageSize}, indicando que os dados foram esgotados.</p>
     *
     * @param rows       linhas retornadas na página atual
     * @param currentTop índice da linha inicial desta página
     * @return {@code true} se deve solicitar a próxima página; {@code false} se chegou ao fim
     */
    public boolean hasMorePages(List<List<Object>> rows, int currentTop) {
        boolean hasMore = rows.size() == pageSize;
        if (hasMore) {
            log.debug("Página completa ({} linhas a partir de {}). Há mais páginas.", rows.size(), currentTop);
        } else {
            log.debug("Página parcial ({} linhas a partir de {}). Fim dos dados.", rows.size(), currentTop);
        }
        return hasMore;
    }

    /**
     * Calcula o índice inicial da próxima página.
     *
     * @param currentTop  índice da linha inicial da página atual
     * @return índice da linha inicial da próxima página ({@code currentTop + pageSize})
     */
    public int nextTop(int currentTop) {
        return currentTop + pageSize;
    }

    /**
     * Converte as linhas brutas do JSON Qlik para listas de valores Java.
     *
     * <p>Cada célula do Qlik é convertida seguindo a prioridade:</p>
     * <ol>
     *   <li>Se {@code qIsNull=true}: valor {@code null}</li>
     *   <li>Se o campo é numérico e {@code qNum} é um número válido: retorna o {@code double}</li>
     *   <li>Caso contrário: retorna o {@code qText} (string formatada)</li>
     * </ol>
     *
     * <p>Esta lógica é compatível com o {@link br.gov.ms.saude.ssd.application.service.FieldTransformerService},
     * que sabe lidar com ambos os tipos retornados.</p>
     *
     * @param qMatrix nó JSON {@code qMatrix} da resposta {@code GetHyperCubeData}
     * @return lista de linhas, onde cada linha é uma lista de valores Java
     */
    public List<List<Object>> extractRows(JsonNode qMatrix) {
        List<List<Object>> result = new ArrayList<>();

        for (JsonNode qRow : qMatrix) {
            List<Object> row = new ArrayList<>();
            for (JsonNode cell : qRow) {
                row.add(extractCellValue(cell));
            }
            result.add(row);
        }

        return result;
    }

    /**
     * Extrai o valor de uma célula Qlik, priorizando o tipo mais adequado.
     *
     * @param cell nó JSON de uma célula do qMatrix
     * @return valor Java correspondente: {@code null}, {@code Double} ou {@code String}
     */
    private Object extractCellValue(JsonNode cell) {
        // Valor nulo na fonte
        if (cell.path("qIsNull").asBoolean(false)) {
            return null;
        }

        // Valor numérico — retorna Double para campos como ID_ATENDIMENTO, HR_AGENDAMENTO
        JsonNode qNum = cell.path("qNum");
        if (!qNum.isMissingNode() && !qNum.isNull()) {
            double num = qNum.asDouble();
            // Qlik usa Double.MAX_VALUE como sentinela para "não numérico"
            if (num < 1.0E15) {
                return num;
            }
        }

        // Valor textual — retorna qText para datas, strings e campos formatados
        String text = cell.path("qText").asText(null);
        return (text == null || text.isBlank() || text.equals("-")) ? null : text;
    }
}
