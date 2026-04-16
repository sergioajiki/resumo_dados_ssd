package br.gov.ms.saude.ssd.adapter.out.mock;

import br.gov.ms.saude.ssd.contract.DataExtractorPortContractTest;
import br.gov.ms.saude.ssd.domain.port.out.DataExtractorPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Valida que o {@link MockDataSourceAdapter} satisfaz integralmente o contrato
 * definido por {@link DataExtractorPortContractTest}.
 *
 * <p>Esta subclasse não adiciona testes próprios: sua única responsabilidade é
 * fornecer uma instância configurada do {@link MockDataSourceAdapter} para que
 * a classe pai execute todas as verificações de contrato de extração.</p>
 *
 * <p>O mesmo adaptador {@link MockDataSourceAdapter} implementa tanto
 * {@link br.gov.ms.saude.ssd.domain.port.out.DataSourcePort} quanto
 * {@link DataExtractorPort}, por isso esta classe testa a segunda interface
 * de forma independente — respeitando o princípio ISP.</p>
 *
 * <p>O método {@link MockDataSourceAdapter#init()} é invocado via
 * {@link ReflectionTestUtils#invokeMethod} porque o teste roda sem o contêiner Spring,
 * impossibilitando o processamento automático de {@code @PostConstruct}.</p>
 *
 * @see DataExtractorPortContractTest
 * @see MockDataSourceAdapter
 */
class MockAdapterExtractorContractTest extends DataExtractorPortContractTest {

    /**
     * Constrói e inicializa o {@link MockDataSourceAdapter} sem o contêiner Spring
     * para validação do contrato de {@link DataExtractorPort}.
     *
     * <p>A tabela padrão {@code DB_DIGSAUDE} está disponível no mock via
     * {@code mock-data/atendimentos.json}, garantindo que todos os testes de contrato
     * encontrarão dados para extrair.</p>
     *
     * @return instância de {@link DataExtractorPort} pronta para validação do contrato
     */
    @Override
    protected DataExtractorPort createExtractor() {
        var adapter = new MockDataSourceAdapter(new ObjectMapper());
        // Chama o @PostConstruct manualmente — necessário fora do contêiner Spring
        ReflectionTestUtils.invokeMethod(adapter, "init");
        return adapter;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Confirma explicitamente a tabela {@code DB_DIGSAUDE} como a tabela válida
     * para os testes de contrato com o adaptador mock.</p>
     *
     * @return nome da tabela principal de atendimentos disponível no mock
     */
    @Override
    protected String tabelaValida() {
        return MockDataSourceAdapter.TABELA_ATENDIMENTOS;
    }
}
