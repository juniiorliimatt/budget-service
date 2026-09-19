package br.com.budget.exceptions;

/** Exclusão recusada porque o recurso ainda é referenciado por outro dado (ex.: tipo em uso por um lançamento). */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(final String message) {
        super(message);
    }
}
