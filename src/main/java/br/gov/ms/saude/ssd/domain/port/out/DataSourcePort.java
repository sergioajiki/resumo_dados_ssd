package br.gov.ms.saude.ssd.domain.port.out;

import br.gov.ms.saude.ssd.domain.model.*;

import java.util.List;

/**
 * Porta de saída principal — define o contrato que qualquer fonte de dados
 * deve cumprir para ser utilizada pelo domínio da aplicação.
 *
 * <p>Esta interface é o ponto central da arquitetura hexagonal do projeto.
 * O domínio depende exclusivamente desta abstração, nunca de implementações concretas,
 * garantindo o princípio da Inversão de Dependência (DIP — SOLID).</p>
 *
 * <p>Implementações disponíveis:</p>
 * <ul>
 *   <li>{@code QlikRestAdapter} — metadados via HTTP REST (acesso público)</li>
 *   <li>{@code QlikEngineAdapter} — dados reais via WebSocket + JSON-RPC</li>
 *   <li>{@code MockAdapter} — dados simulados para desenvolvimento e testes</li>
 * </ul>
 *
 * <p><b>Como trocar a fonte de dados:</b> implemente esta interface, estenda
 * {@code DataSourcePortContractTest} para validar o contrato e altere
 * {@code datasource.adapter} no {@code application.yml}. Nenhuma outra
 * classe do domínio precisará ser modificada.</p>
 *
 * @see DataExtractorPort
 * @see br.gov.ms.saude.ssd.domain.model.AppMetadata
 * @see br.gov.ms.saude.ssd.domain.model.DataSchema
 */
public interface DataSourcePort {

    /**
     * Retorna os metadados descritivos da fonte de dados (app/dataset).
     *
     * @return {@link AppMetadata} com nome, ID, datas e informações do app
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se não for possível conectar à fonte de dados
     */
    AppMetadata getAppMetadata();

    /**
     * Retorna o schema completo da fonte de dados: tabelas, campos,
     * tipos e cardinalidades.
     *
     * <p>Utilizado para inspeção do modelo de dados e validação antes
     * da extração completa via {@link DataExtractorPort}.</p>
     *
     * @return {@link DataSchema} com a lista de tabelas e seus campos
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se não for possível conectar à fonte de dados
     */
    DataSchema getDataSchema();

    /**
     * Retorna os dados de um objeto específico (gráfico, KPI, tabela ou filtro)
     * identificado pelo seu ID na fonte de dados.
     *
     * @param objectId ID do objeto na fonte de dados (ex: Object ID do Qlik Sense)
     * @param options  opções de consulta: filtros, paginação, ordenação
     * @return {@link ObjectData} com cabeçalhos e linhas de dados do objeto
     * @throws br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException
     *         se não for possível conectar à fonte de dados
     * @throws br.gov.ms.saude.ssd.domain.exception.DataExtractionException
     *         se o objectId não existir ou ocorrer erro durante a leitura
     */
    ObjectData getObjectData(String objectId, QueryOptions options);

    /**
     * Lista todos os objetos disponíveis na fonte de dados (gráficos, KPIs, filtros).
     *
     * <p>Utilizado para descoberta e validação dos objetos antes
     * de iniciar a extração.</p>
     *
     * @return lista de {@link ObjectDescriptor}; pode ser vazia se nenhum objeto
     *         estiver disponível sem autenticação, mas nunca {@code null}
     */
    List<ObjectDescriptor> listAvailableObjects();

    /**
     * Verifica a disponibilidade e latência da conexão com a fonte de dados.
     *
     * <p>Utilizado pelo endpoint {@code /api/v1/health} e pelo scheduler de
     * sincronização antes de iniciar uma extração.</p>
     *
     * @return {@link HealthStatus} com status (UP/DOWN/DEGRADED) e latência em ms
     */
    HealthStatus checkHealth();

    /**
     * Lista os nomes de todos os campos disponíveis em uma tabela da fonte de dados.
     *
     * <p>Método diagnóstico para descobrir os nomes exatos dos campos no Qlik,
     * especialmente útil quando o nome de um campo é desconhecido.
     * Implementações que não suportam descoberta dinâmica retornam lista vazia.</p>
     *
     * @param tabelaNome nome da tabela na fonte (ex: "DB_DIGSAUDE")
     * @return lista com os nomes dos campos; lista vazia se não suportado
     */
    default List<String> listarCamposDisponiveis(String tabelaNome) {
        return List.of();
    }
}
