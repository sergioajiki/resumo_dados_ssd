package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.ProfissionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data para a entidade {@link ProfissionalEntity}.
 *
 * <p>Fornece operações de leitura e escrita do cadastro de profissionais
 * de saúde extraído da tabela {@code TEMPDB_USER} do Qlik Sense.</p>
 */
@Repository
public interface ProfissionalRepository extends JpaRepository<ProfissionalEntity, Long> {

    /**
     * Busca profissionais pelo município de atuação (correspondência exata, case-insensitive).
     *
     * @param municipio nome do município
     * @return lista de profissionais que atuam naquele município
     */
    List<ProfissionalEntity> findByMunicipioIgnoreCase(String municipio);

    /**
     * Busca profissionais por especialidade médica (correspondência parcial, case-insensitive).
     *
     * @param especialidade parte do nome da especialidade
     * @return lista de profissionais com aquela especialidade
     */
    List<ProfissionalEntity> findByEspecialidadeNomeContainingIgnoreCase(String especialidade);
}
