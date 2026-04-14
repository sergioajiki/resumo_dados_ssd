package br.gov.ms.saude.ssd.adapter.out.persistence;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.SyncLogEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.SyncLogRepository;
import br.gov.ms.saude.ssd.domain.model.SyncLog;
import br.gov.ms.saude.ssd.domain.port.out.SyncRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de saída que implementa {@link SyncRepositoryPort} usando JPA/H2.
 *
 * <p>Traduz entre o domain object {@link SyncLog} e a entidade JPA {@link SyncLogEntity}.
 * O serviço de sincronização não conhece o JPA — ele só enxerga a interface
 * {@link SyncRepositoryPort}, garantindo que a troca do banco de dados
 * não afete a lógica de negócio.</p>
 *
 * <p>Princípio aplicado: DIP — o domínio e a camada de aplicação dependem de
 * {@link SyncRepositoryPort}, nunca deste adaptador concreto.</p>
 *
 * @see SyncLog
 * @see SyncLogRepository
 */
@Component
public class SyncRepositoryAdapter implements SyncRepositoryPort {

    private final SyncLogRepository syncLogRepository;

    /**
     * Injeta o repositório JPA via construtor (imutável, facilita testes).
     *
     * @param syncLogRepository repositório Spring Data para sync_log
     */
    public SyncRepositoryAdapter(SyncLogRepository syncLogRepository) {
        this.syncLogRepository = syncLogRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Consulta o campo {@code concluido_em} do último registro com status
     * {@code SUCCESS} para a tabela informada. O nome da tabela mapeado aqui
     * é o nome da tabela de <em>destino</em> no H2 (ex: "atendimento"),
     * não o nome da tabela no Qlik (ex: "DB_DIGSAUDE").</p>
     */
    @Override
    public Optional<LocalDateTime> getLastSyncTime(String tableName) {
        return syncLogRepository.findLastSuccessfulSyncTime(tableName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Converte o domain object para entidade JPA antes de persistir.
     * Se o {@link SyncLog#id()} for {@code null}, o banco gera um novo ID.
     * Caso contrário, o registro existente é atualizado (upsert via JPA).</p>
     */
    @Override
    public void recordSync(SyncLog log) {
        syncLogRepository.save(SyncLogEntity.of(log));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retorna os {@code limit} registros mais recentes para a tabela,
     * convertidos para domain objects {@link SyncLog}.</p>
     */
    @Override
    public List<SyncLog> getHistory(String tableName, int limit) {
        return syncLogRepository
                .findByTabelaOrderByIniciadoEmDesc(tableName, PageRequest.of(0, limit))
                .stream()
                .map(SyncLogEntity::toDomain)
                .collect(Collectors.toList());
    }
}
