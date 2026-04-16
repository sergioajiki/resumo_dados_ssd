package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.SyncDiagnosticoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data para a entidade {@link SyncDiagnosticoEntity}.
 *
 * <p>Usado pelo {@code LoaderService} para persistir contagens de campos após
 * cada sincronização ETL, permitindo auditoria de completude dos dados via SQL:</p>
 *
 * <pre>SELECT * FROM SYNC_DIAGNOSTICO ORDER BY DT_SYNC DESC</pre>
 */
@Repository
public interface SyncDiagnosticoRepository extends JpaRepository<SyncDiagnosticoEntity, Long> {
}
