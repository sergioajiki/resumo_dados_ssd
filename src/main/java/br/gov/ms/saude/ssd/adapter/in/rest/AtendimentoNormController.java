package br.gov.ms.saude.ssd.adapter.in.rest;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.AtendimentoNormEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.AtendimentoNormRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/atendimentos-norm")
@Tag(name = "Atendimentos Normalizados", description = "Consulta da tabela normalizada atendimento_norm")
public class AtendimentoNormController {

    private final AtendimentoNormRepository repository;

    public AtendimentoNormController(AtendimentoNormRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Listar atendimentos normalizados", description = "Lista paginada da tabela atendimento_norm com filtros opcionais")
    public Page<AtendimentoNormEntity> listar(
            @Parameter(description = "Município (parcial)") @RequestParam(required = false) String municipio,
            @Parameter(description = "Especialidade (parcial)") @RequestParam(required = false) String especialidade,
            @Parameter(description = "Status da consulta") @RequestParam(required = false) String statusConsulta,
            @Parameter(description = "Data início (dd/MM/yyyy)") @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dataInicio,
            @Parameter(description = "Data fim (dd/MM/yyyy)") @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dataFim,
            @Parameter(description = "CNS do paciente") @RequestParam(required = false) String cnsPaciente,
            @PageableDefault(size = 20, sort = "dtAgendamento") Pageable pageable) {

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim    = dataFim    != null ? dataFim.atTime(23, 59, 59) : null;

        return repository.filtrar(municipio, especialidade, statusConsulta, inicio, fim, cnsPaciente, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar atendimento normalizado por ID")
    public ResponseEntity<AtendimentoNormEntity> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchElementException("Atendimento não encontrado: " + id));
    }
}
