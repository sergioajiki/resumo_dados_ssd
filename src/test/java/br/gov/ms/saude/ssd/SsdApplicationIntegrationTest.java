package br.gov.ms.saude.ssd;

import br.gov.ms.saude.ssd.domain.port.out.DataExtractorPort;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração que valida a inicialização completa do contexto Spring
 * com o profile de teste (MockAdapter + H2 in-memory).
 *
 * <p>Garante que:</p>
 * <ul>
 *   <li>O contexto Spring sobe sem erros de configuração</li>
 *   <li>O {@link DataSourcePort} está disponível e operacional</li>
 *   <li>O {@link DataExtractorPort} está disponível</li>
 *   <li>O perfil de teste usa o MockAdapter (sem dependência de rede)</li>
 * </ul>
 *
 * <p>Este teste deve sempre passar independente do ambiente de execução,
 * pois o profile {@code test} garante o uso do MockAdapter.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SsdApplicationIntegrationTest {

    @Autowired
    private DataSourcePort dataSourcePort;

    @Autowired
    private DataExtractorPort dataExtractorPort;

    @Test
    @DisplayName("Contexto Spring deve subir corretamente com profile test")
    void contextLoads() {
        // Se o contexto falhar ao subir, este teste falhará antes de executar
        // Nenhuma asserção necessária — a própria inicialização é o teste
    }

    @Test
    @DisplayName("DataSourcePort deve estar disponível no contexto Spring")
    void dataSourcePort_deveEstarDisponivel() {
        assertThat(dataSourcePort).isNotNull();
    }

    @Test
    @DisplayName("DataSourcePort deve retornar saúde operacional no profile test")
    void dataSourcePort_deveEstarSaudavel() {
        var health = dataSourcePort.checkHealth();
        assertThat(health).isNotNull();
        assertThat(health.isAvailable())
            .as("MockAdapter deve sempre reportar disponibilidade")
            .isTrue();
    }

    @Test
    @DisplayName("DataSourcePort no profile test deve ser o MockAdapter")
    void dataSourcePort_profileTest_deveSerMockAdapter() {
        assertThat(dataSourcePort.getClass().getSimpleName())
            .as("No profile 'test' o adaptador deve ser o MockDataSourceAdapter")
            .containsIgnoringCase("Mock");
    }

    @Test
    @DisplayName("DataExtractorPort deve estar disponível no contexto Spring")
    void dataExtractorPort_deveEstarDisponivel() {
        assertThat(dataExtractorPort).isNotNull();
    }

    @Test
    @DisplayName("getAppMetadata deve retornar o App ID real do Qlik Sense")
    void getAppMetadata_deveRetornarAppIdCorreto() {
        var metadata = dataSourcePort.getAppMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.id())
            .as("O App ID deve corresponder ao app real do Qlik Sense")
            .isEqualTo("10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb");
    }
}
