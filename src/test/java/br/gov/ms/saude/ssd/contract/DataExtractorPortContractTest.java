package br.gov.ms.saude.ssd.contract;

import br.gov.ms.saude.ssd.domain.model.ExtractOptions;
import br.gov.ms.saude.ssd.domain.port.out.DataExtractorPort;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato abstrato que <strong>todos</strong> os adaptadores de {@link DataExtractorPort}
 * devem satisfazer.
 *
 * <p>Para registrar um novo adaptador de extração:</p>
 * <ol>
 *   <li>Implemente {@link DataExtractorPort}</li>
 *   <li>Crie uma subclasse deste teste fornecendo o extrator via {@link #createExtractor()}</li>
 *   <li>Execute: {@code ./mvnw test -P contract}</li>
 * </ol>
 * <p>Se todos os testes passarem, o adaptador cumpre o contrato de extração e pode ser
 * utilizado pelo {@code ExecutarSyncUseCase} sem modificações no domínio.</p>
 *
 * <p><strong>Princípio ISP (Interface Segregation — SOLID):</strong> este contrato é
 * separado de {@link DataSourcePortContractTest} porque nem toda fonte de dados suporta
 * extração em lote — e nem todo consumidor de dados precisa dela.</p>
 *
 * @see DataExtractorPort
 * @see br.gov.ms.saude.ssd.adapter.out.mock.MockAdapterExtractorContractTest
 */
@Tag("contract")
public abstract class DataExtractorPortContractTest {

    /** Instância do extrator sob teste, criada antes de cada método. */
    private DataExtractorPort extractor;

    /**
     * Inicializa o extrator antes de cada teste via {@link #createExtractor()}.
     * Garante isolamento entre execuções de teste.
     */
    @BeforeEach
    void setUp() {
        extractor = createExtractor();
    }

    /**
     * Fornece a instância do extrator a ser testado.
     *
     * <p>Implementar nas subclasses com a construção concreta do adaptador,
     * incluindo qualquer inicialização necessária.</p>
     *
     * @return instância de {@link DataExtractorPort} pronta para uso
     */
    protected abstract DataExtractorPort createExtractor();

    /**
     * Fornece o nome de uma tabela válida disponível no adaptador sob teste.
     *
     * <p>Subclasses podem sobrescrever este método quando a tabela padrão
     * {@code DB_DIGSAUDE} não estiver disponível na fonte de dados testada.</p>
     *
     * @return nome da tabela válida para os testes de extração
     */
    protected String tabelaValida() {
        return "DB_DIGSAUDE";
    }

    // =========================================================================
    // Contrato: extractTable
    // =========================================================================

    /**
     * Verifica que {@code extractTable()} nunca retorna {@code null}.
     * O pipeline ETL não verifica nulidade do resultado — retornar {@code null}
     * causaria {@link NullPointerException} ao persistir os dados.
     */
    @Test
    @DisplayName("extractTable deve retornar resultado não nulo")
    void extractTable_naoDeveRetornarNulo() {
        var result = extractor.extractTable(tabelaValida(), List.of("ID_ATENDIMENTO"), ExtractOptions.defaults());
        assertThat(result).isNotNull();
    }

    /**
     * Verifica que a lista de headers retornada por {@code extractTable()} não é nula.
     * Os headers são usados para mapear colunas nas entidades de persistência.
     */
    @Test
    @DisplayName("extractTable headers não devem ser nulos")
    void extractTable_headersNaoDevemSerNulos() {
        var result = extractor.extractTable(tabelaValida(), List.of("ID_ATENDIMENTO"), ExtractOptions.defaults());
        assertThat(result.headers()).isNotNull();
    }

    /**
     * Verifica que a lista de linhas retornada por {@code extractTable()} não é nula.
     * Retornar {@code null} em {@code rows()} violaria o contrato e quebraria
     * o loop de persistência do {@code ExecutarSyncUseCase}.
     */
    @Test
    @DisplayName("extractTable rows não devem ser nulas")
    void extractTable_rowsNaoDevemSerNulas() {
        var result = extractor.extractTable(tabelaValida(), List.of("ID_ATENDIMENTO"), ExtractOptions.defaults());
        assertThat(result.rows()).isNotNull();
    }

    /**
     * Verifica que {@code totalExtraidos()} é igual ao tamanho de {@code rows()}.
     *
     * <p>Esta invariante garante a integridade dos metadados de extração: o campo
     * {@code totalExtraidos} é usado pelo {@code SyncLog} para registrar quantos
     * registros foram efetivamente processados. Se diferir do tamanho real de
     * {@code rows()}, o relatório de sincronização estará incorreto.</p>
     */
    @Test
    @DisplayName("totalExtraidos deve corresponder ao tamanho de rows")
    void extractTable_totalExtraidosDeveCorresponderARows() {
        var result = extractor.extractTable(tabelaValida(), List.of("ID_ATENDIMENTO"), ExtractOptions.defaults());
        assertThat(result.totalExtraidos())
                .as("totalExtraidos deve ser igual ao número de linhas em rows()")
                .isEqualTo(result.rows().size());
    }

    /**
     * Verifica que a duração retornada por {@code extractTable()} não é nula.
     * A duração é registrada no {@code SyncLog} para monitoramento de desempenho.
     */
    @Test
    @DisplayName("duração não deve ser nula")
    void extractTable_duracaoNaoDeveSerNula() {
        var result = extractor.extractTable(tabelaValida(), List.of("ID_ATENDIMENTO"), ExtractOptions.defaults());
        assertThat(result.duracao()).isNotNull();
    }

    /**
     * Verifica que o watermark retornado por {@code extractTable()} não é nulo.
     * O watermark é persisted no {@code SyncLog} e usado como ponto de corte
     * para a próxima extração incremental.
     */
    @Test
    @DisplayName("watermark do resultado não deve ser nulo")
    void extractTable_watermarkNaoDeveSerNulo() {
        var result = extractor.extractTable(tabelaValida(), List.of("ID_ATENDIMENTO"), ExtractOptions.defaults());
        assertThat(result.watermark()).isNotNull();
    }

    // =========================================================================
    // Contrato: extractSince
    // =========================================================================

    /**
     * Verifica que {@code extractSince()} com watermark 100 anos no futuro
     * retorna resultado vazio ({@code isEmpty() == true}).
     *
     * <p>Esta é a garantia fundamental de extração incremental: se não há
     * registros novos desde a data informada, o resultado deve ser vazio —
     * não nulo e não cheio de dados antigos.</p>
     */
    @Test
    @DisplayName("extractSince com watermark no futuro deve retornar resultado vazio")
    void extractSince_watermarkFutura_deveRetornarVazio() {
        var watermark = LocalDateTime.now().plusYears(100);
        var result = extractor.extractSince(tabelaValida(), List.of("ID_ATENDIMENTO"), watermark);
        assertThat(result.isEmpty())
                .as("Extração desde uma data futura deve retornar resultado vazio")
                .isTrue();
    }

    /**
     * Verifica que {@code extractSince()} com watermark no passado retorna registros.
     *
     * <p>Garante que a extração incremental retorna dados quando a marca d'água
     * é anterior ao registro mais antigo da tabela. Essencial para a primeira
     * carga (full load) do ETL.</p>
     */
    @Test
    @DisplayName("extractSince com watermark no passado deve retornar registros")
    void extractSince_watermarkPassada_deveRetornarRegistros() {
        var watermark = LocalDateTime.of(2020, 1, 1, 0, 0);
        var result = extractor.extractSince(tabelaValida(), List.of("ID_ATENDIMENTO"), watermark);
        assertThat(result.rows())
                .as("Extração desde 2020-01-01 deve retornar ao menos um registro")
                .isNotEmpty();
    }
}
