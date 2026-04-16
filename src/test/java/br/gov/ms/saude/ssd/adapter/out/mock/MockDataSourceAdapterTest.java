package br.gov.ms.saude.ssd.adapter.out.mock;

import br.gov.ms.saude.ssd.domain.exception.DataExtractionException;
import br.gov.ms.saude.ssd.domain.model.ExtractOptions;
import br.gov.ms.saude.ssd.domain.model.ObjectDescriptor;
import br.gov.ms.saude.ssd.domain.model.QueryOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários do {@link MockDataSourceAdapter}.
 *
 * <p>Instancia o adaptador diretamente (sem Spring Boot) para testes
 * rápidos e isolados. O método {@code init()} é invocado manualmente
 * via {@link ReflectionTestUtils} para simular o comportamento do
 * {@code @PostConstruct}.</p>
 */
@DisplayName("MockDataSourceAdapter")
class MockDataSourceAdapterTest {

    private MockDataSourceAdapter adapter;

    @BeforeEach
    void setUp() {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new MockDataSourceAdapter(mapper);
        // invoca o @PostConstruct manualmente fora do contexto Spring
        ReflectionTestUtils.invokeMethod(adapter, "init");
    }

    // -------------------------------------------------------------------------
    // getAppMetadata
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAppMetadata deve retornar metadados não nulos")
    void getAppMetadata_deveRetornarMetadadosNaoNulos() {
        assertThat(adapter.getAppMetadata()).isNotNull();
    }

    @Test
    @DisplayName("getAppMetadata deve retornar o App ID real do Qlik Sense")
    void getAppMetadata_idDeveSerOAppIdDoQlik() {
        assertThat(adapter.getAppMetadata().id())
            .isEqualTo("10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb");
    }

    @Test
    @DisplayName("getAppMetadata deve retornar nome do app não vazio")
    void getAppMetadata_nomeDeveSerNaoVazio() {
        assertThat(adapter.getAppMetadata().nome()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // getDataSchema
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDataSchema deve retornar schema com duas tabelas")
    void getDataSchema_deveConterDuasTabelas() {
        assertThat(adapter.getDataSchema().tabelas()).hasSize(2);
    }

    @Test
    @DisplayName("getDataSchema deve conter a tabela principal DB_DIGSAUDE")
    void getDataSchema_deveConterTabelaDBDIGSAUDE() {
        var tabela = adapter.getDataSchema().findTabela("DB_DIGSAUDE");
        assertThat(tabela).isPresent();
        assertThat(tabela.get().totalRegistros()).isEqualTo(16426L);
    }

    @Test
    @DisplayName("getDataSchema deve conter a tabela de profissionais TEMPDB_USER")
    void getDataSchema_deveConterTabelaTEMPDB_USER() {
        assertThat(adapter.getDataSchema().findTabela("TEMPDB_USER")).isPresent();
    }

    // -------------------------------------------------------------------------
    // checkHealth
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("checkHealth deve retornar status UP")
    void checkHealth_deveRetornarStatusUp() {
        assertThat(adapter.checkHealth().status())
            .isEqualTo(br.gov.ms.saude.ssd.domain.model.HealthStatus.HealthStatusEnum.UP);
    }

    @Test
    @DisplayName("checkHealth latência não deve ser negativa")
    void checkHealth_latenciaNaoDeveSerNegativa() {
        assertThat(adapter.checkHealth().latencyMs()).isGreaterThanOrEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // listAvailableObjects
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("listAvailableObjects deve retornar 19 objetos (8 filtros + 3 KPIs + 8 gráficos)")
    void listAvailableObjects_deveRetornar19Objetos() {
        assertThat(adapter.listAvailableObjects()).hasSize(19);
    }

    @Test
    @DisplayName("listAvailableObjects deve conter filtros, KPIs e gráficos")
    void listAvailableObjects_deveConterTodosOsTipos() {
        var objetos = adapter.listAvailableObjects();
        assertThat(objetos).extracting(ObjectDescriptor::tipo)
            .contains(
                ObjectDescriptor.ObjectType.FILTER,
                ObjectDescriptor.ObjectType.KPI,
                ObjectDescriptor.ObjectType.CHART);
    }

    // -------------------------------------------------------------------------
    // getObjectData
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getObjectData com ID inexistente deve lançar DataExtractionException")
    void getObjectData_idInexistente_deveLancarDataExtractionException() {
        assertThrows(DataExtractionException.class,
            () -> adapter.getObjectData("ID_INEXISTENTE_999", QueryOptions.defaultOptions()));
    }

    @Test
    @DisplayName("getObjectData com filtro válido deve retornar dados não nulos")
    void getObjectData_filtroValido_deveRetornarDados() {
        var data = adapter.getObjectData("ADyMGB", QueryOptions.defaultOptions());
        assertThat(data).isNotNull();
        assertThat(data.headers()).isNotEmpty();
        assertThat(data.rows()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // extractTable
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("extractTable DB_DIGSAUDE deve retornar registros de atendimentos")
    void extractTable_DB_DIGSAUDE_deveRetornarRegistros() {
        var result = adapter.extractTable("DB_DIGSAUDE",
            List.of("ID_ATENDIMENTO", "NOME_MUNICIPIO"), ExtractOptions.defaults());
        assertThat(result).isNotNull();
        assertThat(result.rows()).isNotEmpty();
        assertThat(result.totalExtraidos()).isEqualTo(result.rows().size());
    }

    @Test
    @DisplayName("extractTable com tabela desconhecida deve retornar resultado vazio")
    void extractTable_tabelaDesconhecida_deveRetornarResultadoVazio() {
        var result = adapter.extractTable("TABELA_INEXISTENTE",
            List.of("CAMPO"), ExtractOptions.defaults());
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.totalExtraidos()).isZero();
    }

    // -------------------------------------------------------------------------
    // extractSince
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("extractSince com watermark 100 anos no futuro deve retornar zero registros")
    void extractSince_watermarkFutura_deveRetornarZeroRegistros() {
        var futuro = LocalDateTime.now().plusYears(100);
        var result = adapter.extractSince("DB_DIGSAUDE", List.of("ID_ATENDIMENTO"), futuro);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("extractSince com watermark em 2020 deve retornar registros")
    void extractSince_watermarkPassada_deveRetornarRegistros() {
        var passado = LocalDateTime.of(2020, 1, 1, 0, 0);
        var result = adapter.extractSince("DB_DIGSAUDE", List.of("ID_ATENDIMENTO"), passado);
        assertThat(result.rows()).isNotEmpty();
    }
}
