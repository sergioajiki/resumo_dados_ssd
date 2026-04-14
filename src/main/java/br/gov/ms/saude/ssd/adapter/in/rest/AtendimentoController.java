package br.gov.ms.saude.ssd.adapter.in.rest;

import br.gov.ms.saude.ssd.adapter.in.rest.dto.AtendimentoDTO;
import br.gov.ms.saude.ssd.application.usecase.ConsultarAtendimentosUseCase;
import br.gov.ms.saude.ssd.domain.model.AtendimentoFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Controller REST para consulta de atendimentos do Núcleo de Telessaúde MS.
 *
 * <p>Adaptador de entrada que expõe os dados do repositório local H2 via HTTP.
 * Não acessa a fonte Qlik diretamente — toda consulta é sobre dados já sincronizados
 * pelo pipeline ETL, garantindo respostas rápidas independente da disponibilidade do Qlik.</p>
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>{@code GET /api/v1/atendimentos} — lista paginada com filtros opcionais</li>
 *   <li>{@code GET /api/v1/atendimentos/{id}} — atendimento por ID</li>
 *   <li>{@code GET /api/v1/atendimentos/resumo} — KPIs por município, especialidade e status</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/atendimentos")
@Tag(name = "Atendimentos", description = "Consulta de atendimentos do Núcleo de Telessaúde MS")
public class AtendimentoController {

    private final ConsultarAtendimentosUseCase consultarAtendimentosUseCase;

    /**
     * Injeta o use case via construtor (sem dependência direta do JPA).
     *
     * @param consultarAtendimentosUseCase use case de consulta dos atendimentos
     */
    public AtendimentoController(ConsultarAtendimentosUseCase consultarAtendimentosUseCase) {
        this.consultarAtendimentosUseCase = consultarAtendimentosUseCase;
    }

    /**
     * Lista atendimentos com filtros opcionais e paginação.
     *
     * <p>Todos os parâmetros de filtro são opcionais — quando ausentes, não restringem
     * o resultado. A paginação padrão é 20 registros por página, ordenados por
     * {@code dtAgendamento} decrescente.</p>
     *
     * @param municipio      filtro por nome do município (parcial, case-insensitive)
     * @param dataInicio     filtro por data de agendamento a partir de (inclusiva, formato dd/MM/yyyy)
     * @param dataFim        filtro por data de agendamento até (inclusiva, formato dd/MM/yyyy)
     * @param especialidade  filtro por especialidade médica (parcial, case-insensitive)
     * @param statusConsulta filtro por status da consulta (exato)
     * @param faixaEtaria    filtro por faixa etária (exato, ex: "18-29", "60+")
     * @param racaPaciente   filtro por raça/cor do paciente
     * @param pageable       configuração de paginação e ordenação
     * @return página de {@link AtendimentoDTO} correspondente aos critérios
     */
    @GetMapping
    @Operation(
        summary = "Listar atendimentos",
        description = "Lista atendimentos sincronizados do Qlik com filtros opcionais e paginação"
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public Page<AtendimentoDTO> listar(
            @Parameter(description = "Nome do município (parcial)") @RequestParam(required = false) String municipio,
            @Parameter(description = "Data início (dd/MM/yyyy)") @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dataInicio,
            @Parameter(description = "Data fim (dd/MM/yyyy)") @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dataFim,
            @Parameter(description = "Especialidade médica (parcial)") @RequestParam(required = false) String especialidade,
            @Parameter(description = "Status da consulta") @RequestParam(required = false) String statusConsulta,
            @Parameter(description = "Faixa etária (ex: 0-17, 18-29, 30-59, 60+)") @RequestParam(required = false) String faixaEtaria,
            @Parameter(description = "Raça/cor do paciente") @RequestParam(required = false) String racaPaciente,
            @PageableDefault(size = 20, sort = "dtAgendamento") Pageable pageable) {

        AtendimentoFilter filter = new AtendimentoFilter(
                municipio, dataInicio, dataFim, especialidade,
                statusConsulta, faixaEtaria, racaPaciente);

        return consultarAtendimentosUseCase.consultar(filter, pageable);
    }

    /**
     * Busca um atendimento específico pelo ID.
     *
     * @param id identificador do atendimento no banco local H2
     * @return {@link AtendimentoDTO} correspondente, ou 404 se não encontrado
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar atendimento por ID")
    @ApiResponse(responseCode = "200", description = "Atendimento encontrado")
    @ApiResponse(responseCode = "404", description = "Atendimento não encontrado")
    public ResponseEntity<AtendimentoDTO> buscarPorId(@PathVariable Long id) {
        return consultarAtendimentosUseCase.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchElementException("Atendimento não encontrado: " + id));
    }

    /**
     * Retorna resumo/KPIs dos atendimentos agrupados por dimensão.
     *
     * <p>Retorna três mapas de contagem (por município, especialidade e status)
     * aplicando os mesmos filtros opcionais do endpoint de listagem.
     * Usado para geração de indicadores em dashboards.</p>
     *
     * @param municipio      filtro por município (opcional)
     * @param dataInicio     filtro por data início (opcional)
     * @param dataFim        filtro por data fim (opcional)
     * @param especialidade  filtro por especialidade (opcional)
     * @param statusConsulta filtro por status (opcional)
     * @return mapa com três chaves: {@code porMunicipio}, {@code porEspecialidade}, {@code porStatus}
     */
    @GetMapping("/resumo")
    @Operation(
        summary = "Resumo / KPIs",
        description = "Contagem de atendimentos agrupada por município, especialidade e status"
    )
    @ApiResponse(responseCode = "200", description = "Resumo calculado com sucesso")
    public Map<String, Object> resumo(
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dataFim,
            @RequestParam(required = false) String especialidade,
            @RequestParam(required = false) String statusConsulta) {

        AtendimentoFilter filter = new AtendimentoFilter(
                municipio, dataInicio, dataFim, especialidade,
                statusConsulta, null, null);

        return Map.of(
                "porMunicipio", consultarAtendimentosUseCase.contarPorMunicipio(filter),
                "porEspecialidade", consultarAtendimentosUseCase.contarPorEspecialidade(filter),
                "porStatus", consultarAtendimentosUseCase.contarPorStatus(filter)
        );
    }
}
