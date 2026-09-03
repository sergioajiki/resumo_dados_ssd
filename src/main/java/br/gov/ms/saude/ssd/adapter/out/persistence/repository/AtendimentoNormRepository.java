package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.AtendimentoNormEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface AtendimentoNormRepository extends JpaRepository<AtendimentoNormEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM atendimento_norm", nativeQuery = true)
    void truncate();

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO atendimento_norm (
                id, cns_paciente, dt_nascimento, raca, etnia, municipio, cod_ibge, tipo_zona,
                telefone, cep_paciente, rua_paciente, num_end_paciente, bairro_paciente,
                complemento_end_paciente, descricao_endereco_paciente,
                dt_agendamento, hr_agendamento, dt_solicitacao,
                status_consulta, classif_conclusao, tipo_servico, desfecho, cid,
                nome_medico, especialidade, cbo_medico, id_medico, cns_profissional,
                cnes_estabelecimento, nome_estabelecimento, id_estabelecimento, id_digsaude_ref
            )
            SELECT
                id,
                REPLACE(cns_paciente, ' ', ''),
                dt_nascimento, raca, etnia, municipio, ibge, tipo_zona, telefone,
                REPLACE(REPLACE(cep_paciente, '-', ''), ' ', ''),
                rua_paciente, num_paciente, bairro_paciente, complemento_end, descricao_endereco,
                dt_agendamento, hr_agendamento, dt_solicitacao,
                status_consulta, classif_conclusao, tipo_servico, desfecho, cid,
                CASE WHEN nome_medico IS NULL OR LOCATE(' - ', nome_medico) = 0
                     THEN nome_medico
                     ELSE TRIM(SUBSTRING(nome_medico, LOCATE(' - ', nome_medico) + 3)) END,
                CASE WHEN nome_medico IS NULL OR LOCATE(' - ', nome_medico) = 0
                     THEN NULL
                     ELSE TRIM(SUBSTRING(nome_medico, 1, LOCATE(' - ', nome_medico) - 1)) END,
                cbo_medico, id_medico, NULL,
                CASE WHEN cnes_estabelecimento IS NULL OR LOCATE(' ', cnes_estabelecimento) = 0
                     THEN cnes_estabelecimento
                     ELSE TRIM(SUBSTRING(cnes_estabelecimento, 1, LOCATE(' ', cnes_estabelecimento) - 1)) END,
                CASE WHEN cnes_estabelecimento IS NULL OR LOCATE(' ', cnes_estabelecimento) = 0
                     THEN NULL
                     ELSE TRIM(SUBSTRING(cnes_estabelecimento, LOCATE(' ', cnes_estabelecimento) + 1)) END,
                id_estabelecimento, id_digsaude_ref
            FROM atendimento
            """, nativeQuery = true)
    void populateFromAtendimento();

    @Query("""
            SELECT a FROM AtendimentoNormEntity a
            WHERE (:municipio IS NULL OR UPPER(a.municipio) LIKE UPPER(CONCAT('%', :municipio, '%')))
              AND (:especialidade IS NULL OR UPPER(a.especialidade) LIKE UPPER(CONCAT('%', :especialidade, '%')))
              AND (:statusConsulta IS NULL OR a.statusConsulta = :statusConsulta)
              AND (:dataInicio IS NULL OR a.dtAgendamento >= :dataInicio)
              AND (:dataFim IS NULL OR a.dtAgendamento <= :dataFim)
              AND (:cnsPaciente IS NULL OR a.cnsPaciente = :cnsPaciente)
            """)
    Page<AtendimentoNormEntity> filtrar(
            @Param("municipio") String municipio,
            @Param("especialidade") String especialidade,
            @Param("statusConsulta") String statusConsulta,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("cnsPaciente") String cnsPaciente,
            Pageable pageable);
}
