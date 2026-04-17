package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MapsOffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para {@link MapsOffEntity}.
 *
 * <p>Utilizado pelo {@code LoaderService} em operações de truncate-reload:
 * {@code deleteAllInBatch()} + {@code saveAll()} a cada sync de {@code MAPS_OFF}.</p>
 */
@Repository
public interface MapsOffRepository extends JpaRepository<MapsOffEntity, String> {
}
