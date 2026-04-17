package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MunicipioPilotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para {@link MunicipioPilotoEntity}.
 *
 * <p>Utilizado pelo {@code LoaderService} em operações de truncate-reload:
 * {@code deleteAllInBatch()} + {@code saveAll()} a cada sync de {@code MUN_PILOTO}.</p>
 */
@Repository
public interface MunicipioPilotoRepository extends JpaRepository<MunicipioPilotoEntity, Long> {
}
