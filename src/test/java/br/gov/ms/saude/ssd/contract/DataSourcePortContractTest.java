package br.gov.ms.saude.ssd.contract;

import br.gov.ms.saude.ssd.domain.exception.DataExtractionException;
import br.gov.ms.saude.ssd.domain.model.QueryOptions;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato abstrato que <strong>todos</strong> os adaptadores de {@link DataSourcePort}
 * devem satisfazer.
 *
 * <p>Para registrar um novo adaptador que substitua o Qlik Sense:</p>
 * <ol>
 *   <li>Implemente {@link DataSourcePort}</li>
 *   <li>Crie uma subclasse deste teste fornecendo o adaptador via {@link #createAdapter()}</li>
 *   <li>Execute: {@code ./mvnw test -P contract}</li>
 * </ol>
 * <p>Se todos os testes passarem, o adaptador cumpre o contrato e pode ser
 * ligado via {@code datasource.adapter} no {@code application.yml} sem
 * quebrar a aplicação.</p>
 *
 * <p><strong>Princípio LSP (Liskov Substitution — SOLID):</strong> qualquer adaptador
 * deve poder substituir outro sem alterar o comportamento esperado pelo domínio.</p>
 *
 * @see DataSourcePort
 * @see br.gov.ms.saude.ssd.adapter.out.mock.MockAdapterDataSourceContractTest
 */
@Tag("contract")
public abstract class DataSourcePortContractTest {

    /** Instância do adaptador sob teste, criada antes de cada método. */
    private DataSourcePort adapter;

    /**
     * Inicializa o adaptador antes de cada teste via {@link #createAdapter()}.
     * Garante isolamento entre execuções de teste.
     */
    @BeforeEach
    void setUp() {
        adapter = createAdapter();
    }

    /**
     * Fornece a instância do adaptador a ser testado.
     *
     * <p>Implementar nas subclasses com a construção concreta do adaptador,
     * incluindo qualquer inicialização necessária (ex: chamada ao {@code @PostConstruct}).</p>
     *
     * @return instância de {@link DataSourcePort} pronta para uso
     */
    protected abstract DataSourcePort createAdapter();

    // =========================================================================
    // Contrato: getAppMetadata
    // =========================================================================

    /**
     * Verifica que {@code getAppMetadata()} nunca retorna {@code null}.
     * Um adaptador que retorna {@code null} violaria o contrato e causaria
     * {@link NullPointerException} nos casos de uso.
     */
    @Test
    @DisplayName("getAppMetadata deve retornar objeto não nulo")
    void getAppMetadata_naoDeveRetornarNulo() {
        assertThat(adapter.getAppMetadata()).isNotNull();
    }

    /**
     * Verifica que o ID retornado em {@code getAppMetadata()} não é nulo nem vazio.
     * O ID é utilizado como chave de identificação do app na fonte de dados.
     */
    @Test
    @DisplayName("getAppMetadata deve retornar ID não vazio")
    void getAppMetadata_idDeveSerNaoVazio() {
        assertThat(adapter.getAppMetadata().id())
                .isNotNull()
                .isNotBlank();
    }

    /**
     * Verifica que o nome retornado em {@code getAppMetadata()} não é nulo nem vazio.
     * O nome é exibido na interface e nos relatórios da aplicação.
     */
    @Test
    @DisplayName("getAppMetadata deve retornar nome não vazio")
    void getAppMetadata_nomeDeveSerNaoVazio() {
        assertThat(adapter.getAppMetadata().nome())
                .isNotNull()
                .isNotBlank();
    }

    // =========================================================================
    // Contrato: getDataSchema
    // =========================================================================

    /**
     * Verifica que {@code getDataSchema()} nunca retorna {@code null}.
     * O domínio espera sempre um objeto válido para inspecionar o schema.
     */
    @Test
    @DisplayName("getDataSchema deve retornar schema não nulo")
    void getDataSchema_naoDeveRetornarNulo() {
        assertThat(adapter.getDataSchema()).isNotNull();
    }

    /**
     * Verifica que o schema retornado contém pelo menos uma tabela.
     * Uma fonte de dados sem tabelas não é utilizável pelo pipeline ETL.
     */
    @Test
    @DisplayName("getDataSchema deve conter pelo menos uma tabela")
    void getDataSchema_deveConterPeloMenosUmaTabela() {
        assertThat(adapter.getDataSchema().tabelas())
                .isNotNull()
                .isNotEmpty();
    }

    /**
     * Verifica que todas as tabelas retornadas no schema possuem nome não vazio.
     * Nomes em branco impossibilitariam a identificação das tabelas no pipeline ETL.
     */
    @Test
    @DisplayName("todas as tabelas do schema devem ter nome não vazio")
    void getDataSchema_todasTabelasDevemTerNome() {
        adapter.getDataSchema().tabelas()
                .forEach(tabela ->
                    assertThat(tabela.nome())
                            .as("Nome da tabela não deve ser nulo ou vazio")
                            .isNotNull()
                            .isNotBlank()
                );
    }

    // =========================================================================
    // Contrato: checkHealth
    // =========================================================================

    /**
     * Verifica que {@code checkHealth()} nunca retorna {@code null}.
     * O endpoint {@code /api/v1/health} depende desta garantia para serializar a resposta.
     */
    @Test
    @DisplayName("checkHealth deve retornar status não nulo")
    void checkHealth_naoDeveRetornarNulo() {
        assertThat(adapter.checkHealth()).isNotNull();
    }

    /**
     * Verifica que o campo {@code status} dentro do {@link br.gov.ms.saude.ssd.domain.model.HealthStatus}
     * não é nulo. O status é obrigatório para determinar se a extração pode prosseguir.
     */
    @Test
    @DisplayName("checkHealth deve retornar status operacional não nulo")
    void checkHealth_statusNaoDeveSerNulo() {
        assertThat(adapter.checkHealth().status()).isNotNull();
    }

    /**
     * Verifica que a latência retornada em {@code checkHealth()} não é menor do que {@code -1}.
     * Por convenção, {@code -1} representa latência indisponível (status DOWN);
     * qualquer adaptador funcional deve retornar {@code -1} ou um valor não negativo.
     */
    @Test
    @DisplayName("checkHealth latência não deve ser negativa além de -1")
    void checkHealth_latenciaNaoDeveSerNegativa() {
        // Status DOWN retorna -1 (latência não mensurável); outros devem ser >= 0
        assertThat(adapter.checkHealth().latencyMs())
                .isGreaterThanOrEqualTo(-1L);
    }

    // =========================================================================
    // Contrato: listAvailableObjects
    // =========================================================================

    /**
     * Verifica que {@code listAvailableObjects()} nunca retorna {@code null}.
     * O domínio itera sobre a lista sem verificação de nulidade — retornar {@code null}
     * causaria {@link NullPointerException}.
     */
    @Test
    @DisplayName("listAvailableObjects deve retornar lista não nula")
    void listAvailableObjects_naoDeveRetornarNulo() {
        assertThat(adapter.listAvailableObjects()).isNotNull();
    }

    // =========================================================================
    // Contrato: getObjectData
    // =========================================================================

    /**
     * Verifica que {@code getObjectData()} lança {@link DataExtractionException}
     * ao receber um ID inexistente, conforme especificado no contrato de
     * {@link DataSourcePort#getObjectData(String, QueryOptions)}.
     *
     * <p>Sem essa garantia, o domínio receberia {@code null} ou um objeto vazio
     * sem saber que a operação falhou.</p>
     */
    @Test
    @DisplayName("getObjectData com ID inexistente deve lançar DataExtractionException")
    void getObjectData_idInexistente_deveLancarExcecaoDeDominio() {
        assertThrows(DataExtractionException.class,
                () -> adapter.getObjectData("ID_QUE_NAO_EXISTE_999", QueryOptions.defaultOptions()));
    }

    /**
     * Verifica que {@code getObjectData()} retorna headers não nulos para um ID válido.
     * Pula o teste se não houver objetos disponíveis (ambiente sem dados).
     *
     * <p>Usa {@code assumeThat} para tornar o teste condicional: se a lista de objetos
     * estiver vazia, o teste é marcado como "assumção não satisfeita" (aborted),
     * não como falha.</p>
     */
    @Test
    @DisplayName("getObjectData com ID válido deve retornar headers não nulos")
    void getObjectData_idValido_headersNaoDevemSerNulos() {
        var objetos = adapter.listAvailableObjects();
        // Pula o teste se nenhum objeto estiver disponível no ambiente de teste
        assumeThat(objetos).isNotEmpty();
        var data = adapter.getObjectData(objetos.get(0).id(), QueryOptions.defaultOptions());
        assertThat(data.headers()).isNotNull();
    }
}
