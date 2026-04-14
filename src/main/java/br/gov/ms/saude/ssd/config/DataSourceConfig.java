package br.gov.ms.saude.ssd.config;

import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * Configuração central de seleção do adaptador de fonte de dados.
 *
 * <p>Implementa o mecanismo de troca de fonte de dados por configuração,
 * sem necessidade de alteração de código. O adaptador ativo é determinado
 * pela propriedade {@code datasource.adapter} no {@code application.yml}.</p>
 *
 * <p>O Spring injeta automaticamente todos os beans que implementam
 * {@link DataSourcePort} em um {@code Map<String, DataSourcePort>},
 * usando o nome do bean como chave. Este {@code @Configuration} seleciona
 * o bean correto com base na propriedade configurada e o expõe como bean
 * {@code @Primary}. Quando o adaptador também implementa {@link DataExtractorPort}
 * (mock, qlik-engine), o mesmo bean primário cobre ambas as interfaces.</p>
 *
 * <p><b>Como adicionar um novo adaptador:</b></p>
 * <ol>
 *   <li>Implemente {@link DataSourcePort} (e {@link DataExtractorPort} se necessário)</li>
 *   <li>Anote com {@code @Component("nomeDoAdapter")}</li>
 *   <li>Configure {@code datasource.adapter: nomeDoAdapter} no {@code application.yml}</li>
 * </ol>
 *
 * <p>Nenhuma outra classe precisa ser modificada — o domínio, os serviços
 * e os controllers continuam usando a injeção de {@link DataSourcePort} normalmente.</p>
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * Nome do adaptador ativo, lido de {@code datasource.adapter}.
     * Padrão: {@code "mock"} — garante que a aplicação sobe sem rede disponível.
     */
    @Value("${datasource.adapter:mock}")
    private String adapterName;

    /**
     * Seleciona e expõe como bean primário o adaptador {@link DataSourcePort} configurado.
     *
     * <p>O Spring injeta todos os beans que implementam {@link DataSourcePort}
     * no mapa {@code adapters}, usando o nome do bean ({@code @Component("nome")})
     * como chave. Este método seleciona o bean cujo nome corresponde a
     * {@code datasource.adapter}.</p>
     *
     * @param adapters mapa de todos os adaptadores disponíveis no contexto Spring
     * @return o adaptador de fonte de dados configurado como ativo
     * @throws IllegalStateException se nenhum adaptador com o nome configurado for encontrado
     */
    @Bean
    @Primary
    public DataSourcePort activeDataSourcePort(Map<String, DataSourcePort> adapters) {
        var adapter = adapters.get(adapterName + "Adapter");
        if (adapter == null) {
            // tenta sem sufixo, para nomes de bean personalizados
            adapter = adapters.get(adapterName);
        }
        if (adapter == null) {
            throw new IllegalStateException(
                "Nenhum DataSourcePort encontrado para datasource.adapter='" + adapterName +
                "'. Adaptadores disponíveis: " + adapters.keySet());
        }
        log.info("DataSourcePort ativo: {} ({})", adapterName, adapter.getClass().getSimpleName());
        return adapter;
    }

}
