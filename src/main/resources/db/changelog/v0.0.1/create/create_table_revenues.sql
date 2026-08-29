--liquibase formatted sql

-- changeset budget-service:revenues-v1-initial context:structure labels:revenues
-- comment: Cria a tabela inicial para registrar as receitas (revenues)
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'revenues'
CREATE TABLE budget.revenues
(
    id         UUID NOT NULL PRIMARY KEY,
    name       VARCHAR(50)    NOT NULL,
    value      NUMERIC(10, 2) NOT NULL,
    date       DATE           NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
