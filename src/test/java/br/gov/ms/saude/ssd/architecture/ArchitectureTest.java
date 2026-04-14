package br.gov.ms.saude.ssd.architecture;

import br.gov.ms.saude.ssd.domain.port.out.DataExtractorPort;
import br.gov.ms.saude.ssd.domain.port.out.DataSourcePort;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Testes de arquitetura que garantem que as regras SOLID não são violadas.
 *
 * <p>Executados automaticamente em cada build pelo Maven (fase {@code test}).
 * Uma falha nestes testes indica que uma dependência proibida foi introduzida,
 * violando os princípios arquiteturais do projeto.</p>
 *
 * <p>Para adicionar uma nova regra: declare um campo {@code static final ArchRule}
 * anotado com {@code @ArchTest}. O ArchUnit a executará automaticamente.</p>
 */
@AnalyzeClasses(
    packages = "br.gov.ms.saude.ssd",
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    /**
     * O domínio não pode importar classes dos adaptadores.
     *
     * <p>Violação deste princípio significa que o domínio está acoplado a
     * uma implementação concreta (ex: Qlik, JPA), quebrando o DIP (Dependency
     * Inversion Principle). O domínio deve depender apenas de abstrações
     * definidas nos pacotes {@code domain.port}.</p>
     */
    @ArchTest
    static final ArchRule domainNaoDeveDependerDeAdapters =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .as("O domínio não deve conhecer nenhuma implementação de adaptador");

    /**
     * Os domain objects (records) não podem usar anotações ou classes de framework.
     *
     * <p>Os records em {@code domain.model} devem ser POJO puros, sem Spring,
     * JPA ou qualquer dependência de framework. Isso garante que o domínio
     * seja testável de forma isolada e substituível sem reescrita.</p>
     */
    @ArchTest
    static final ArchRule domainModelNaoDeveUsarFrameworks =
        noClasses()
            .that().resideInAPackage("..domain.model..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
            .as("Domain objects (records) devem ser POJO puros, sem frameworks");

    /**
     * Controllers REST não podem acessar repositories diretamente.
     *
     * <p>Todo acesso a dados deve passar pelos use cases da camada de aplicação.
     * Controllers que acessam repositories diretamente violam a separação de
     * responsabilidades e tornam a lógica de negócio inacessível para reutilização.</p>
     */
    @ArchTest
    static final ArchRule controllerNaoAcessaRepositoryDiretamente =
        noClasses()
            .that().resideInAPackage("..adapter.in.rest..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.out.persistence.repository..")
            .as("Controllers não devem acessar repositories diretamente — use os use cases");

    /**
     * Adaptadores Qlik não podem depender do MockAdapter e vice-versa.
     *
     * <p>Cada adaptador deve ser completamente independente dos demais.
     * Dependências cruzadas entre adaptadores criariam acoplamento desnecessário
     * e dificultariam a troca individual de qualquer um deles.</p>
     */
    @ArchTest
    static final ArchRule adaptadoresQlikNaoConhecemMock =
        noClasses()
            .that().resideInAPackage("..adapter.out.qlik..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.out.mock..")
            .as("Adaptadores Qlik não devem depender do MockAdapter");

    /**
     * Toda classe com sufixo "Adapter" em {@code adapter.out} deve implementar
     * {@link DataSourcePort} ou {@link DataExtractorPort}.
     *
     * <p>Esta regra previne a criação de classes chamadas "Adapter" que não
     * implementam nenhum contrato, o que tornaria inútil o mecanismo de
     * contract tests para validação de substituição.</p>
     */
    @ArchTest
    static final ArchRule adapterOutDeveImplementarPorta =
        classes()
            .that().resideInAPackage("..adapter.out..")
            .and().haveSimpleNameEndingWith("Adapter")
            .should().implement(
                JavaClass.Predicates.assignableTo(DataSourcePort.class)
                    .or(JavaClass.Predicates.assignableTo(DataExtractorPort.class))
            )
            .as("Classes com sufixo Adapter em adapter.out devem implementar DataSourcePort ou DataExtractorPort");

    /**
     * Exceções lançadas pelo domínio devem residir no pacote {@code domain.exception}.
     *
     * <p>Centralizar as exceções de domínio evita que regras de negócio sejam
     * espalhadas pelos adaptadores, facilitando o entendimento do que pode
     * dar errado no domínio.</p>
     */
    @ArchTest
    static final ArchRule excecoesDeDominioDevemEstarNoPacoteCorreto =
        classes()
            .that().haveSimpleNameEndingWith("Exception")
            .and().resideInAPackage("..domain..")
            .should().resideInAPackage("..domain.exception..")
            .as("Exceções de domínio devem residir no pacote domain.exception");
}
