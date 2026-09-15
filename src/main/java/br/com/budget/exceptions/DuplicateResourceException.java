package br.com.budget.exceptions;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(final String message) {
        super(message);
    }
}
