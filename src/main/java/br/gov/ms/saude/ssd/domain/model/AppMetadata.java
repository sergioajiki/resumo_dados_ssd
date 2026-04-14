package br.gov.ms.saude.ssd.domain.model;

import java.time.LocalDateTime;

/**
 * Metadados descritivos de um app/dataset da fonte de dados (ex: app Qlik Sense).
 *
 * <p>Record imutável que representa as informações de identificação e ciclo de vida
 * do app de origem dos dados. Não contém lógica de negócio — é um valor puro
 * transportado entre as camadas da arquitetura hexagonal.</p>
 *
 * <p>Populado pela porta de saída {@code DataSourcePort#getAppMetadata()} e
 * exposto via {@code ConsultarSchemaUseCase}.</p>
 *
 * @param id           identificador único do app na fonte de dados (ex: GUID do Qlik Sense)
 * @param nome         nome de exibição do app (ex: "Núcleo de Telessaúde MS")
 * @param descricao    descrição textual do propósito do app
 * @param proprietario nome do proprietário ou equipe responsável pelo app na origem
 * @param criadoEm     data/hora de criação do app na fonte de dados
 * @param publicadoEm  data/hora em que o app foi publicado/disponibilizado para consumo
 * @param ultimoReload data/hora do último reload (recarga de dados) realizado no app
 * @param publicado    {@code true} se o app está publicado e disponível para acesso externo
 */
public record AppMetadata(
        String id,
        String nome,
        String descricao,
        String proprietario,
        LocalDateTime criadoEm,
        LocalDateTime publicadoEm,
        LocalDateTime ultimoReload,
        boolean publicado
) {
}
