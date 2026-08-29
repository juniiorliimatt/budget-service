package br.com.budget.exceptions.handler;

import br.com.budget.exceptions.ResourceNotFoundException;
import br.com.budget.exceptions.models.ErrorResponse;
import br.com.budget.exceptions.models.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e, request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.UNPROCESSABLE_ENTITY, e, request, fieldErrors);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, Exception e, HttpServletRequest request, List<FieldError> fieldErrors) {
        var body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .statusName(status.name())
                .exception(e.getClass().getSimpleName())
                .message(e.getMessage())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
