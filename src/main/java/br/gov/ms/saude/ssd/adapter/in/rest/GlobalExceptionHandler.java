package br.gov.ms.saude.ssd.adapter.in.rest;

import br.gov.ms.saude.ssd.domain.exception.DataExtractionException;
import br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException;
import br.gov.ms.saude.ssd.domain.exception.InvalidQueryOptionsException;
import br.gov.ms.saude.ssd.domain.exception.SyncAlreadyRunningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Handler global de exceções para a camada REST.
 *
 * <p>Converte exceções de domínio em respostas HTTP padronizadas usando
 * o formato RFC 7807 ({@link ProblemDetail}), disponível nativamente no
 * Spring Boot 3.x. Garante que nenhum stack trace vaze para os clientes
 * e que os status HTTP sejam semanticamente corretos.</p>
 *
 * <p>Mapeamento de exceções para status HTTP:</p>
 * <ul>
 *   <li>{@link DataSourceUnavailableException} → {@code 503 Service Unavailable}</li>
 *   <li>{@link DataExtractionException} → {@code 502 Bad Gateway}</li>
 *   <li>{@link SyncAlreadyRunningException} → {@code 409 Conflict}</li>
 *   <li>{@link InvalidQueryOptionsException} → {@code 400 Bad Request}</li>
 *   <li>{@link NoSuchElementException} → {@code 404 Not Found}</li>
 *   <li>{@link Exception} genérica → {@code 500 Internal Server Error}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata erros de indisponibilidade da fonte de dados (ex: Qlik fora do ar).
     *
     * @param ex exceção lançada quando a fonte está inacessível
     * @return ProblemDetail com status 503 e descrição do problema
     */
    @ExceptionHandler(DataSourceUnavailableException.class)
    public ProblemDetail handleDataSourceUnavailable(DataSourceUnavailableException ex) {
        log.warn("Fonte de dados indisponível: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Fonte de dados indisponível");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("/errors/data-source-unavailable"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }

    /**
     * Trata erros durante a extração de dados da fonte.
     *
     * @param ex exceção lançada quando a extração falha parcialmente
     * @return ProblemDetail com status 502 indicando erro na fonte upstream
     */
    @ExceptionHandler(DataExtractionException.class)
    public ProblemDetail handleDataExtraction(DataExtractionException ex) {
        log.error("Erro na extração de dados: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Erro na extração de dados");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("/errors/data-extraction-error"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }

    /**
     * Trata conflito de sync em andamento — evita execuções sobrepostas.
     *
     * @param ex exceção lançada quando uma sync já está em progresso
     * @return ProblemDetail com status 409 Conflict
     */
    @ExceptionHandler(SyncAlreadyRunningException.class)
    public ProblemDetail handleSyncAlreadyRunning(SyncAlreadyRunningException ex) {
        log.info("Tentativa de sync bloqueada: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Sincronização já em andamento");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("/errors/sync-already-running"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }

    /**
     * Trata filtros ou opções de consulta inválidos fornecidos pelo cliente.
     *
     * @param ex exceção lançada para parâmetros de consulta inválidos
     * @return ProblemDetail com status 400 Bad Request
     */
    @ExceptionHandler(InvalidQueryOptionsException.class)
    public ProblemDetail handleInvalidQueryOptions(InvalidQueryOptionsException ex) {
        log.debug("Opções de consulta inválidas: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Parâmetros de consulta inválidos");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("/errors/invalid-query-options"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }

    /**
     * Trata recursos não encontrados (ex: atendimento com ID inexistente).
     *
     * @param ex exceção lançada quando um recurso solicitado não existe
     * @return ProblemDetail com status 404 Not Found
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException ex) {
        log.debug("Recurso não encontrado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso não encontrado");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("/errors/not-found"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }

    /**
     * Trata recursos estáticos não encontrados (ex: favicon.ico solicitado pelo browser).
     *
     * <p>O browser requisita automaticamente {@code /favicon.ico} a cada acesso.
     * Como não existe esse recurso, Spring lança {@link NoResourceFoundException}.
     * Este handler evita que o catch-all genérico a registre como ERROR no log.</p>
     *
     * @param ex exceção lançada pelo Spring MVC para recursos estáticos ausentes
     * @return ProblemDetail com status 404 Not Found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Recurso estático não encontrado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso não encontrado");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("/errors/not-found"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }

    /**
     * Fallback para exceções não tratadas pelos handlers específicos.
     *
     * <p>Loga o erro completo internamente mas retorna apenas uma mensagem
     * genérica ao cliente, evitando vazamento de informações internas.</p>
     *
     * @param ex exceção genérica não tratada
     * @return ProblemDetail com status 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Erro interno do servidor");
        problem.setDetail("Ocorreu um erro inesperado. Verifique os logs para detalhes.");
        problem.setType(URI.create("/errors/internal-error"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;
    }
}
