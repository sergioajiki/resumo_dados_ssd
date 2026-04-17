package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MunaprovPilotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para {@link MunaprovPilotoEntity}.
 *
 * <p>Utilizado pelo {@code LoaderService} em operações de truncate-reload:
 * {@code deleteAllInBatch()} + {@code saveAll()} a cada sync de {@code MUNAPROV_PILOTO}.</p>
 */
@Repository
public interface MunaprovPilotoRepository extends JpaRepository<MunaprovPilotoEntity, Long> {
}
