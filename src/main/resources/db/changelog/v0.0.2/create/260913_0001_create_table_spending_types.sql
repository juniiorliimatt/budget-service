-- liquibase formatted sql

-- changeset budget-service:spending_types-v1-initial context:structure labels:spending_types
-- comment: Catalogo de tipos de despesa, cadastrado via CRUD proprio - substitui o campo "name" solto da tabela spending.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'spending_types'
CREATE TABLE budget.spending_types
(
    id         UUID         NOT NULL PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    CONSTRAINT uk_spending_types_name UNIQUE (name)
);
