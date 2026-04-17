package br.gov.ms.saude.ssd.adapter.out.persistence.repository;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.JornadaVagasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data para a entidade {@link JornadaVagasEntity}.
 *
 * <p>Fornece acesso ao banco H2 para leitura e escrita de jornadas/vagas.
 * Usado pelo {@code LoaderService} para upsert em batch e pela camada de
 * consulta para cálculo de taxa de ocupação da agenda.</p>
 */
@Repository
public interface JornadaVagasRepository extends JpaRepository<JornadaVagasEntity, Long> {

    /**
     * Retorna todas as jornadas de um profissional específico.
     *
     * @param profissionalId ID do profissional
     * @return lista de jornadas do profissional
     */
    List<JornadaVagasEntity> findByProfissionalId(Long profissionalId);

    /**
     * Retorna todas as jornadas de um determinado mês/ano.
     *
     * @param ano  ano de referência
     * @param mes  mês de referência (1–12)
     * @return lista de jornadas do período
     */
    List<JornadaVagasEntity> findByAnoAndMes(Integer ano, Integer mes);
}
