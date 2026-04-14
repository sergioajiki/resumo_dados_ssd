package br.gov.ms.saude.ssd.adapter.out.qlik.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Constrói a definição JSON de um HyperCube para a Qlik Engine API.
 *
 * <p>O HyperCube é o objeto de consulta central do Qlik Sense. Para extrair dados
 * tabulares, criamos um HyperCube com todas as colunas desejadas como
 * {@code qDimensions} (dimensões), sem medidas ({@code qMeasures} vazio).
 * Isso retorna os valores brutos de cada campo sem agregação.</p>
 *
 * <p>Estrutura da definição gerada:</p>
 * <pre>
 * {
 *   "qInfo": { "qType": "ExtractCube" },
 *   "qHyperCubeDef": {
 *     "qDimensions": [
 *       { "qDef": { "qFieldDefs": ["ID_ATENDIMENTO"], "qSortCriterias": [...] } },
 *       { "qDef": { "qFieldDefs": ["CNS_PACIENTE"],   "qSortCriterias": [...] } },
 *       ...
 *     ],
 *     "qMeasures": [],
 *     "qInitialDataFetch": [{ "qTop": 0, "qLeft": 0, "qHeight": 0, "qWidth": N }],
 *     "qSuppressZero": false,
 *     "qSuppressMissing": false
 *   }
 * }
 * </pre>
 *
 * <p>{@code qSuppressMissing: false} é crítico — garante que linhas com campos nulos
 * sejam incluídas no resultado (comportamento de extração completa).</p>
 *
 * @see QlikJsonRpcProtocol#buildCreateSessionObject
 * @see QlikPaginationStrategy
 */
public class QlikHyperCubeBuilder {

    private final ObjectMapper objectMapper;

    /**
     * Cria um builder com o ObjectMapper fornecido.
     *
     * @param objectMapper serializador JSON compartilhado
     */
    public QlikHyperCubeBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Constrói a definição JSON do HyperCube para os campos especificados.
     *
     * <p>Cada campo da lista {@code fields} se torna uma dimensão do HyperCube.
     * A ordem dos campos na lista determina a ordem das colunas no resultado,
     * e deve corresponder à ordem dos headers usados pelo {@code LoaderService}.</p>
     *
     * @param fields lista de nomes de campos Qlik a incluir como dimensões
     *               (ex: ["ID_ATENDIMENTO", "CNS_PACIENTE", "DT_NEW"])
     * @return definição JSON do objeto de sessão para uso em {@code CreateSessionObject}
     */
    public ObjectNode build(List<String> fields) {
        ObjectNode root = objectMapper.createObjectNode();

        // Informações de identificação do objeto de sessão
        ObjectNode qInfo = objectMapper.createObjectNode();
        qInfo.put("qType", "ExtractCube");
        root.set("qInfo", qInfo);

        // Definição do HyperCube
        ObjectNode hyperCubeDef = objectMapper.createObjectNode();

        // Dimensões — um por campo a extrair
        ArrayNode dimensions = objectMapper.createArrayNode();
        for (String field : fields) {
            dimensions.add(buildDimension(field));
        }
        hyperCubeDef.set("qDimensions", dimensions);

        // Sem medidas — extração tabular pura
        hyperCubeDef.set("qMeasures", objectMapper.createArrayNode());

        // Fetch inicial zerado — os dados serão buscados via GetHyperCubeData paginado
        ArrayNode initialFetch = objectMapper.createArrayNode();
        ObjectNode fetchRange = objectMapper.createObjectNode();
        fetchRange.put("qTop", 0);
        fetchRange.put("qLeft", 0);
        fetchRange.put("qHeight", 0);
        fetchRange.put("qWidth", fields.size());
        initialFetch.add(fetchRange);
        hyperCubeDef.set("qInitialDataFetch", initialFetch);

        // Não suprimir zeros nem valores ausentes — extração completa
        hyperCubeDef.put("qSuppressZero", false);
        hyperCubeDef.put("qSuppressMissing", false);

        root.set("qHyperCubeDef", hyperCubeDef);
        return root;
    }

    /**
     * Constrói a definição JSON de uma dimensão para um campo Qlik.
     *
     * <p>O critério de ordenação {@code qSortByLoadOrder} (valor 1) preserva
     * a ordem de carregamento dos dados no Qlik, que é a mais próxima da
     * ordem de inserção na fonte original.</p>
     *
     * @param fieldName nome do campo Qlik (ex: "ID_ATENDIMENTO")
     * @return nó JSON da definição da dimensão
     */
    private ObjectNode buildDimension(String fieldName) {
        ObjectNode dimension = objectMapper.createObjectNode();
        ObjectNode qDef = objectMapper.createObjectNode();

        // Campo a retornar
        ArrayNode fieldDefs = objectMapper.createArrayNode();
        fieldDefs.add(fieldName);
        qDef.set("qFieldDefs", fieldDefs);

        // Ordenação por ordem de carga (preserva ordem original da fonte)
        ArrayNode sortCriterias = objectMapper.createArrayNode();
        ObjectNode sort = objectMapper.createObjectNode();
        sort.put("qSortByLoadOrder", 1);
        sortCriterias.add(sort);
        qDef.set("qSortCriterias", sortCriterias);

        dimension.set("qDef", qDef);
        return dimension;
    }
}
