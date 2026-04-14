package br.gov.ms.saude.ssd.application.usecase;

import br.gov.ms.saude.ssd.domain.model.AppMetadata;
import br.gov.ms.saude.ssd.domain.model.DataSchema;
import br.gov.ms.saude.ssd.domain.model.HealthStatus;

/**
 * Porta de entrada (use case) para consulta do schema, metadados e saúde da fonte de dados.
 *
 * <p>Agrupa as operações de inspeção da fonte de dados (Qlik Sense): estrutura das tabelas
 * e campos, metadados do app e verificação de disponibilidade. Exposto pelo endpoint
 * {@code /api/v1/schema} e utilizado pelo dashboard de monitoramento.</p>
 *
 * <p>Segue o princípio da Segregação de Interfaces (ISP — SOLID): as operações de
 * consulta de schema são separadas das operações de extração ({@code ExecutarSyncUseCase}),
 * pois têm consumidores distintos e frequências de chamada diferentes.</p>
 *
 * @see DataSchema
 * @see HealthStatus
 * @see AppMetadata
 */
public interface ConsultarSchemaUseCase {

    /**
     * Retorna o schema completo da fonte de dados: todas as tabelas, campos,
     * tipos e cardinalidades disponíveis.
     *
     * <p>Realiza uma consulta em tempo real à fonte — não utiliza cache.
     * Pode ser lento para fontes com muitas tabelas.</p>
     *
     * @return {@link DataSchema} com a estrutura atual da fonte de dados
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se a fonte de dados estiver inacessível
     */
    DataSchema getSchema();

    /**
     * Verifica a disponibilidade e latência da conexão com a fonte de dados.
     *
     * <p>Realiza uma chamada de teste à fonte para medir a latência atual.
     * Usado pelo endpoint {@code /api/v1/health} e pelo scheduler antes
     * de iniciar qualquer sincronização.</p>
     *
     * @return {@link HealthStatus} com o status (UP/DOWN/DEGRADED) e latência medida
     */
    HealthStatus getHealth();

    /**
     * Retorna os metadados descritivos do app/dataset na fonte de dados.
     *
     * <p>Inclui nome, ID, proprietário e datas de criação, publicação e
     * último reload. Útil para exibição no dashboard e para auditoria.</p>
     *
     * @return {@link AppMetadata} com as informações do app na fonte de dados
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se a fonte de dados estiver inacessível
     */
    AppMetadata getAppMetadata();
}
