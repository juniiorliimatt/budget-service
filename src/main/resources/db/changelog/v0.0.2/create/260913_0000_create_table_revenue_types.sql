-- liquibase formatted sql

-- changeset budget-service:revenue_types-v1-initial context:structure labels:revenue_types
-- comment: Catalogo de tipos de receita, cadastrado via CRUD proprio - substitui o campo "name" solto da tabela revenues.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'revenue_types'
CREATE TABLE budget.revenue_types
(
    id         UUID         NOT NULL PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    CONSTRAINT uk_revenue_types_name UNIQUE (name)
);
