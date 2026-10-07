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

-- changeset budget-service:additional_income-v1-initial context:structure labels:api,additional_income
-- comment: Cria a tabela inicial para registrar as rendas adicionais (additional_income)
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'additional_income'
CREATE TABLE budget.additional_income
(
    id         UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    name       VARCHAR(20),
    value      NUMERIC(7, 2),
    revenue_id UUID,

    CONSTRAINT FK_ADDITIONAL_INCOME_REVENUE FOREIGN KEY (revenue_id) REFERENCES revenues (id)
);

-- changeset budget-service:spending-v1-initial context:structure labels:api,spending
-- comment: Cria a tabela inicial para registrar os gastos (spending’s)
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'spending'
CREATE TABLE budget.spending
(
    id          UUID NOT NULL primary key,
    name        VARCHAR(25) NOT NULL,
    description VARCHAR(250),
    value       NUMERIC(7, 2) NOT NULL,
    date        DATE NOT NULL,
    was_paid    BOOLEAN DEFAULT FALSE,
    spent_type  VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP
);
