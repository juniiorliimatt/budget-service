-- liquibase formatted sql

-- changeset budget-service:additional_income-v1-drop context:structure labels:additional_income
-- comment: Remove a tabela additional_income - orfa desde sempre, nenhuma entidade JPA a mapeia. Novo changeset em vez de editar/apagar o original ja aplicado (v0.0.1/create/create_table_revenues.sql), que quebraria o checksum do Liquibase em qualquer banco que ja rodou aquele changeset.
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'budget' AND table_name = 'additional_income'
DROP TABLE budget.additional_income;
