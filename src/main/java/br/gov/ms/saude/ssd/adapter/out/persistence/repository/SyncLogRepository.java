package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.SyncLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data para a entidade {@link SyncLogEntity}.
 *
 * <p>Fornece consultas ao histórico de sincronizações ETL, incluindo
 * a busca pelo watermark da última sync bem-sucedida — usado pelo
 * pipeline para determinar o ponto de partida da extração incremental.</p>
 */
@Repository
public interface SyncLogRepository extends JpaRepository<SyncLogEntity, Long> {

    /**
     * Retorna o {@code concluidoEm} mais recente de uma sync com status SUCCESS
     * para uma tabela específica.
     *
     * <p>Este valor é o watermark usado pela extração incremental:
     * apenas registros com {@code DT_NEW} posterior a este timestamp são extraídos
     * na próxima execução.</p>
     *
     * @param tabela nome da tabela de destino (ex: "atendimento")
     * @return {@link Optional} com o timestamp da última sync bem-sucedida,
     *         ou vazio se nunca houve sync com sucesso para esta tabela
     */
    @Query("""
            SELECT s.concluidoEm FROM SyncLogEntity s
            WHERE s.tabela = :tabela AND s.status = 'SUCCESS'
            ORDER BY s.concluidoEm DESC
            LIMIT 1
            """)
    Optional<LocalDateTime> findLastSuccessfulSyncTime(@Param("tabela") String tabela);

    /**
     * Retorna o histórico de sincronizações de uma tabela, ordenado por data decrescente.
     *
     * @param tabela  nome da tabela
     * @param pageable paginação (usado para limitar o número de resultados)
     * @return lista de entidades de log ordenadas por {@code iniciadoEm} decrescente
     */
    List<SyncLogEntity> findByTabelaOrderByIniciadoEmDesc(String tabela, Pageable pageable);

    /**
     * Verifica se existe alguma sync com status RUNNING para uma tabela.
     * Usado pelo {@code SyncService} para evitar execuções sobrepostas.
     *
     * @param tabela nome da tabela
     * @return {@code true} se houver uma sync em andamento para a tabela
     */
    boolean existsByTabelaAndStatus(String tabela, String status);
}
