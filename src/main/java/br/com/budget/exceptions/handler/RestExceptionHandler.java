package br.com.budget.exceptions.handler;

import br.com.budget.exceptions.DuplicateResourceException;
import br.com.budget.exceptions.ResourceInUseException;
import br.com.budget.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Corpo de erro padronizado em RFC 7807 ({@link ProblemDetail}) — mesmo padrão do
 * {@code RestExceptionHandler} do workbox-api, incluindo o catch-all
 * {@link #handleUnexpected}: sem ele, exceção não mapeada aqui cairia no whitelabel error
 * padrão do Spring, potencialmente vazando stack trace.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(final DuplicateResourceException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ProblemDetail handleResourceInUse(final ResourceInUseException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(final MethodArgumentNotValidException exception) {
        final var detail = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList());
        return detail;
    }

    /**
     * Ex.: tentar apagar um tipo de receita/despesa ainda referenciado por algum
     * lançamento. A mensagem original do Hibernate/Postgres expõe detalhe de
     * implementação (nome de constraint, SQL) que não serve pro usuário final - troca por
     * uma mensagem de alto nível, mantendo a exceção original só nos logs do server.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(final DataIntegrityViolationException exception, final HttpServletRequest request) {
        logger.error("Data integrity violation on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.CONFLICT, "Cannot delete: this record is still in use by other data.");
    }

    /**
     * JSON malformado ou com shape errado (ex.: array solto onde o contrato espera um
     * objeto envelope, tipo incompatível) é sempre erro do client, nunca do server -
     * sem este handler, {@code @ExceptionHandler(Exception.class)} abaixo capturava
     * primeiro (roda antes da resolução default do Spring MVC pra essa exceção) e
     * devolvia 500 pra um payload simplesmente mal formado.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(final HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception exception, final HttpServletRequest request) {
        logger.error("Unhandled exception on {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private ProblemDetail problem(final HttpStatus status, final String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
