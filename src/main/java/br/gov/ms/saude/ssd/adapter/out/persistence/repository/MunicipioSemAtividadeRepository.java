package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MunicipioSemAtividadeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para {@link MunicipioSemAtividadeEntity}.
 *
 * <p>Utilizado pelo {@code LoaderService} em operações de truncate-reload:
 * {@code deleteAllInBatch()} + {@code saveAll()} a cada sync de {@code MUN_SEMATIVIDADE}.</p>
 */
@Repository
public interface MunicipioSemAtividadeRepository extends JpaRepository<MunicipioSemAtividadeEntity, String> {
}
