package br.gov.ms.saude.ssd.adapter.in.rest;

import br.gov.ms.saude.ssd.adapter.in.rest.dto.HealthDTO;
import br.gov.ms.saude.ssd.application.usecase.ConsultarSchemaUseCase;
import br.gov.ms.saude.ssd.domain.model.DataSchema;
import br.gov.ms.saude.ssd.domain.model.HealthStatus;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para consulta de metadados e saúde da fonte de dados.
 *
 * <p>Expõe o schema das tabelas disponíveis, os metadados do app Qlik Sense
 * e o status de saúde da conexão com a fonte atual.</p>
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>{@code GET /api/v1/schema} — estrutura das tabelas da fonte de dados</li>
 *   <li>{@code GET /api/v1/health} — saúde da conexão com a fonte</li>
 *   <li>{@code GET /api/v1/schema/metadata} — metadados do app na fonte</li>
 * </ul>
 *
 * <p>Estes endpoints fazem chamadas em tempo real à fonte de dados configurada
 * (MockAdapter em dev/test, QlikEngineAdapter em prod). Podem ser lentos se
 * a fonte estiver sob carga — considere cache para uso intensivo.</p>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Schema e Saúde", description = "Metadados, schema e saúde da fonte de dados")
public class SchemaController {

    private final ConsultarSchemaUseCase consultarSchemaUseCase;
    private final DataSourcePort dataSourcePort;

    /**
     * Injeta o use case e a porta de saída via construtor.
     *
     * <p>{@code dataSourcePort} é injetado diretamente para obter o nome
     * do adaptador ativo via {@code getClass().getSimpleName()}.
     * Esta informação é exposta no endpoint de health para diagnóstico.</p>
     *
     * @param consultarSchemaUseCase use case de consulta de schema e saúde
     * @param dataSourcePort         porta de saída ativa (para identificar o adaptador)
     */
    public SchemaController(ConsultarSchemaUseCase consultarSchemaUseCase,
                             DataSourcePort dataSourcePort) {
        this.consultarSchemaUseCase = consultarSchemaUseCase;
        this.dataSourcePort = dataSourcePort;
    }

    /**
     * Retorna o schema completo da fonte de dados.
     *
     * <p>Inclui todas as tabelas disponíveis com seus campos, tipos e cardinalidades.
     * Realiza chamada em tempo real à fonte configurada.</p>
     *
     * @return {@link DataSchema} com a estrutura atual da fonte
     */
    @GetMapping("/schema")
    @Operation(
        summary = "Schema da fonte de dados",
        description = "Retorna todas as tabelas e campos disponíveis na fonte atual"
    )
    @ApiResponse(responseCode = "200", description = "Schema retornado com sucesso")
    @ApiResponse(responseCode = "503", description = "Fonte de dados indisponível")
    public DataSchema schema() {
        return consultarSchemaUseCase.getSchema();
    }

    /**
     * Verifica a saúde da conexão com a fonte de dados.
     *
     * <p>Mede a latência de resposta e retorna o status operacional.
     * O campo {@code adaptadorAtivo} identifica qual implementação está em uso,
     * facilitando o diagnóstico em ambientes com múltiplos profiles.</p>
     *
     * @return {@link HealthDTO} com status, latência, e nome do adaptador ativo
     */
    @GetMapping("/health")
    @Operation(
        summary = "Saúde da fonte de dados",
        description = "Verifica a disponibilidade e latência da conexão com a fonte atual"
    )
    @ApiResponse(responseCode = "200", description = "Verificação concluída")
    public HealthDTO health() {
        HealthStatus health = consultarSchemaUseCase.getHealth();
        String nomeAdaptador = dataSourcePort.getClass().getSimpleName()
                .replace("Adapter", "")
                .replace("DataSource", "")
                .toLowerCase();

        return new HealthDTO(
                health.status().name(),
                health.latencyMs(),
                health.message(),
                health.checkedAt(),
                nomeAdaptador
        );
    }

    /**
     * Retorna os metadados descritivos do app na fonte de dados.
     *
     * <p>Para o Qlik Sense, inclui nome do app, ID, proprietário e datas de
     * criação e último reload. Para o MockAdapter, retorna metadados simulados
     * com os valores reais do app de produção.</p>
     *
     * @return mapa com campos do {@link br.gov.ms.saude.ssd.domain.model.AppMetadata}
     */
    /**
     * Lista os campos disponíveis em uma tabela da fonte de dados.
     *
     * <p>Endpoint diagnóstico para descobrir os nomes exatos dos campos no Qlik.
     * Útil quando o nome de um campo é desconhecido antes de adicioná-lo à extração ETL.</p>
     *
     * @param tabela nome da tabela (padrão: "DB_DIGSAUDE")
     * @return lista com os nomes de todos os campos encontrados na tabela
     */
    /**
     * Lista as tabelas disponíveis na fonte de dados.
     *
     * <p>Endpoint diagnóstico para descobrir os nomes exatos das tabelas no Qlik.</p>
     *
     * @return lista com os nomes de todas as tabelas encontradas na fonte
     */
    @GetMapping("/schema/tabelas")
    @Operation(
        summary = "Listar tabelas disponíveis",
        description = "Descobre os nomes de todas as tabelas disponíveis na fonte de dados"
    )
    @ApiResponse(responseCode = "200", description = "Tabelas retornadas com sucesso")
    public List<String> tabelas() {
        return dataSourcePort.getDataSchema()
                .tabelas().stream()
                .map(br.gov.ms.saude.ssd.domain.model.TableSchema::nome)
                .toList();
    }

    @GetMapping("/schema/campos")
    @Operation(
        summary = "Listar campos de uma tabela",
        description = "Descobre os nomes exatos dos campos disponíveis em uma tabela do Qlik"
    )
    @ApiResponse(responseCode = "200", description = "Campos retornados com sucesso")
    public List<String> campos(
            @RequestParam(defaultValue = "DB_DIGSAUDE") String tabela) {
        return dataSourcePort.listarCamposDisponiveis(tabela);
    }

    @GetMapping("/schema/metadata")
    @Operation(
        summary = "Metadados do app",
        description = "Informações descritivas do app/dataset na fonte de dados atual"
    )
    @ApiResponse(responseCode = "200", description = "Metadados retornados com sucesso")
    public Map<String, Object> metadata() {
        var meta = consultarSchemaUseCase.getAppMetadata();
        return Map.of(
                "id", meta.id(),
                "nome", meta.nome(),
                "descricao", meta.descricao() != null ? meta.descricao() : "",
                "ultimoReload", meta.ultimoReload() != null ? meta.ultimoReload().toString() : ""
        );
    }
}
