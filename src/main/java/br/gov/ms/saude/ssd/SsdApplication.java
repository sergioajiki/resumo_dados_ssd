package br.gov.ms.saude.ssd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada da aplicação <strong>Resumo de Dados SSD</strong>.
 *
 * <h2>Objetivo</h2>
 * <p>
 * ETL que extrai dados do <strong>Qlik Sense</strong> via WebSocket (Qlik Engine JSON API)
 * e REST, armazena localmente em banco <strong>H2 em modo arquivo</strong> e expõe os dados
 * consolidados via API REST e interface Thymeleaf para relatórios operacionais da
 * Secretaria de Estado de Saúde do Mato Grosso do Sul (SES-MS).
 * </p>
 *
 * <h2>Arquitetura</h2>
 * <p>
 * O projeto segue a <strong>Arquitetura Hexagonal</strong> (Ports and Adapters):
 * </p>
 * <ul>
 *   <li><strong>Domínio</strong> ({@code domain.*}): entidades, value objects e regras de negócio
 *       puras — sem dependência de frameworks.</li>
 *   <li><strong>Aplicação</strong> ({@code application.*}): casos de uso (ports de entrada)
 *       que orquestram o domínio.</li>
 *   <li><strong>Infraestrutura</strong> ({@code infrastructure.*}): adaptadores concretos —
 *       Qlik WebSocket, JPA/H2, HTTP controllers, Thymeleaf views.</li>
 * </ul>
 *
 * <h2>Substituição do Qlik</h2>
 * <p>
 * O adaptador Qlik ({@code QlikEngineAdapter}) implementa a porta de saída
 * {@code DataSourcePort}. Quando o Qlik for substituído por outra fonte de dados,
 * basta criar um novo adaptador que implemente a mesma porta — o domínio e os
 * casos de uso <strong>não precisarão ser alterados</strong>.
 * </p>
 *
 * <h2>Sincronização</h2>
 * <p>
 * A sincronização incremental é agendada via {@code @Scheduled} (cron configurado em
 * {@code application.yml}) e usa o campo {@code DT_NEW} como marca d'água para
 * buscar apenas registros novos ou alterados desde a última execução.
 * </p>
 *
 * @author SES-MS — Equipe de Tecnologia em Saúde
 * @version 0.1.0-SNAPSHOT
 * @see <a href="http://localhost:8080/swagger-ui.html">Swagger UI (local)</a>
 * @see <a href="http://localhost:8080/h2-console">H2 Console (apenas em dev)</a>
 */
@SpringBootApplication
// Registra todas as classes @ConfigurationProperties do pacote como beans Spring
@ConfigurationPropertiesScan
// Habilita o processamento de @Scheduled — necessário para o job de sincronização ETL
@EnableScheduling
public class SsdApplication {

    /**
     * Método principal — inicializa o contexto Spring Boot.
     *
     * <p>O profile ativo é determinado pela propriedade
     * {@code spring.profiles.active} (padrão: {@code dev}) ou pela
     * variável de ambiente {@code SPRING_PROFILES_ACTIVE}.</p>
     *
     * @param args argumentos de linha de comando (repassados ao Spring)
     */
    public static void main(String[] args) {
        SpringApplication.run(SsdApplication.class, args);
    }
}
