package br.gov.ms.saude.ssd.application.service;

import br.gov.ms.saude.ssd.application.usecase.ConsultarSchemaUseCase;
import br.gov.ms.saude.ssd.domain.model.AppMetadata;
import br.gov.ms.saude.ssd.domain.model.DataSchema;
import br.gov.ms.saude.ssd.domain.model.HealthStatus;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import org.springframework.stereotype.Service;

/**
 * Serviço de aplicação que implementa {@link ConsultarSchemaUseCase}.
 *
 * <p>Delega todas as chamadas para {@link DataSourcePort}, que é injetado pelo
 * Spring com o adaptador ativo (Mock, QlikRest ou QlikEngine, conforme a
 * propriedade {@code datasource.adapter} no {@code application.yml}).</p>
 *
 * <p>Este serviço não contém lógica de negócio própria — sua responsabilidade
 * é desacoplar os controllers da porta de saída {@link DataSourcePort}, seguindo
 * o princípio da Inversão de Dependência (DIP).</p>
 *
 * @see DataSourcePort
 * @see br.gov.ms.saude.ssd.config.DataSourceConfig
 */
@Service
public class ConsultarSchemaService implements ConsultarSchemaUseCase {

    private final DataSourcePort dataSourcePort;

    /**
     * Injeta a porta de saída ativa via construtor.
     * O Spring seleciona o bean correto baseado em {@code datasource.adapter}.
     *
     * @param dataSourcePort porta de saída ativa (Mock, QlikRest, QlikEngine)
     */
    public ConsultarSchemaService(DataSourcePort dataSourcePort) {
        this.dataSourcePort = dataSourcePort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSchema getSchema() {
        return dataSourcePort.getDataSchema();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HealthStatus getHealth() {
        return dataSourcePort.checkHealth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AppMetadata getAppMetadata() {
        return dataSourcePort.getAppMetadata();
    }
}
