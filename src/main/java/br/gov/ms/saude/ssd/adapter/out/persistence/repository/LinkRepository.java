package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.LinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para {@link LinkEntity}.
 *
 * <p>Utilizado pelo {@code LoaderService} em operações de truncate-reload:
 * {@code deleteAllInBatch()} + {@code saveAll()} a cada sync de {@code LINK}.</p>
 */
@Repository
public interface LinkRepository extends JpaRepository<LinkEntity, String> {
}
