package br.gov.ms.saude.ssd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação OpenAPI/Swagger da aplicação.
 *
 * <p>Disponível em {@code /swagger-ui.html} quando a aplicação está em execução.
 * A especificação completa em formato JSON está em {@code /api-docs}.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Define os metadados da API expostos na interface Swagger UI.
     *
     * @return configuração OpenAPI com título, descrição, versão, contato e grupos de tags
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API Saúde Digital MS — Telessaúde")
                .description("""
                    API REST para consulta dos dados do Núcleo de Telessaúde e
                    Superintendência de Saúde Digital da Secretaria de Estado de Saúde
                    do Mato Grosso do Sul (SES-MS).

                    Os dados são extraídos do Qlik Sense via Engine API (WebSocket/JSON-RPC)
                    e armazenados localmente para consultas de alta performance,
                    eliminando dependência de rede no caminho crítico das consultas.

                    A fonte de dados é desacoplada via interface DataSourcePort —
                    o Qlik Sense pode ser substituído por qualquer outra API sem
                    alteração nos controllers ou na camada de domínio.
                    """)
                .version("0.1.0")
                .contact(new Contact()
                    .name("Superintendência de Saúde Digital — SES-MS")
                    .email("saude.digital@saude.ms.gov.br")))
            .addTagsItem(new Tag()
                .name("Atendimentos")
                .description("Consulta, análise e exportação de atendimentos de telessaúde"))
            .addTagsItem(new Tag()
                .name("Profissionais")
                .description("Consulta de profissionais de saúde e especialidades disponíveis"))
            .addTagsItem(new Tag()
                .name("Vagas")
                .description("Consulta de vagas e jornadas de atendimento"))
            .addTagsItem(new Tag()
                .name("Municípios")
                .description("Municípios atendidos, programa piloto e status de atividade"))
            .addTagsItem(new Tag()
                .name("Sincronização")
                .description("Controle do ETL, histórico e trigger de sincronização com a fonte"))
            .addTagsItem(new Tag()
                .name("Schema")
                .description("Inspeção do schema, saúde da conexão e metadados da fonte de dados"));
    }
}
