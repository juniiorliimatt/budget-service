package br.com.budget.exceptions.models;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class FieldError implements Serializable {

    private final String fieldName;
    private final String message;

    public FieldError(String fieldName, String message) {
        this.fieldName = fieldName;
        this.message = message;
    }
}
