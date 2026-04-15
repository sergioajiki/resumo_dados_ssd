package br.gov.ms.saude.ssd.adapter.out.mock;

import br.gov.ms.saude.ssd.domain.exception.DataExtractionException;
import br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException;
import br.gov.ms.saude.ssd.domain.model.*;
import br.gov.ms.saude.ssd.domain.port.out.DataExtractorPort;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adaptador simulado que implementa {@link DataSourcePort} e {@link DataExtractorPort}
 * com dados estáticos carregados dos arquivos JSON em {@code src/test/resources/mock-data/}.
 *
 * <p>Projetado para ser utilizado em:</p>
 * <ul>
 *   <li>Testes unitários e de integração (profile {@code test})</li>
 *   <li>Desenvolvimento local sem acesso ao Qlik Sense</li>
 *   <li>Validação do contrato de porta — estende
 *       {@code DataSourcePortContractTest} e {@code DataExtractorPortContractTest}</li>
 * </ul>
 *
 * <p>A inicialização dos dados ocorre em {@link #init()}, chamado automaticamente
 * pelo Spring via {@link PostConstruct}. Em testes sem Spring, chame
 * {@code init()} manualmente ou via
 * {@code ReflectionTestUtils.invokeMethod(adapter, "init")}.</p>
 *
 * <p>Ativado quando a propriedade {@code datasource.adapter=mock} estiver configurada.
 * Isso acontece automaticamente no profile {@code test} e no profile {@code dev}
 * pela ausência de configuração Qlik.</p>
 *
 * @see DataSourcePort
 * @see DataExtractorPort
 */
@Component("mockAdapter")
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "mock", matchIfMissing = true)
public class MockDataSourceAdapter implements DataSourcePort, DataExtractorPort {

    /** Identificador fixo do app simulado — utilizado como referência nos testes. */
    public static final String MOCK_APP_ID = "mock-app-id-001";

    /** Nome do app simulado, correspondente ao projeto real. */
    public static final String MOCK_APP_NOME = "Núcleo de Telessaúde MS (Mock)";

    /** Nome da tabela principal de atendimentos disponível no mock. */
    public static final String TABELA_ATENDIMENTOS = "DB_DIGSAUDE";

    /** Nome da tabela secundária de profissionais disponível no mock — igual ao nome real no Qlik. */
    public static final String TABELA_PROFISSIONAIS = "TEMPDB_USER";

    private final ObjectMapper objectMapper;

    /** Dados carregados de atendimentos.json indexados por nome de tabela. */
    private final Map<String, List<Map<String, Object>>> dadosPorTabela = new LinkedHashMap<>();

    /** Lista de objetos disponíveis (descritores de objetos simulados). */
    private List<ObjectDescriptor> objetosDisponiveis = new ArrayList<>();

    /**
     * Constrói o adaptador mock injetando o {@link ObjectMapper} do Spring.
     *
     * @param objectMapper serializador/deserializador Jackson — injetado pelo contêiner
     */
    public MockDataSourceAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Inicializa o adaptador carregando os dados dos arquivos JSON do classpath.
     *
     * <p>Chamado automaticamente pelo Spring após a construção do bean ({@link PostConstruct}).
     * Em contextos sem Spring, deve ser invocado explicitamente antes do uso — por exemplo,
     * via {@code ReflectionTestUtils.invokeMethod(adapter, "init")} nos testes de contrato.</p>
     *
     * <p>Os arquivos esperados são:</p>
     * <ul>
     *   <li>{@code mock-data/atendimentos.json} — tabela {@code DB_DIGSAUDE}</li>
     *   <li>{@code mock-data/profissionais.json} — tabela {@code TEMPDB_USER}</li>
     * </ul>
     *
     * @throws DataSourceUnavailableException se os arquivos JSON não puderem ser lidos
     */
    @PostConstruct
    public void init() {
        dadosPorTabela.put(TABELA_ATENDIMENTOS, carregarJson("mock-data/atendimentos.json"));
        dadosPorTabela.put(TABELA_PROFISSIONAIS, carregarJson("mock-data/profissionais.json"));
        objetosDisponiveis = construirObjetosDisponiveis();
    }

