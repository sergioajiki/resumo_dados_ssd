package br.gov.ms.saude.ssd.adapter.in.rest.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO imutável de saída que representa um atendimento do Núcleo de Telessaúde MS.
 *
 * <p>Record em camelCase exposto pela API REST ({@code /api/v1/atendimentos}).
 * Produzido pelo {@code AtendimentoTransformer} a partir das entidades JPA e
 * retornado pelos controllers como corpo de resposta JSON.</p>
 *
 * <p>Separado da entidade JPA para garantir que alterações no modelo de persistência
 * não afetem o contrato público da API. O campo {@link #faixaEtaria} é calculado
 * pelo transformador com base em {@link #dtNascimento} e não existe diretamente
 * na entidade.</p>
 *
 * @param id               identificador único do atendimento no repositório local H2
 * @param cnsPaciente      Cartão Nacional de Saúde do paciente (15 dígitos)
 * @param dtNascimento     data de nascimento do paciente para cálculo de faixa etária
 * @param faixaEtaria      faixa etária calculada pelo transformador (ex: "0-17", "18-59", "60+")
 * @param raca             raça/cor autodeclarada do paciente conforme classificação IBGE
 * @param etnia            etnia do paciente (aplicável a populações indígenas)
 * @param municipio        nome do município de residência do paciente
 * @param ibge             código IBGE de 7 dígitos do município de residência
 * @param dtAgendamento    data e hora em que a consulta foi agendada
 * @param hrAgendamento    horário específico do agendamento da consulta
 * @param nomeMedico       nome completo do profissional de saúde responsável
 * @param cboCodigo        código CBO (Classificação Brasileira de Ocupações) do profissional
 * @param especialidade    especialidade médica do atendimento (ex: "Cardiologia", "Pediatria")
 * @param statusConsulta   status atual da consulta (ex: "Realizado", "Cancelado", "Agendado")
 * @param classifConclusao classificação da conclusão (ex: "Com atendimento", "Sem atendimento")
 * @param desfecho         desfecho clínico registrado ao final do atendimento
 * @param cid              código CID-10 do diagnóstico principal registrado
 * @param dtSolicitacao    data e hora em que a solicitação do atendimento foi criada na fonte
 * @param tipoZona             classificação de zona do endereço do paciente (ex: "Urbana", "Rural")
 * @param dtCriacao            data e hora de inserção do registro no repositório local H2
 * @param cnesEstabelecimento  código CNES do estabelecimento de saúde (7 dígitos)
 * @param idEstabelecimento    identificador do estabelecimento na fonte
 * @param idMedico             identificador do médico/profissional na fonte
 * @param classificCor         segunda classificação de raça/cor do paciente
 * @param tpNwConclusao        tipo de conclusão (campo TP_NW_CONCLUSAO)
 * @param idDigsaudeRef        identificador de referência no DigSaúde
 * @param telefone             telefone de contato do paciente
 * @param cepPaciente          CEP do endereço do paciente
 * @param ruaPaciente          logradouro do endereço do paciente
 * @param numPaciente          número do endereço do paciente
 * @param bairroPaciente       bairro do endereço do paciente
 * @param complementoEnd       complemento do endereço do paciente
 * @param descricaoEndereco    descrição textual do endereço
 * @param enderecoCompleto     endereço completo formatado
 * @param descricaoConsulta    notas clínicas da consulta
 */
public record AtendimentoDTO(
        Long id,
        String cnsPaciente,
        LocalDate dtNascimento,
        String faixaEtaria,
        String raca,
        String etnia,
        String municipio,
        String ibge,
        LocalDateTime dtAgendamento,
        LocalTime hrAgendamento,
        String nomeMedico,
        String cboCodigo,
        String especialidade,
        String statusConsulta,
        String classifConclusao,
        String desfecho,
        String cid,
        LocalDateTime dtSolicitacao,
        String tipoZona,
        String tipoServico,
        LocalDateTime dtCriacao,
        String cnesEstabelecimento,
        String idEstabelecimento,
        String idMedico,
        String classificCor,
        String tpNwConclusao,
        String idDigsaudeRef,
        String telefone,
        String cepPaciente,
        String ruaPaciente,
        String numPaciente,
        String bairroPaciente,
        String complementoEnd,
        String descricaoEndereco,
        String enderecoCompleto,
        String descricaoConsulta
) {

    /**
     * Cria um {@link AtendimentoDTO} completamente vazio, com todos os campos nulos.
     *
     * <p>Útil como valor neutro (Null Object Pattern) para evitar retorno de {@code null}
     * em buscas por ID que não encontram resultado, ou como base para testes unitários
     * que precisam de uma instância mínima sem preencher todos os campos.</p>
     *
     * @return nova instância de {@link AtendimentoDTO} com todos os campos {@code null}
     */
    public static AtendimentoDTO vazio() {
        return new AtendimentoDTO(
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null
        );
    }
}
