package br.gov.ms.saude.ssd.adapter.out.mock;

import br.gov.ms.saude.ssd.contract.DataSourcePortContractTest;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Valida que o {@link MockDataSourceAdapter} satisfaz integralmente o contrato
 * definido por {@link DataSourcePortContractTest}.
 *
 * <p>Esta subclasse não adiciona testes próprios: sua única responsabilidade é
 * fornecer uma instância configurada do {@link MockDataSourceAdapter} para que
 * a classe pai execute todas as verificações de contrato.</p>
 *
 * <p>O método {@link MockDataSourceAdapter#init()} é invocado via
 * {@link ReflectionTestUtils#invokeMethod} porque o teste roda <em>sem</em>
 * o contêiner Spring — portanto a anotação {@code @PostConstruct} não é
 * processada automaticamente. Isso também valida que a inicialização pode
 * ser feita programaticamente, importante para testes de integração parcial.</p>
 *
 * <p><strong>Para adicionar um novo adaptador ao projeto:</strong> copie esta classe,
 * substitua {@code MockDataSourceAdapter} pelo novo adaptador e ajuste a construção
 * em {@link #createAdapter()}.</p>
 *
 * @see DataSourcePortContractTest
 * @see MockDataSourceAdapter
 */
class MockAdapterDataSourceContractTest extends DataSourcePortContractTest {

    /**
     * Constrói e inicializa o {@link MockDataSourceAdapter} sem o contêiner Spring.
     *
     * <p>O {@link ObjectMapper} padrão (sem customizações) é suficiente para os arquivos
     * JSON do mock, uma vez que os dados são simples objetos sem tipos personalizados.</p>
     *
     * <p>A chamada a {@code ReflectionTestUtils.invokeMethod(adapter, "init")} aciona
     * o método anotado com {@code @PostConstruct}, carregando os dados dos arquivos
     * {@code mock-data/atendimentos.json} e {@code mock-data/profissionais.json}.</p>
     *
     * @return instância de {@link DataSourcePort} pronta para validação do contrato
     */
    @Override
    protected DataSourcePort createAdapter() {
        var adapter = new MockDataSourceAdapter(new ObjectMapper());
        // Chama o @PostConstruct manualmente — necessário fora do contêiner Spring
        ReflectionTestUtils.invokeMethod(adapter, "init");
        return adapter;
    }
}