    // =========================================================================
    // DataSourcePort
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Retorna metadados fixos do app simulado, com datas de criação e
     * publicação em 2024 e último reload no momento da inicialização.</p>
     */
    @Override
    public AppMetadata getAppMetadata() {
        return new AppMetadata(
                MOCK_APP_ID,
                MOCK_APP_NOME,
                "Dados simulados para desenvolvimento e testes automatizados.",
                "SES-MS — Equipe de Tecnologia (Mock)",
                LocalDateTime.of(2024, 1, 15, 8, 0),
                LocalDateTime.of(2024, 2, 1, 10, 0),
                LocalDateTime.now().minusHours(1),
                true
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Infere o schema dinamicamente a partir das chaves dos objetos JSON carregados.
     * Cada chave de primeiro nível se torna um {@link FieldSchema} com tipo inferido.</p>
     */
    @Override
    public DataSchema getDataSchema() {
        List<TableSchema> tabelas = dadosPorTabela.entrySet().stream()
                .map(entry -> {
                    List<Map<String, Object>> registros = entry.getValue();
                    List<FieldSchema> campos = inferirCampos(registros);
                    return new TableSchema(entry.getKey(), registros.size(), campos);
                })
                .toList();
        return new DataSchema(tabelas);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Procura o {@code objectId} na lista de objetos disponíveis.
     * Se não encontrado, lança {@link DataExtractionException} para cumprir o contrato.</p>
     *
     * @throws DataExtractionException se o {@code objectId} não existir no mock
     */
    @Override
    public ObjectData getObjectData(String objectId, QueryOptions options) {
        ObjectDescriptor descritor = objetosDisponiveis.stream()
                .filter(o -> o.id().equals(objectId))
                .findFirst()
                .orElseThrow(() -> new DataExtractionException(
                        objectId, "Objeto não encontrado no adaptador mock."));

        // Determina qual tabela representa este objeto pelo prefixo do ID
        String tabelaNome = objectId.contains("prof") ? TABELA_PROFISSIONAIS : TABELA_ATENDIMENTOS;
        List<Map<String, Object>> registros = dadosPorTabela.getOrDefault(tabelaNome, List.of());

        List<String> headers = registros.isEmpty() ? List.of() : new ArrayList<>(registros.get(0).keySet());
        int inicio = options.page() * options.pageSize();
        int fim = Math.min(inicio + options.pageSize(), registros.size());
        List<List<Object>> rows = registros.subList(Math.min(inicio, registros.size()), fim)
                .stream()
                .map(r -> headers.stream().map(r::get).collect(Collectors.toList()))
                .toList();

        return new ObjectData(objectId, headers, rows);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retorna os objetos construídos na inicialização a partir das tabelas carregadas.</p>
     */
    @Override
    public List<ObjectDescriptor> listAvailableObjects() {
        return Collections.unmodifiableList(objetosDisponiveis);
    }

    /**
     * {@inheritDoc}
     *
     * <p>O mock sempre retorna {@link HealthStatus#up(long)} com latência simulada de 1 ms,
     * pois não realiza I/O real para verificar disponibilidade.</p>
     */
    @Override
    public HealthStatus checkHealth() {
        return HealthStatus.up(1L);
    }

    // =========================================================================
    // DataExtractorPort
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Retorna todos os registros da tabela especificada. Se a tabela não existir,
     * lança {@link DataExtractionException}. Se {@code fields} for vazio,
     * todos os campos são incluídos.</p>
     */
    @Override
    public ExtractResult extractTable(String tableName, List<String> fields, ExtractOptions options) {
        List<Map<String, Object>> registros = obterRegistrosPorTabela(tableName);
        List<String> headers = resolverHeaders(registros, fields);
        List<List<Object>> rows = projetarRegistros(registros, headers);

        return new ExtractResult(
                tableName,
                rows,
                headers,
                rows.size(),
                LocalDateTime.now(),
                Duration.ofMillis(5)
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Filtra os registros pelo campo {@code DT_NEW}: apenas aqueles com valor
     * posterior à {@code watermark} são retornados. Se o campo {@code DT_NEW}
     * não existir no registro, ele é excluído por segurança.</p>
     */
    @Override
    public ExtractResult extractSince(String tableName, List<String> fields, LocalDateTime watermark) {
        List<Map<String, Object>> todos = obterRegistrosPorTabela(tableName);

        List<Map<String, Object>> filtrados = todos.stream()
                .filter(r -> {
                    Object dtNew = r.get("DT_NEW");
                    if (dtNew == null) return false;
                    LocalDateTime dtRegistro = LocalDateTime.parse(dtNew.toString()
                            .replace(" ", "T").substring(0, 19));
                    return dtRegistro.isAfter(watermark);
                })
                .toList();

        List<String> headers = resolverHeaders(filtrados.isEmpty() ? todos : filtrados, fields);
        List<List<Object>> rows = projetarRegistros(filtrados, headers);

        LocalDateTime novaWatermark = filtrados.isEmpty()
                ? watermark
                : LocalDateTime.now();

        return new ExtractResult(
                tableName,
                rows,
                headers,
                rows.size(),
                novaWatermark,
                Duration.ofMillis(3)
        );
    }

    // =========================================================================
    // Métodos auxiliares privados
    // =========================================================================

    /**
     * Carrega e desserializa um arquivo JSON do classpath como lista de mapas.
     *
     * @param caminho caminho relativo ao classpath (ex: "mock-data/atendimentos.json")
     * @return lista de registros; vazia se o arquivo não for encontrado
     * @throws DataSourceUnavailableException se o arquivo existir mas não puder ser lido
     */
    private List<Map<String, Object>> carregarJson(String caminho) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (is == null) {
                // Arquivo ausente é tolerado — retorna lista vazia
                return new ArrayList<>();
            }
            return objectMapper.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            throw new DataSourceUnavailableException(
                    "Falha ao carregar dados mock de '" + caminho + "': " + e.getMessage(), e);
        }
    }

    /**
     * Infere os campos de uma tabela a partir das chaves dos primeiros registros JSON.
     *
     * @param registros registros JSON da tabela
     * @return lista de {@link FieldSchema} com tipo inferido; vazia se não há registros
     */
    private List<FieldSchema> inferirCampos(List<Map<String, Object>> registros) {
        if (registros.isEmpty()) return List.of();
        return registros.get(0).entrySet().stream()
                .map(e -> {
                    long distinctCount = registros.stream()
                            .filter(r -> r.get(e.getKey()) != null)
                            .map(r -> r.get(e.getKey()))
                            .distinct()
                            .count();
                    boolean isPk = distinctCount == registros.size() && distinctCount > 0;
                    return new FieldSchema(
                            e.getKey(),
                            inferirTipo(e.getValue()),
                            (int) distinctCount,
                            isPk,
                            List.of());
                })
                .toList();
    }

    /**
     * Infere o tipo de um valor Java como string descritiva simples.
     *
     * @param valor valor do campo, pode ser {@code null}
     * @return nome do tipo inferido: "INTEGER", "DECIMAL", "DATETIME", "TEXT" ou "UNKNOWN"
     */
    private String inferirTipo(Object valor) {
        if (valor == null) return "UNKNOWN";
        if (valor instanceof Integer || valor instanceof Long) return "INTEGER";
        if (valor instanceof Double || valor instanceof Float) return "DECIMAL";
        String s = valor.toString();
        if (s.matches("\\d{4}-\\d{2}-\\d{2}.*")) return "DATETIME";
        return "TEXT";
    }

    /**
     * Constrói a lista de descritores de objetos a partir das tabelas carregadas.
     * Cada tabela gera dois objetos simulados: um {@code TABLE} e um {@code CHART}.
     *
     * @return lista de {@link ObjectDescriptor} representando os objetos disponíveis no mock
     */
    private List<ObjectDescriptor> construirObjetosDisponiveis() {
        List<ObjectDescriptor> lista = new ArrayList<>();
        dadosPorTabela.forEach((nome, registros) -> {
            String prefixo = nome.toLowerCase().replace("_", "-");
            lista.add(new ObjectDescriptor(
                    prefixo + "-table-obj",
                    ObjectDescriptor.ObjectType.TABLE,
                    "Tabela: " + nome,
                    "Tabela simulada de " + nome + " com " + registros.size() + " registros."
            ));
            lista.add(new ObjectDescriptor(
                    prefixo + "-chart-obj",
                    ObjectDescriptor.ObjectType.CHART,
                    "Gráfico: " + nome,
                    "Gráfico simulado baseado nos dados de " + nome + "."
            ));
        });
        return lista;
    }

    /**
     * Obtém os registros de uma tabela pelo nome, lançando exceção se não encontrada.
     *
     * @param tableName nome da tabela
     * @return lista de registros; nunca {@code null}
     * @throws DataExtractionException se a tabela não existir no mock
     */
    private List<Map<String, Object>> obterRegistrosPorTabela(String tableName) {
        if (!dadosPorTabela.containsKey(tableName)) {
            throw new DataExtractionException(tableName,
                    "Tabela não encontrada no adaptador mock. Tabelas disponíveis: "
                    + dadosPorTabela.keySet());
        }
        return dadosPorTabela.get(tableName);
    }

    /**
     * Resolve os cabeçalhos a serem incluídos na projeção dos registros.
     * Se {@code fields} estiver vazio, retorna todos os campos do primeiro registro.
     *
     * @param registros registros de referência para inferir cabeçalhos
     * @param fields    campos solicitados; vazio significa "todos"
     * @return lista ordenada de nomes de campos
     */
    private List<String> resolverHeaders(List<Map<String, Object>> registros, List<String> fields) {
        if (registros.isEmpty()) return fields.isEmpty() ? List.of() : new ArrayList<>(fields);
        List<String> todosOsCampos = new ArrayList<>(registros.get(0).keySet());
        return (fields == null || fields.isEmpty()) ? todosOsCampos : new ArrayList<>(fields);
    }

    /**
     * Projeta os registros para uma lista de listas de valores, na ordem dos cabeçalhos.
     *
     * @param registros registros a serem projetados
     * @param headers   colunas a incluir, na ordem desejada
     * @return lista de linhas, onde cada linha segue a ordem de {@code headers}
     */
    private List<List<Object>> projetarRegistros(List<Map<String, Object>> registros, List<String> headers) {
        return registros.stream()
                .map(r -> headers.stream()
                        .map(h -> (Object) r.get(h))
                        .collect(Collectors.toList()))
                .toList();
    }
}
